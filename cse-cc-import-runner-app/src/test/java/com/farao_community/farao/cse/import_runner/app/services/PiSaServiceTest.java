/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class PiSaServiceTest {
    private static final double DOUBLE_TOLERANCE = 1;

    @Autowired
    private PiSaService piSaService;

    @Test
    void testPiSaPreProcessInIdccWithOneLinkInSetpoint() throws IOException {
        String networkFilename = "20210901_2230_test_network_pisa_test_one_link_connected_and_setpoint.uct";
        Network network = Network.read(networkFilename, getClass().getResourceAsStream(networkFilename));

        assertEquals(1000, piSaService.getPiSaLink1Processor().getFrFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(-987, piSaService.getPiSaLink1Processor().getItFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(482, piSaService.getPiSaLink2Processor().getFrFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(-500, piSaService.getPiSaLink2Processor().getItFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertFalse(piSaService.getPiSaLink1Processor().isLinkConnected(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink2Processor().isLinkInACEmulation(network));

        piSaService.alignGenerators(network);
        String cracFilename = "cse_crac_with_hvdc.xml";
        Crac crac = Crac.readWithContext(cracFilename, getClass().getResourceAsStream(cracFilename), network, OffsetDateTime.parse("2021-09-01T22:30Z"), CracCreationParameters.load()).getCrac();
        // Already in set-point for the connected link so nothing is done
        piSaService.forceSetPoint(ProcessType.IDCC, network, crac);

        // Not aligned because not connected
        assertEquals(1000, piSaService.getPiSaLink1Processor().getFrFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(-987, piSaService.getPiSaLink1Processor().getItFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        // Aligned because connected
        assertEquals(500, piSaService.getPiSaLink2Processor().getFrFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(-500, piSaService.getPiSaLink2Processor().getItFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertFalse(piSaService.getPiSaLink1Processor().isLinkConnected(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink2Processor().isLinkInACEmulation(network));
    }

    @Test
    void testPiSaPreProcessInIdccWithBothLinksSetpointAndACEmulation() throws IOException {
        String networkFilename = "20210901_2230_test_network_pisa_test_both_links_connected_setpoint_and_emulation.uct";
        Network network = Network.read(networkFilename, getClass().getResourceAsStream(networkFilename));

        assertEquals(1000, piSaService.getPiSaLink1Processor().getFrFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(-987, piSaService.getPiSaLink1Processor().getItFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(0, piSaService.getPiSaLink2Processor().getFrFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(0, piSaService.getPiSaLink2Processor().getItFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertTrue(piSaService.getPiSaLink1Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink1Processor().isLinkInACEmulation(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkConnected(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkInACEmulation(network));

        piSaService.alignGenerators(network);
        String cracFilename = "cse_crac_with_hvdc.xml";
        Crac crac = Crac.readWithContext(cracFilename, getClass().getResourceAsStream(cracFilename), network, OffsetDateTime.parse("2021-09-01T22:30Z"), CracCreationParameters.load()).getCrac();
        // Link 1 already in set-point so nothing is done but link 2 is forced
        piSaService.forceSetPoint(ProcessType.IDCC, network, crac);

        assertEquals(1000, piSaService.getPiSaLink1Processor().getFrFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(-1000, piSaService.getPiSaLink1Processor().getItFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(-200, piSaService.getPiSaLink2Processor().getFrFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(200, piSaService.getPiSaLink2Processor().getItFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertTrue(piSaService.getPiSaLink1Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink1Processor().isLinkInACEmulation(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink2Processor().isLinkInACEmulation(network));
    }

    @Test
    void testPiSaPreProcessInD2ccWithOneLinkInSetpoint() throws IOException {
        String networkFilename = "20210901_2230_test_network_pisa_test_one_link_connected_and_setpoint.uct";
        Network network = Network.read(networkFilename, getClass().getResourceAsStream(networkFilename));

        assertEquals(1000, piSaService.getPiSaLink1Processor().getFrFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(-987, piSaService.getPiSaLink1Processor().getItFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(482, piSaService.getPiSaLink2Processor().getFrFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(-500, piSaService.getPiSaLink2Processor().getItFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertFalse(piSaService.getPiSaLink1Processor().isLinkConnected(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink2Processor().isLinkInACEmulation(network));

        piSaService.alignGenerators(network);
        String cracFilename = "cse_crac_with_hvdc.xml";
        Crac crac = Crac.readWithContext(cracFilename, getClass().getResourceAsStream(cracFilename), network, OffsetDateTime.parse("2021-09-01T22:30Z"), CracCreationParameters.load()).getCrac();
        // In D2CC nothing is done
        piSaService.forceSetPoint(ProcessType.D2CC, network, crac);

        // Not aligned because not connected
        assertEquals(1000, piSaService.getPiSaLink1Processor().getFrFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(-987, piSaService.getPiSaLink1Processor().getItFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        // Aligned because connected
        assertEquals(500, piSaService.getPiSaLink2Processor().getFrFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(-500, piSaService.getPiSaLink2Processor().getItFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertFalse(piSaService.getPiSaLink1Processor().isLinkConnected(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink2Processor().isLinkInACEmulation(network));
    }

    @Test
    void testPiSaPreProcessInD2ccWithBothLinksSetpointAndACEmulation() throws IOException {
        String networkFilename = "20210901_2230_test_network_pisa_test_both_links_connected_setpoint_and_emulation.uct";
        Network network = Network.read(networkFilename, getClass().getResourceAsStream(networkFilename));

        assertEquals(1000, piSaService.getPiSaLink1Processor().getFrFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(-987, piSaService.getPiSaLink1Processor().getItFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(0, piSaService.getPiSaLink2Processor().getFrFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(0, piSaService.getPiSaLink2Processor().getItFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertTrue(piSaService.getPiSaLink1Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink1Processor().isLinkInACEmulation(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkConnected(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkInACEmulation(network));

        piSaService.alignGenerators(network);
        String cracFilename = "cse_crac_with_hvdc.xml";
        Crac crac = Crac.readWithContext(cracFilename, getClass().getResourceAsStream(cracFilename), network, OffsetDateTime.parse("2021-09-01T22:30Z"), CracCreationParameters.load()).getCrac();
        // In D2CC nothing is done
        piSaService.forceSetPoint(ProcessType.D2CC, network, crac);

        assertEquals(1000, piSaService.getPiSaLink1Processor().getFrFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(-1000, piSaService.getPiSaLink1Processor().getItFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(0, piSaService.getPiSaLink2Processor().getFrFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(0, piSaService.getPiSaLink2Processor().getItFictiveGenerator(network).getTargetP(), DOUBLE_TOLERANCE);
        assertTrue(piSaService.getPiSaLink1Processor().isLinkConnected(network));
        assertFalse(piSaService.getPiSaLink1Processor().isLinkInACEmulation(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkConnected(network));
        assertTrue(piSaService.getPiSaLink2Processor().isLinkInACEmulation(network));
    }
}
