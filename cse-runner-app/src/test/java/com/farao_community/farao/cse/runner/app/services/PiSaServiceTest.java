/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.services;

import com.farao_community.farao.cse.runner.app.configurations.PiSaConfiguration;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class PiSaServiceTest {

    @Autowired
    private PiSaConfiguration piSaConfiguration;

    @Autowired
    private PiSaService piSaService;

    @Test
    void testGeneratorAlignment() {
        String networkFilename = "20210901_2230_test_network_pisa_test.uct";
        Network network = Importers.loadNetwork(networkFilename, getClass().getResourceAsStream(networkFilename));

        assertEquals(1000, piSaService.getGenerator(network, piSaConfiguration.getPiSaLink1NodeFr()).getTargetP());
        assertEquals(-987, piSaService.getGenerator(network, piSaConfiguration.getPiSaLink1NodeIt()).getTargetP());
        assertEquals(482, piSaService.getGenerator(network, piSaConfiguration.getPiSaLink2NodeFr()).getTargetP());
        assertEquals(-500, piSaService.getGenerator(network, piSaConfiguration.getPiSaLink2NodeIt()).getTargetP());

        piSaService.alignFictiveGenerators(network);

        assertEquals(1000, piSaService.getGenerator(network, piSaConfiguration.getPiSaLink1NodeFr()).getTargetP());
        assertEquals(-1000, piSaService.getGenerator(network, piSaConfiguration.getPiSaLink1NodeIt()).getTargetP());
        assertEquals(500, piSaService.getGenerator(network, piSaConfiguration.getPiSaLink2NodeFr()).getTargetP());
        assertEquals(-500, piSaService.getGenerator(network, piSaConfiguration.getPiSaLink2NodeIt()).getTargetP());
    }
}
