package org.hkyaxhfg.tat.amqp;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hkyaxhfg.tat.autoconfiguration.AutoConfigurationInitializer;
import org.hkyaxhfg.tat.autoconfiguration.AutoConfigurationProperty;
import org.hkyaxhfg.tat.lang.util.LoggerGenerator;
import org.slf4j.Logger;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * AMQP自动配置.
 *
 * @author: wjf
 * @date: 2022/1/18
 */
@Configuration
@ComponentScan(
        basePackages = {
                "org.hkyaxhfg.tat.amqp"
        }
)
public class AmqpAutoConfigurationInitializer implements AutoConfigurationInitializer {

    private static final Logger logger = LoggerGenerator.logger(AmqpAutoConfigurationInitializer.class);

    @Configuration
    @EnableConfigurationProperties(AmqpProviderDef.class)
    @ConditionalOnProperty(prefix = AutoConfigurationProperty.AMQP_PROVIDER_MAIN_KEY, name = "enabled", havingValue = "true")
    public static class AmqpProviderConf {

        private final AmqpProviderDef amqpProviderDef;

        private final AmqpAdmin amqpAdmin;

        @Autowired
        public AmqpProviderConf(AmqpProviderDef amqpProviderDef, AmqpAdmin amqpAdmin) {
            this.amqpProviderDef = amqpProviderDef;
            this.amqpAdmin = amqpAdmin;
        }

        @PostConstruct
        public void init() {
            if (amqpProviderDef.isEnabled()) {
                AmqpUtils.init(amqpProviderDef, amqpAdmin);
                logger.info("AMQP-PROVIDER: {} 初始化完成...", amqpProviderDef.getAmqpDescription());
            }
        }

    }

    @Configuration
    @EnableConfigurationProperties(AmqpConsumerDef.class)
    @ConditionalOnProperty(prefix = AutoConfigurationProperty.AMQP_CONSUMER_MAIN_KEY, name = "enabled", havingValue = "true")
    public static class AmqpConsumerConf {

        private final AmqpConsumerDef amqpConsumerDef;

        private final ApplicationContext applicationContext;

        private final ConnectionFactory connectionFactory;

        @Autowired
        public AmqpConsumerConf(AmqpConsumerDef amqpConsumerDef, ApplicationContext applicationContext, ConnectionFactory connectionFactory) {
            this.amqpConsumerDef = amqpConsumerDef;
            this.applicationContext = applicationContext;
            this.connectionFactory = connectionFactory;
        }

        @PostConstruct
        public void init() {
            if (amqpConsumerDef.isEnabled()) {
                List<AmqpConsumerDef.MessageListener> messageListeners = amqpConsumerDef.getMessageListeners();
                if (CollectionUtils.isNotEmpty(messageListeners)) {
                    messageListeners.forEach(messageListener -> {
                        Object bean = applicationContext.getBean(messageListener.getQueueListenerBeanName());
                        if (StringUtils.isBlank(messageListener.getQueueNames())) {
                            return;
                        }
                        AmqpUtils.initQueueListener(
                                bean,
                                messageListener.getListenerMethodName(),
                                AmqpUtils.messageConverter(messageListener.getMessageConverterType()),
                                connectionFactory,
                                messageListener.getQueueNames().split(",")
                        );
                    });
                }

                logger.info("AMQP-CONSUMER: {} 初始化完成...", amqpConsumerDef.getAmqpDescription());
            }
        }

    }

}
