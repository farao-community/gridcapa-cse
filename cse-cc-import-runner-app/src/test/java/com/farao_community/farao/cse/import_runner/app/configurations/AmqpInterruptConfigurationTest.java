/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Thieulin Sébastien {@literal <sebastien.thieulin at rte-france.com>}
 */
@SpringBootTest
class AmqpInterruptConfigurationTest {

    @Autowired
    private AmqpInterruptConfiguration amqpConfiguration;

    @Autowired
    private Queue cseInterruptQueue;

    @Autowired
    private FanoutExchange cseResponseExchange;

    @Test
    void checkAmqpMessageConfiguration() {
        assertNotNull(amqpConfiguration);
        assertNotNull(cseInterruptQueue);
        assertEquals("cse-interrupt.cse-runner", cseInterruptQueue.getName());
        assertNotNull(cseResponseExchange);
        assertEquals("cse-response", cseResponseExchange.getName());
        assertEquals("60000", amqpConfiguration.getCseInterruptResponseExpiration());
    }
}
