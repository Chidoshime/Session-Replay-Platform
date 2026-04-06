package com.sessionreplay.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.listener.simple.prefetch:10}")
    private int prefetchCount;

    public static final String SESSION_EVENTS_QUEUE = "session.events.queue";
    public static final String SESSION_EVENTS_EXCHANGE = "session.events.exchange";
    public static final String SESSION_EVENTS_ROUTING_KEY = "session.event";

    @Bean
    public Queue sessionEventsQueue() {
        return QueueBuilder.durable(SESSION_EVENTS_QUEUE)
                .withArgument("x-max-priority", 10)
                .build();
    }

    @Bean
    public TopicExchange sessionEventsExchange() {
        return new TopicExchange(SESSION_EVENTS_EXCHANGE);
    }

    @Bean
    public Binding sessionEventsBinding(Queue sessionEventsQueue, TopicExchange sessionEventsExchange) {
        return BindingBuilder.bind(sessionEventsQueue)
                .to(sessionEventsExchange)
                .with(SESSION_EVENTS_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
