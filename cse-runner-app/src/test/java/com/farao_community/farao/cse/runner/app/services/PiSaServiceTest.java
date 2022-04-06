/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.services;

import com.farao_community.farao.cse.runner.api.resource.ProcessType;
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
    private PiSaService piSaService;

    @Test
    void testPiSaPreProcessInIdccWithOneLinkInSetpoint() {
        String networkFilename = "20210901_2230_test_network_pisa_test_one_link_connected_and_setpoint.uct";
        Network network = Importers.loadNetwork(networkFilename, getClass().getResourceAsStream(networkFilename));

        assertEquals(1000, piSaService.getPiSaLink1Processor().getFrFictiveGenerator(network).getTargetP(), 1);
        assertEquals(-987, piSaService.getPiSaLink1Processor().getItFictiveGenerator(network).getTargetP(), 1);
        assertEquals(482, piSaService.getPiSaLink2Processor().getFrFictiveGenerator(network).getTargetP(), 1);
        assertEquals(-500, piSaService.getPiSaLink2Processor().getItFictiveGenerator(network).getTargetP(), 1);
        assertFalse(piSaService.getPiSaLink1Processor().isLinkConnected(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink2Processor().isLinkInACEmulation(network));

        piSaService.process(ProcessType.IDCC, network);

        // Not aligned because not connected
        assertEquals(1000, piSaService.getPiSaLink1Processor().getFrFictiveGenerator(network).getTargetP(), 1);
        assertEquals(-987, piSaService.getPiSaLink1Processor().getItFictiveGenerator(network).getTargetP(), 1);
        // Aligned because connected
        assertEquals(500, piSaService.getPiSaLink2Processor().getFrFictiveGenerator(network).getTargetP(), 1);
        assertEquals(-500, piSaService.getPiSaLink2Processor().getItFictiveGenerator(network).getTargetP(), 1);
        assertFalse(piSaService.getPiSaLink1Processor().isLinkConnected(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink2Processor().isLinkInACEmulation(network));
    }

    @Test
    void testPiSaPreProcessInIdccWithBothLinksSetpointAndACEmulation() {
        String networkFilename = "20210901_2230_test_network_pisa_test_both_link_connected_setpoint_and_emulation.uct";
        Network network = Importers.loadNetwork(networkFilename, getClass().getResourceAsStream(networkFilename));

        assertEquals(1000, piSaService.getPiSaLink1Processor().getFrFictiveGenerator(network).getTargetP(), 1);
        assertEquals(-987, piSaService.getPiSaLink1Processor().getItFictiveGenerator(network).getTargetP(), 1);
        assertEquals(0, piSaService.getPiSaLink2Processor().getFrFictiveGenerator(network).getTargetP(), 1);
        assertEquals(0, piSaService.getPiSaLink2Processor().getItFictiveGenerator(network).getTargetP(), 1);
        assertTrue(piSaService.getPiSaLink1Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink1Processor().isLinkInACEmulation(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkConnected(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkInACEmulation(network));

        piSaService.process(ProcessType.IDCC, network);

        assertEquals(1000, piSaService.getPiSaLink1Processor().getFrFictiveGenerator(network).getTargetP(), 1);
        assertEquals(-1000, piSaService.getPiSaLink1Processor().getItFictiveGenerator(network).getTargetP(), 1);
        assertEquals(-600, piSaService.getPiSaLink2Processor().getFrFictiveGenerator(network).getTargetP(), 1);
        assertEquals(600, piSaService.getPiSaLink2Processor().getItFictiveGenerator(network).getTargetP(), 1);
        assertTrue(piSaService.getPiSaLink1Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink1Processor().isLinkInACEmulation(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink2Processor().isLinkInACEmulation(network));
    }

    @Test
    void testPiSaPreProcessInD2ccWithOneLinkInSetpoint() {
        String networkFilename = "20210901_2230_test_network_pisa_test_one_link_connected_and_setpoint.uct";
        Network network = Importers.loadNetwork(networkFilename, getClass().getResourceAsStream(networkFilename));

        assertEquals(1000, piSaService.getPiSaLink1Processor().getFrFictiveGenerator(network).getTargetP(), 1);
        assertEquals(-987, piSaService.getPiSaLink1Processor().getItFictiveGenerator(network).getTargetP(), 1);
        assertEquals(482, piSaService.getPiSaLink2Processor().getFrFictiveGenerator(network).getTargetP(), 1);
        assertEquals(-500, piSaService.getPiSaLink2Processor().getItFictiveGenerator(network).getTargetP(), 1);
        assertFalse(piSaService.getPiSaLink1Processor().isLinkConnected(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink2Processor().isLinkInACEmulation(network));

        piSaService.process(ProcessType.D2CC, network);

        // Not aligned because not connected
        assertEquals(1000, piSaService.getPiSaLink1Processor().getFrFictiveGenerator(network).getTargetP(), 1);
        assertEquals(-987, piSaService.getPiSaLink1Processor().getItFictiveGenerator(network).getTargetP(), 1);
        // Aligned because connected
        assertEquals(500, piSaService.getPiSaLink2Processor().getFrFictiveGenerator(network).getTargetP(), 1);
        assertEquals(-500, piSaService.getPiSaLink2Processor().getItFictiveGenerator(network).getTargetP(), 1);
        assertFalse(piSaService.getPiSaLink1Processor().isLinkConnected(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink2Processor().isLinkInACEmulation(network));
    }

    @Test
    void testPiSaPreProcessInD2ccWithBothLinksSetpointAndACEmulation() {
        String networkFilename = "20210901_2230_test_network_pisa_test_both_link_connected_setpoint_and_emulation.uct";
        Network network = Importers.loadNetwork(networkFilename, getClass().getResourceAsStream(networkFilename));

        assertEquals(1000, piSaService.getPiSaLink1Processor().getFrFictiveGenerator(network).getTargetP(), 1);
        assertEquals(-987, piSaService.getPiSaLink1Processor().getItFictiveGenerator(network).getTargetP(), 1);
        assertEquals(0, piSaService.getPiSaLink2Processor().getFrFictiveGenerator(network).getTargetP(), 1);
        assertEquals(0, piSaService.getPiSaLink2Processor().getItFictiveGenerator(network).getTargetP(), 1);
        assertTrue(piSaService.getPiSaLink1Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink1Processor().isLinkInACEmulation(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkConnected(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkInACEmulation(network));

        piSaService.process(ProcessType.D2CC, network);

        assertEquals(1000, piSaService.getPiSaLink1Processor().getFrFictiveGenerator(network).getTargetP(), 1);
        assertEquals(-1000, piSaService.getPiSaLink1Processor().getItFictiveGenerator(network).getTargetP(), 1);
        assertEquals(0, piSaService.getPiSaLink2Processor().getFrFictiveGenerator(network).getTargetP(), 1);
        assertEquals(0, piSaService.getPiSaLink2Processor().getItFictiveGenerator(network).getTargetP(), 1);
        assertTrue(piSaService.getPiSaLink1Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink1Processor().isLinkInACEmulation(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkConnected(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkInACEmulation(network));
    }
}
