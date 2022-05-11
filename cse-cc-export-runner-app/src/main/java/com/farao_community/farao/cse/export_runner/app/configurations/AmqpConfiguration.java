/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.configurations;

import com.farao_community.farao.cse.export_runner.app.CseExportListener;
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
    private String cseExportResponseDestination;
    @Value("${cse-cc-runner.bindings.response.expiration}")
    private String cseExportResponseExpiration;
    @Value("${cse-cc-runner.bindings.request.destination}")
    private String cseExportRequestDestination;
    @Value("${cse-cc-runner.bindings.request.routing-key}")
    private String cseExportRequestRoutingKey;
    @Value("${cse-cc-runner.bindings.request.group}")
    private String cseExportRequestGroup;

    @Bean
    public Queue cseExportRequestQueue() {
        return new Queue(cseExportRequestDestination + "." + cseExportRequestGroup);
    }

    @Bean
    public TopicExchange cseExportTopicExchange() {
        return new TopicExchange(cseExportRequestDestination);
    }

    @Bean
    public Binding cseExportRequestBinding() {
        return BindingBuilder.bind(cseExportRequestQueue()).to(cseExportTopicExchange()).with(Optional.ofNullable(cseExportRequestRoutingKey).orElse("#"));
    }

    @Bean
    public MessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory, Queue cseExportRequestQueue, CseExportListener listener) {
        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(connectionFactory);
        simpleMessageListenerContainer.setQueues(cseExportRequestQueue);
        simpleMessageListenerContainer.setMessageListener(listener);
        simpleMessageListenerContainer.setPrefetchCount(1);
        return simpleMessageListenerContainer;
    }

    @Bean
    public FanoutExchange cseExportResponseExchange() {
        return new FanoutExchange(cseExportResponseDestination);
    }

    public String getCseExportResponseExpiration() {
        return cseExportResponseExpiration;
    }
}
