/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@SpringBootTest
class AmqpConfigurationTest {
    @Autowired
    private AmqpConfiguration amqpConfiguration;

    @Autowired
    private Queue cseExportRequestQueue;

    @Autowired
    private FanoutExchange cseExportResponseExchange;

    @Test
    void checkAmqpConfiguration() {
        assertNotNull(cseExportRequestQueue);
        assertNotNull(cseExportResponseExchange);
        assertEquals("cse-export-request.cse-export-runner", cseExportRequestQueue.getName());
        assertEquals("cse-export-response", cseExportResponseExchange.getName());
    }
}
