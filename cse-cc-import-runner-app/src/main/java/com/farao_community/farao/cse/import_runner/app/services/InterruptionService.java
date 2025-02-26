/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.dichotomy.api.InterruptionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 * @author Daniel Thirion {@literal <daniel.thirion at rte-france.com>}
 */
@Service
public class InterruptionService implements InterruptionStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(InterruptionService.class);

    private final Logger businessLogger;
    private final Set<String> runsToInterruptSoftly;

    public InterruptionService(final Logger businessLogger) {
        this.businessLogger = businessLogger;
        this.runsToInterruptSoftly = new HashSet<>();
    }

    @Bean
    public Consumer<String> softInterrupt() {
        return this::activateSoftInterruptionFlag;
    }

    private void activateSoftInterruptionFlag(final String runId) {
        LOGGER.info("Soft interruption requested for Run {}", runId);
        runsToInterruptSoftly.add(runId);
    }

    @Override
    public boolean shouldRunBeInterruptedSoftly(final String runId) {
        final boolean runShouldBeInterrupted = runsToInterruptSoftly.remove(runId);
        if (runShouldBeInterrupted) {
            businessLogger.warn("Soft interruption requested");
            LOGGER.info("Run {} should be interrupted softly", runId);
        } else {
            LOGGER.info("Run {} doesn't need to be interrupted softly", runId);
        }
        return runShouldBeInterrupted;
    }
}
