/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

@SpringBootTest
class InterruptionServiceTest {

    @Autowired
    InterruptionService interruptionService;

    @MockBean
    StreamBridge streamBridge;

    private static class MyThread extends Thread {

        public MyThread(String id) {
            super(id);
        }

        @Override
        public void run() {
            int count = 0;
            for (int i = 0; i < 10; i++) {
                count += i;
                await().atMost(i, SECONDS);
            }
        }
    }

    @Test
    void threadInterruption() {
        MyThread th = new MyThread("myThread");
        assertFalse(isRunning("myThread").isPresent());

        th.start();
        assertTrue(isRunning("myThread").isPresent());

        interruptionService.interrupt().accept("myThread");
        assertFalse(isRunning("myThread").isPresent());
    }

    private Optional<Thread> isRunning(String id) {
        return Thread.getAllStackTraces()
                .keySet()
                .stream()
                .filter(t -> t.getName().equals(id))
                .findFirst();
    }

    @Test
    void softInterruption() {
        final String taskId = "testTask";

        assertFalse(interruptionService.shouldTaskBeInterruptedSoftly(taskId));

        interruptionService.softInterrupt().accept(taskId);
        Mockito.verify(streamBridge, Mockito.times(1)).send("stop-rao", taskId);
        assertTrue(interruptionService.shouldTaskBeInterruptedSoftly(taskId));

        assertFalse(interruptionService.shouldTaskBeInterruptedSoftly(taskId));
    }
}
