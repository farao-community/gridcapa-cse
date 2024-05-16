/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class InterruptionServiceTest {
    @Autowired
    InterruptionService interruptionService;

    @MockBean
    StreamBridge streamBridge;

    @Test
    void softInterruption() {
        String taskId = "id";

        assertFalse(interruptionService.shouldTaskBeInterruptedSoftly(taskId));

        interruptionService.softInterrupt().accept(taskId);
        Mockito.verify(streamBridge, Mockito.times(1)).send("stop-rao", taskId);
        assertTrue(interruptionService.shouldTaskBeInterruptedSoftly(taskId));

        assertFalse(interruptionService.shouldTaskBeInterruptedSoftly(taskId));
    }
}
