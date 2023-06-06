/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.dichotomy.api.InterruptionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */

@Service
public class InterruptionService implements InterruptionStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(InterruptionService.class);
    private static final String STOP_RAO_BINDING = "stop-rao";

    private final StreamBridge streamBridge;
    private final Set<String> tasksToInterruptSoftly;

    public InterruptionService(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
        this.tasksToInterruptSoftly = new HashSet<>();
    }

    @Bean
    public Consumer<String> softInterrupt() {
        return this::activateSoftInterruptionFlag;
    }

    private void activateSoftInterruptionFlag(String taskId) {
        LOGGER.info("Soft interruption requested for task {}", taskId);
        streamBridge.send(STOP_RAO_BINDING, taskId);
        tasksToInterruptSoftly.add(taskId);
    }

    @Override
    public boolean shouldTaskBeInterruptedSoftly(String taskId) {
        boolean taskShouldBeInterrupted = tasksToInterruptSoftly.remove(taskId);

        if (taskShouldBeInterrupted) {
            LOGGER.info("Task {} should be interrupted softly", taskId);
        } else {
            LOGGER.info("Task {} doesn't need to be interrupted softly", taskId);
        }
        return taskShouldBeInterrupted;
    }
}
