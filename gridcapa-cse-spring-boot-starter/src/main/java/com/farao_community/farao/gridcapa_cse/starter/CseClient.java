/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_cse.starter;

import com.farao_community.farao.gridcapa_cse.api.resource.CseRequest;
import com.farao_community.farao.gridcapa_cse.api.resource.CseResponse;
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

    public CseResponse run(CseRequest cseRequest, int priority) {
        LOGGER.info("Cse request sent: {}", cseRequest);
        Message responseMessage = amqpTemplate.sendAndReceive(cseClientProperties.getAmqp().getExchange(), cseClientProperties.getAmqp().getRoutingKey(), cseMessageHandler.buildMessage(cseRequest, priority));
        CseResponse cseResponse = cseMessageHandler.readMessage(responseMessage);
        LOGGER.info("Cse response received: {}", cseResponse);
        return cseResponse;
    }

    public CseResponse run(CseRequest cseRequest) {
        return run(cseRequest, DEFAULT_PRIORITY);
    }
}
