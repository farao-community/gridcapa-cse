/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@SpringBootTest
class PiSaConfigurationTest {

    @Autowired
    PiSaConfiguration piSaConfiguration;

    @Test
    void checkPisaConfigurationIsRetrievedCorrectly() {
        assertEquals(2, piSaConfiguration.link1().fictiveLines().size());
        assertEquals(2, piSaConfiguration.link2().fictiveLines().size());
        assertEquals("FFG.IL12", piSaConfiguration.link1().nodeFr());
        assertEquals("FFG.IL11", piSaConfiguration.link1().nodeIt());
        assertEquals("FFG.IL14", piSaConfiguration.link2().nodeFr());
        assertEquals("FFG.IL13", piSaConfiguration.link2().nodeIt());
        assertEquals("FFG.IL12 FFG.IL11 1", piSaConfiguration.link1().fictiveLines().get(0));
        assertEquals("FFG.IL14 FFG.IL13 1", piSaConfiguration.link2().fictiveLines().get(0));

    }
}
