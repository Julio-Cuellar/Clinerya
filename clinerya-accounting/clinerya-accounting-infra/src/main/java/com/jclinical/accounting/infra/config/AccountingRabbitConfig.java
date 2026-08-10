package com.jclinical.accounting.infra.config;

import com.jclinical.core.events.DomainEventRoutingKeys;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cola del ARE que escucha eventos de consumo conciliado para generar pólizas contables
 * (Momento 2). Ver documento de arquitectura §3.1.
 */
@Configuration
public class AccountingRabbitConfig {

    public static final String CONSUMPTION_RECONCILED_QUEUE = "are.consumption.reconciled";
    public static final String PAYMENT_REGISTERED_QUEUE = "are.payment.registered";
    public static final String SUPPLY_WASTED_QUEUE = "are.supply.wasted";
    public static final String CASH_EXPENSE_REGISTERED_QUEUE = "are.cash.expense.registered";
    public static final String CASH_EXPENSE_VOIDED_QUEUE = "are.cash.expense.voided";
    public static final String PURCHASE_ORDER_CREATED_QUEUE = "are.purchase.order.created";
    public static final String DEAD_LETTER_QUEUE = "are.dlq";

    @Bean
    public Queue consumptionReconciledQueue() {
        return QueueBuilder.durable(CONSUMPTION_RECONCILED_QUEUE)
                .withArgument("x-dead-letter-exchange", DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DomainEventRoutingKeys.CONSUMPTION_RECONCILED)
                .build();
    }

    @Bean
    public Queue paymentRegisteredQueue() {
        return QueueBuilder.durable(PAYMENT_REGISTERED_QUEUE)
                .withArgument("x-dead-letter-exchange", DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DomainEventRoutingKeys.PAYMENT_REGISTERED)
                .build();
    }

    @Bean
    public Queue supplyWastedQueue() {
        return QueueBuilder.durable(SUPPLY_WASTED_QUEUE)
                .withArgument("x-dead-letter-exchange", DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DomainEventRoutingKeys.SUPPLY_WASTED)
                .build();
    }

    @Bean
    public Queue cashExpenseRegisteredQueue() {
        return QueueBuilder.durable(CASH_EXPENSE_REGISTERED_QUEUE)
                .withArgument("x-dead-letter-exchange", DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DomainEventRoutingKeys.CASH_EXPENSE_REGISTERED)
                .build();
    }

    @Bean
    public Queue cashExpenseVoidedQueue() {
        return QueueBuilder.durable(CASH_EXPENSE_VOIDED_QUEUE)
                .withArgument("x-dead-letter-exchange", DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DomainEventRoutingKeys.CASH_EXPENSE_VOIDED)
                .build();
    }

    @Bean
    public Queue purchaseOrderCreatedQueue() {
        return QueueBuilder.durable(PURCHASE_ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DomainEventRoutingKeys.PURCHASE_ORDER_CREATED)
                .build();
    }

    @Bean
    public Queue areDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding consumptionReconciledBinding(Queue consumptionReconciledQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(consumptionReconciledQueue)
                .to(domainEventsExchange)
                .with(DomainEventRoutingKeys.CONSUMPTION_RECONCILED);
    }

    @Bean
    public Binding paymentRegisteredBinding(Queue paymentRegisteredQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(paymentRegisteredQueue)
                .to(domainEventsExchange)
                .with(DomainEventRoutingKeys.PAYMENT_REGISTERED);
    }

    @Bean
    public Binding areDeadLetterBinding(Queue areDeadLetterQueue, DirectExchange domainEventsDeadLetterExchange) {
        return BindingBuilder.bind(areDeadLetterQueue)
                .to(domainEventsDeadLetterExchange)
                .with(DomainEventRoutingKeys.CONSUMPTION_RECONCILED);
    }

    @Bean
    public Binding areDeadLetterPaymentBinding(Queue areDeadLetterQueue, DirectExchange domainEventsDeadLetterExchange) {
        return BindingBuilder.bind(areDeadLetterQueue)
                .to(domainEventsDeadLetterExchange)
                .with(DomainEventRoutingKeys.PAYMENT_REGISTERED);
    }

    @Bean
    public Binding supplyWastedBinding(Queue supplyWastedQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(supplyWastedQueue)
                .to(domainEventsExchange)
                .with(DomainEventRoutingKeys.SUPPLY_WASTED);
    }

    @Bean
    public Binding areDeadLetterSupplyBinding(Queue areDeadLetterQueue, DirectExchange domainEventsDeadLetterExchange) {
        return BindingBuilder.bind(areDeadLetterQueue)
                .to(domainEventsDeadLetterExchange)
                .with(DomainEventRoutingKeys.SUPPLY_WASTED);
    }

    @Bean
    public Binding cashExpenseRegisteredBinding(Queue cashExpenseRegisteredQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(cashExpenseRegisteredQueue)
                .to(domainEventsExchange)
                .with(DomainEventRoutingKeys.CASH_EXPENSE_REGISTERED);
    }

    @Bean
    public Binding areDeadLetterCashRegisteredBinding(Queue areDeadLetterQueue, DirectExchange domainEventsDeadLetterExchange) {
        return BindingBuilder.bind(areDeadLetterQueue)
                .to(domainEventsDeadLetterExchange)
                .with(DomainEventRoutingKeys.CASH_EXPENSE_REGISTERED);
    }

    @Bean
    public Binding cashExpenseVoidedBinding(Queue cashExpenseVoidedQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(cashExpenseVoidedQueue)
                .to(domainEventsExchange)
                .with(DomainEventRoutingKeys.CASH_EXPENSE_VOIDED);
    }

    @Bean
    public Binding areDeadLetterCashVoidedBinding(Queue areDeadLetterQueue, DirectExchange domainEventsDeadLetterExchange) {
        return BindingBuilder.bind(areDeadLetterQueue)
                .to(domainEventsDeadLetterExchange)
                .with(DomainEventRoutingKeys.CASH_EXPENSE_VOIDED);
    }

    @Bean
    public Binding purchaseOrderCreatedBinding(Queue purchaseOrderCreatedQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(purchaseOrderCreatedQueue)
                .to(domainEventsExchange)
                .with(DomainEventRoutingKeys.PURCHASE_ORDER_CREATED);
    }

    @Bean
    public Binding areDeadLetterPurchaseCreatedBinding(Queue areDeadLetterQueue, DirectExchange domainEventsDeadLetterExchange) {
        return BindingBuilder.bind(areDeadLetterQueue)
                .to(domainEventsDeadLetterExchange)
                .with(DomainEventRoutingKeys.PURCHASE_ORDER_CREATED);
    }
}
