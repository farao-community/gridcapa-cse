/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_cse.app.configurations;

import com.farao_community.farao.gridcapa_cse.app.CseListener;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Configuration
public class AmqpConfiguration {

    @Value("${cse-cc.messages.cse-response.exchange}")
    private String cseResponseExchange;
    @Value("${cse-cc.messages.cse-response.expiration}")
    private String cseResponseExpiration;
    @Value("${cse-cc.messages.cse-request.queue-name}")
    private String cseRequestQueueName;

    @Bean
    public Queue cseRequestQueue() {
        return new Queue(cseRequestQueueName);
    }

    @Bean
    public MessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory, Queue cseRequestQueue, CseListener listener) {
        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(connectionFactory);
        simpleMessageListenerContainer.setQueues(cseRequestQueue);
        simpleMessageListenerContainer.setMessageListener(listener);
        return simpleMessageListenerContainer;
    }

    @Bean
    public FanoutExchange cseResponseExchange() {
        return new FanoutExchange(cseResponseExchange);
    }

    public String getCseResponseExpiration() {
        return cseResponseExpiration;
    }
}
