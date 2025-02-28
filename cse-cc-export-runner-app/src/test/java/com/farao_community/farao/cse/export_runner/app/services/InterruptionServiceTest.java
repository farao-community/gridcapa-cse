/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class InterruptionServiceTest {

    @Mock
    private Logger businessLogger;

    @InjectMocks
    private InterruptionService interruptionService;
    private static final String TASK_ID = "taskId";

    @BeforeEach
    void setUp() {
        interruptionService = new InterruptionService(businessLogger);
    }

    @Test
    void shouldMarkRunForSoftInterruption() {
        interruptionService.softInterrupt().accept(TASK_ID);

        assertTrue(interruptionService.shouldRunBeInterruptedSoftly(TASK_ID));
        verify(businessLogger).warn("Soft interruption requested");
    }

    @Test
    void shouldNotInterruptRunThatWasNotMarked() {
        assertFalse(interruptionService.shouldRunBeInterruptedSoftly(TASK_ID));
    }
}
