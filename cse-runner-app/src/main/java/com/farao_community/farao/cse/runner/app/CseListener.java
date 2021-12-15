/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.app;

import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.CseResponse;
import com.farao_community.farao.cse.runner.app.services.CseRunner;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Function;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Component
public class CseListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CseListener.class);
    private static final String TASK_STATUS_UPDATE_BINDING = "task-status-update";

    private final StreamBridge streamBridge;
    private final CseRunner cseServer;

    public CseListener(StreamBridge streamBridge, CseRunner cseServer) {
        this.streamBridge = streamBridge;
        this.cseServer = cseServer;
    }

    @Bean
    public Function<CseRequest, CseResponse> handleRun() {
        return cseRequest -> {
            try {
                LOGGER.info("Cse request received : {}", cseRequest);
                streamBridge.send(TASK_STATUS_UPDATE_BINDING, new TaskStatusUpdate(UUID.fromString(cseRequest.getId()), TaskStatus.RUNNING));
                CseResponse cseResponse = cseServer.run(cseRequest);
                LOGGER.info("Cse response sent: {}", cseResponse);
                streamBridge.send(TASK_STATUS_UPDATE_BINDING, new TaskStatusUpdate(UUID.fromString(cseRequest.getId()), TaskStatus.SUCCESS));
                return cseResponse;
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
                streamBridge.send(TASK_STATUS_UPDATE_BINDING, new TaskStatusUpdate(UUID.fromString(cseRequest.getId()), TaskStatus.ERROR));
                throw new CseInternalException("Not handled exception", e);
            }
        };
    }
}
