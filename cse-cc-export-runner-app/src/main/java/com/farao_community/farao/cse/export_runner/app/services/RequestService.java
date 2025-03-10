/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.runner.api.JsonApiConverter;
import com.farao_community.farao.cse.runner.api.exception.AbstractCseException;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.CseExportRequest;
import com.farao_community.farao.cse.runner.api.resource.CseExportResponse;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Function;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
public class RequestService {
    private static final String TASK_STATUS_UPDATE = "task-status-update";
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestService.class);
    private final CseExportRunner cseExportRunner;
    private final Logger businessLogger;
    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();
    private final StreamBridge streamBridge;

    public RequestService(CseExportRunner cseExportRunner, Logger businessLogger, StreamBridge streamBridge) {
        this.cseExportRunner = cseExportRunner;
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
        CseExportRequest cseExportRequest = jsonApiConverter.fromJsonMessage(req, CseExportRequest.class);
        // propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
        // This should be done only once, as soon as the information to add in mdc is available.
        MDC.put("gridcapa-task-id", cseExportRequest.getId());
        final OffsetDateTime startTime = OffsetDateTime.now();
        try {
            streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(cseExportRequest.getId()), TaskStatus.RUNNING));
            LOGGER.info("Cse request received : {}", cseExportRequest);
            CseExportResponse cseExportResponse = cseExportRunner.run(cseExportRequest);
            result = sendCseResponse(cseExportResponse);
            LOGGER.info("Cse response sent: {}", cseExportResponse);
        } catch (Exception e) {
            result = handleError(e, cseExportRequest.getId());
        } finally {
            logComputationTime(startTime);
        }
        return result;
    }

    private void logComputationTime(final OffsetDateTime startTime) {
        final Duration difference = Duration.between(startTime, OffsetDateTime.now());
        businessLogger.info("Summary : computation time: {}h {}min {}s since the task switched to RUNNING.",
                difference.toHours(),
                difference.toMinutesPart(),
                difference.toSecondsPart());
    }

    private byte[] sendCseResponse(CseExportResponse cseResponse) {
        if (cseResponse.isInterrupted()) {
            businessLogger.info("CSE run has been interrupted");
            streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(cseResponse.getId()), TaskStatus.INTERRUPTED));
        } else {
            streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(cseResponse.getId()), TaskStatus.SUCCESS));
        }
        return jsonApiConverter.toJsonMessage(cseResponse, CseExportResponse.class);
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
