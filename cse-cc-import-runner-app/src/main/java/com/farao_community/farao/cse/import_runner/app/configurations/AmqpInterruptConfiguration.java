/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.configurations;

import com.farao_community.farao.cse.import_runner.app.CseInterruptListener;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Configuration
public class AmqpInterruptConfiguration {

    @Value("${cse-cc-runner.bindings.response.destination}")
    private String cseResponseDestination;
    @Value("${cse-cc-runner.bindings.response.expiration}")
    private String cseResponseExpiration;
    @Value("${cse-cc-runner.bindings.interrupt.destination}")
    private String cseInterruptDestination;
    @Value("${cse-cc-runner.bindings.interrupt.routing-key}")
    private String cseInterruptRoutingKey;
    @Value("${cse-cc-runner.bindings.interrupt.group}")
    private String cseInterruptGroup;

    @Value("${cse-cc-runner.bindings.max-concurrent}")
    private String maxconcurrent;

    @Bean
    public Queue cseInterruptQueue() {
        return new Queue(cseInterruptDestination + "." + cseInterruptGroup);
    }

    @Bean
    public TopicExchange cseInterruptTopicExchange() {
        return new TopicExchange(cseInterruptDestination);
    }

    @Bean
    public Binding cseInterruptBinding() {
        return BindingBuilder.bind(cseInterruptQueue()).to(cseInterruptTopicExchange()).with(Optional.ofNullable(cseInterruptRoutingKey).orElse("#"));
    }

    @Bean
    public MessageListenerContainer messageInterruptListenerContainer(ConnectionFactory connectionFactory, Queue cseInterruptQueue, CseInterruptListener listener) {
        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(connectionFactory);
        simpleMessageListenerContainer.setQueues(cseInterruptQueue);
        simpleMessageListenerContainer.setMessageListener(listener);
        simpleMessageListenerContainer.setPrefetchCount(1);
        simpleMessageListenerContainer.setConcurrency(maxconcurrent);
        return simpleMessageListenerContainer;
    }

    @Bean
    public FanoutExchange cseInterruptResponseExchange() {
        return new FanoutExchange(cseResponseDestination);
    }

    public String getCseInterruptResponseExpiration() {
        return cseResponseExpiration;
    }
}
