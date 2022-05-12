/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.configurations;

import com.farao_community.farao.cse.import_runner.app.CseListener;
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
public class AmqpConfiguration {

    @Value("${cse-cc-runner.bindings.response.destination}")
    private String cseResponseDestination;
    @Value("${cse-cc-runner.bindings.response.expiration}")
    private String cseResponseExpiration;
    @Value("${cse-cc-runner.bindings.request.destination}")
    private String cseRequestDestination;
    @Value("${cse-cc-runner.bindings.request.routing-key}")
    private String cseRequestRoutingKey;
    @Value("${cse-cc-runner.bindings.request.group}")
    private String cseRequestGroup;

    @Bean
    public Queue cseRequestQueue() {
        return new Queue(cseRequestDestination + "." + cseRequestGroup);
    }

    @Bean
    public TopicExchange cseTopicExchange() {
        return new TopicExchange(cseRequestDestination);
    }

    @Bean
    public Binding cseRequestBinding() {
        return BindingBuilder.bind(cseRequestQueue()).to(cseTopicExchange()).with(Optional.ofNullable(cseRequestRoutingKey).orElse("#"));
    }

    @Bean
    public MessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory, Queue cseRequestQueue, CseListener listener) {
        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(connectionFactory);
        simpleMessageListenerContainer.setQueues(cseRequestQueue);
        simpleMessageListenerContainer.setMessageListener(listener);
        simpleMessageListenerContainer.setPrefetchCount(1);
        return simpleMessageListenerContainer;
    }

    @Bean
    public FanoutExchange cseResponseExchange() {
        return new FanoutExchange(cseResponseDestination);
    }

    public String getCseResponseExpiration() {
        return cseResponseExpiration;
    }
}
