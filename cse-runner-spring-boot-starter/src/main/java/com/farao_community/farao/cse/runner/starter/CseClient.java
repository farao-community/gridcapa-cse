/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public class CseClient {
    private static final int DEFAULT_PRIORITY = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(CseClient.class);

    private final AmqpTemplate amqpTemplate;
    private final CseClientProperties cseClientProperties;
    private final CseMessageHandler cseMessageHandler;

    public CseClient(AmqpTemplate amqpTemplate, CseClientProperties cseClientProperties) {
        this.amqpTemplate = amqpTemplate;
        this.cseClientProperties = cseClientProperties;
        this.cseMessageHandler = new CseMessageHandler(cseClientProperties);
    }

    public <I, J> J run(I request, Class<I> requestClass, Class<J> responseClass, int priority) {
        LOGGER.info("Request sent: {}", request);
        Message responseMessage = amqpTemplate.sendAndReceive(
                cseClientProperties.getBinding().getDestination(),
                cseClientProperties.getBinding().getRoutingKey(),
                cseMessageHandler.buildMessage(request, requestClass, priority)
        );
        J response = cseMessageHandler.readMessage(responseMessage, responseClass);
        LOGGER.info("Response received: {}", response);
        return response;
    }

    public <I, J> J run(I request, Class<I> requestClass, Class<J> responseClass) {
        return run(request, requestClass, responseClass, DEFAULT_PRIORITY);
    }
}
