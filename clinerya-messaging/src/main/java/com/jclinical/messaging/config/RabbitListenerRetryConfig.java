package com.jclinical.messaging.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;

/**
 * Política de reintentos para los @RabbitListener del proyecto: 3 intentos con backoff
 * exponencial (5s, 30s, ~2min), y tras agotarlos el mensaje se re-publica en el exchange
 * de dead letters (jclinical.dlx) en vez de perderse.
 */
@Configuration
public class RabbitListenerRetryConfig {

    @Bean
    public RetryOperationsInterceptor domainEventRetryInterceptor(RabbitTemplate rabbitTemplate) {
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(5000L);
        backOffPolicy.setMultiplier(6.0);
        backOffPolicy.setMaxInterval(120000L);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return org.springframework.amqp.rabbit.config.RetryInterceptorBuilder.stateless()
                .retryOperations(retryTemplate)
                .recoverer(new RepublishMessageRecoverer(rabbitTemplate, com.jclinical.core.events.DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE))
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory domainEventListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter,
            RetryOperationsInterceptor domainEventRetryInterceptor) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAdviceChain(domainEventRetryInterceptor);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
