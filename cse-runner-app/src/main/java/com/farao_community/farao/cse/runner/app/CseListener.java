/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.app;

import com.farao_community.farao.cse.runner.api.JsonApiConverter;
import com.farao_community.farao.cse.runner.api.exception.AbstractCseException;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.CseResponse;
import com.farao_community.farao.cse.runner.app.configurations.AmqpConfiguration;
import com.farao_community.farao.cse.runner.app.services.CseRunner;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.*;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Component
public class CseListener implements MessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CseListener.class);
    private static final String TASK_STATUS_UPDATE = "task-status-update";

    private final AmqpTemplate amqpTemplate;
    private final StreamBridge streamBridge;
    private final AmqpConfiguration amqpConfiguration;
    private final JsonApiConverter jsonApiConverter;
    private final CseRunner cseServer;
    private final Logger businessLogger;

    public CseListener(AmqpTemplate amqpTemplate, StreamBridge streamBridge, AmqpConfiguration amqpConfiguration, CseRunner cseServer, Logger businessLogger) {
        this.amqpTemplate = amqpTemplate;
        this.streamBridge = streamBridge;
        this.amqpConfiguration = amqpConfiguration;
        this.cseServer = cseServer;
        this.jsonApiConverter = new JsonApiConverter();
        this.businessLogger = businessLogger;
    }

    @Override
    public void onMessage(Message message) {
        String replyTo = message.getMessageProperties().getReplyTo();
        String correlationId = message.getMessageProperties().getCorrelationId();
        CseRequest cseRequest = jsonApiConverter.fromJsonMessage(message.getBody(), CseRequest.class);
        // propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
        // This should be done only once, as soon as the information to add in mdc is available.
        MDC.put("gridcapa-task-id", cseRequest.getId());
        try {
            streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(cseRequest.getId()), TaskStatus.RUNNING));
            LOGGER.info("Cse request received : {}", cseRequest);
            CseResponse cseResponse = cseServer.run(cseRequest);
            LOGGER.info("Cse response sent: {}", cseResponse);
            sendCseResponse(cseResponse, replyTo, correlationId);
        } catch (AbstractCseException e) {
            handleError(e, cseRequest.getId(), replyTo, correlationId);
        } catch (Exception e) {
            CseInternalException unknownException = new CseInternalException("Unknown exception", e);
            handleError(unknownException, cseRequest.getId(), replyTo, correlationId);
        }
    }

    private void sendCseResponse(CseResponse cseResponse, String replyTo, String correlationId) {
        streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(cseResponse.getId()), TaskStatus.SUCCESS));
        if (replyTo != null) {
            amqpTemplate.send(replyTo, createMessageResponse(cseResponse, correlationId));
        } else {
            amqpTemplate.send(amqpConfiguration.cseResponseExchange().getName(), "", createMessageResponse(cseResponse, correlationId));
        }
    }

    private void handleError(AbstractCseException e, String requestId, String replyTo, String correlationId) {
        LOGGER.error(e.getDetails(), e);
        businessLogger.error(e.getDetails(), e);
        sendErrorResponse(requestId, e, replyTo, correlationId);
    }

    private void sendErrorResponse(String requestId, AbstractCseException exception, String replyTo, String correlationId) {
        streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(requestId), TaskStatus.ERROR));
        if (replyTo != null) {
            amqpTemplate.send(replyTo, createErrorResponse(exception, correlationId));
        } else {
            amqpTemplate.send(amqpConfiguration.cseResponseExchange().getName(), "", createErrorResponse(exception, correlationId));
        }
    }

    private Message createErrorResponse(AbstractCseException exception, String correlationId) {
        return MessageBuilder.withBody(exceptionToJsonMessage(exception))
                .andProperties(buildMessageResponseProperties(correlationId))
                .build();
    }

    private byte[] exceptionToJsonMessage(AbstractCseException e) {
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
