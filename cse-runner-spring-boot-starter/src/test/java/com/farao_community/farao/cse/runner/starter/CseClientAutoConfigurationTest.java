/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.starter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
class CseClientAutoConfigurationTest {
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    public void createContext() {
        context = new AnnotationConfigApplicationContext();
    }

    @AfterEach
    public void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void registerCseClient() {
        context.registerBean("amqpTemplate", AmqpTemplate.class, () -> Mockito.mock(AmqpTemplate.class));
        context.register(CseClientAutoConfiguration.class);
        context.refresh();
        CseClient client = context.getBean(CseClient.class);
        assertNotNull(client);
    }

}

