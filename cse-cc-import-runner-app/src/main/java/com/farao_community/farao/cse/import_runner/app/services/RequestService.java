/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.runner.api.JsonApiConverter;
import com.farao_community.farao.cse.runner.api.exception.AbstractCseException;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.CseResponse;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.function.Function;

@Service
public class RequestService {
    private static final String TASK_STATUS_UPDATE = "task-status-update";
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestService.class);
    private final CseRunner cseServer;
    private final Logger businessLogger;
    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();
    private final StreamBridge streamBridge;

    public RequestService(CseRunner cseServer, Logger businessLogger, StreamBridge streamBridge) {
        this.cseServer = cseServer;
        this.businessLogger = businessLogger;
        this.streamBridge = streamBridge;
    }

    @Bean
    public Function<Flux<byte[]>, Flux<byte[]>> request(RequestService requestService) {
        return cseRequestFlux -> cseRequestFlux
                .map(this::launchCseRequest)
                .log();
    }

    public byte[] launchCseRequest(byte[] req) {
        byte[] result;
        CseRequest cseRequest = jsonApiConverter.fromJsonMessage(req, CseRequest.class);
        // propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
        // This should be done only once, as soon as the information to add in mdc is available.
        MDC.put("gridcapa-task-id", cseRequest.getId());
        try {
            streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(cseRequest.getId()), TaskStatus.RUNNING));
            LOGGER.info("Cse request received : {}", cseRequest);
            CseResponse cseResponse = cseServer.run(cseRequest);
            result = sendCseResponse(cseResponse);
            LOGGER.info("Cse response sent: {}", cseResponse);
        } catch (Exception e) {
            result = handleError(e, cseRequest.getId());
        }
        return result;
    }

    private byte[] sendCseResponse(CseResponse cseResponse) {
        if (cseResponse.isInterrupted()) {
            businessLogger.info("CSE run has been interrupted");
            streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(cseResponse.getId()), TaskStatus.INTERRUPTED));
        } else {
            streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(cseResponse.getId()), TaskStatus.SUCCESS));
        }
        return jsonApiConverter.toJsonMessage(cseResponse, CseResponse.class);
    }

    private byte[] handleError(Exception e, String requestId) {
        AbstractCseException cseException = new CseInternalException("CSE run failed", e);
        LOGGER.error(cseException.getDetails(), cseException);
        businessLogger.error(cseException.getDetails());
        return sendErrorResponse(requestId, cseException);
    }

    private byte[] sendErrorResponse(String requestId, AbstractCseException exception) {
        streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(requestId), TaskStatus.ERROR));
        return exceptionToJsonMessage(exception);
    }

    private byte[] exceptionToJsonMessage(AbstractCseException e) {
        return jsonApiConverter.toJsonMessage(e);
    }

}
