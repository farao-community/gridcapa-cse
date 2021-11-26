/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.app;

import com.farao_community.farao.cse.runner.api.JsonApiConverter;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.CseResponse;
import com.farao_community.farao.cse.runner.app.configurations.AmqpConfiguration;
import com.farao_community.farao.cse.runner.app.services.CseRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.stereotype.Component;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Component
public class CseListener implements MessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CseListener.class);

    private final AmqpTemplate amqpTemplate;
    private final AmqpConfiguration amqpConfiguration;
    private final JsonApiConverter jsonApiConverter;
    private final CseRunner cseServer;

    public CseListener(AmqpTemplate amqpTemplate, AmqpConfiguration amqpConfiguration, CseRunner cseServer) {
        this.amqpTemplate = amqpTemplate;
        this.amqpConfiguration = amqpConfiguration;
        this.cseServer = cseServer;
        this.jsonApiConverter = new JsonApiConverter();
    }

    @Override
    public void onMessage(Message message) {
        String replyTo = message.getMessageProperties().getReplyTo();
        String correlationId = message.getMessageProperties().getCorrelationId();
        try {
            CseRequest cseRequest = jsonApiConverter.fromJsonMessage(message.getBody(), CseRequest.class);
            LOGGER.info("Cse request received : {}", cseRequest);
            CseResponse cseResponse = cseServer.run(cseRequest);
            LOGGER.info("Cse response sent: {}", cseResponse);
            sendCseResponse(cseResponse, replyTo, correlationId);
        } catch (CseInternalException e) {
            sendErrorResponse(e, replyTo, correlationId);
        } catch (Exception e) {
            CseInternalException unknownException = new CseInternalException("Unknown exception", e);
            sendErrorResponse(unknownException, replyTo, correlationId);
        }
    }

    private void sendCseResponse(CseResponse cseResponse, String replyTo, String correlationId) {
        if (replyTo != null) {
            amqpTemplate.send(replyTo, createMessageResponse(cseResponse, correlationId));
        } else {
            amqpTemplate.send(amqpConfiguration.cseResponseExchange().getName(), "", createMessageResponse(cseResponse, correlationId));
        }
    }

    private void sendErrorResponse(CseInternalException exception, String replyTo, String correlationId) {
        if (replyTo != null) {
            amqpTemplate.send(replyTo, createErrorResponse(exception, correlationId));
        } else {
            amqpTemplate.send(amqpConfiguration.cseResponseExchange().getName(), "", createErrorResponse(exception, correlationId));
        }
    }

    private Message createErrorResponse(CseInternalException exception, String correlationId) {
        return MessageBuilder.withBody(exceptionToJsonMessage(exception))
                .andProperties(buildMessageResponseProperties(correlationId))
                .build();
    }

    private byte[] exceptionToJsonMessage(CseInternalException e) {
        return jsonApiConverter.toJsonMessage(e);
    }

    private Message createMessageResponse(CseResponse cseResponse, String correlationId) {
        return MessageBuilder.withBody(jsonApiConverter.toJsonMessage(cseResponse))
            .andProperties(buildMessageResponseProperties(correlationId))
            .build();
    }

    private MessageProperties buildMessageResponseProperties(String correlationId) {
        return MessagePropertiesBuilder.newInstance()
                .setAppId("cse-server")
                .setContentEncoding("UTF-8")
                .setContentType("application/vnd.api+json")
                .setCorrelationId(correlationId)
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                .setExpiration(amqpConfiguration.getCseResponseExpiration())
                .setPriority(1)
                .build();
    }
}
