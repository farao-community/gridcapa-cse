/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.network_processing.pisa_change;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class PiSaLinkProcessorTest {

    private PiSaLinkConfiguration piSaLink1Configuration;
    private PiSaLinkProcessor piSaLink1Processor;
    private PiSaLinkConfiguration piSaLink2Configuration;
    private PiSaLinkProcessor piSaLink2Processor;

    @BeforeEach
    void setUp() {
        piSaLink1Configuration = new PiSaLinkConfiguration(
            "SWISS111",
            "SWISS211",
            List.of("SWISS111 SWISS211 1", "SWISS111 SWISS211 2", "SWISS111 SWISS211 3")
        );
        piSaLink1Processor = new PiSaLinkProcessor(piSaLink1Configuration);
        piSaLink2Configuration = new PiSaLinkConfiguration(
            "SWISS311",
            "SWISS411",
            List.of("SWISS311 SWISS411 1", "SWISS311 SWISS411 2", "SWISS311 SWISS411 3")
        );
        piSaLink2Processor = new PiSaLinkProcessor(piSaLink2Configuration);
    }

    @Test
    void testCheckPiSaWithMissingElement() {
        String networkFilename = "20210901_2230_test_network_pisa_uncomplete_model_test.uct";
        Network network = Importers.loadNetwork(networkFilename, getClass().getResourceAsStream(networkFilename));

        // Missing generator
        assertThrows(PiSaLinkException.class, () -> piSaLink1Processor.isLinkPresent(network));
        // Missing fictive lines
        assertThrows(PiSaLinkException.class, () -> piSaLink2Processor.isLinkPresent(network));
    }

    @Test
    void testCheckPiSaWithCompleteModels() {
        String networkFilename = "20210901_2230_test_network_pisa_no_model_test.uct";
        Network network = Importers.loadNetwork(networkFilename, getClass().getResourceAsStream(networkFilename));

        assertTrue(piSaLink1Processor.isLinkPresent(network));
        assertFalse(piSaLink2Processor.isLinkPresent(network));
    }

    @Test
    void testCheckConnectedLinks() {
        String networkFilename = "20210901_2230_test_network_pisa_test.uct";
        Network network = Importers.loadNetwork(networkFilename, getClass().getResourceAsStream(networkFilename));

        assertFalse(piSaLink1Processor.isLinkConnected(network));
        assertTrue(piSaLink2Processor.isLinkConnected(network));
    }

    @Test
    void testGeneratorAlignment() {
        String networkFilename = "20210901_2230_test_network_pisa_test.uct";
        Network network = Importers.loadNetwork(networkFilename, getClass().getResourceAsStream(networkFilename));

        assertEquals(1000, PiSaLinkProcessor.getGenerator(network, piSaLink1Configuration.getPiSaLinkFictiveNodeFr()).getTargetP());
        assertEquals(-987, PiSaLinkProcessor.getGenerator(network, piSaLink1Configuration.getPiSaLinkFictiveNodeIt()).getTargetP());
        assertEquals(482, PiSaLinkProcessor.getGenerator(network, piSaLink2Configuration.getPiSaLinkFictiveNodeFr()).getTargetP());
        assertEquals(-500, PiSaLinkProcessor.getGenerator(network, piSaLink2Configuration.getPiSaLinkFictiveNodeIt()).getTargetP());

        piSaLink1Processor.alignFictiveGenerators(network);
        piSaLink2Processor.alignFictiveGenerators(network);

        assertEquals(1000, PiSaLinkProcessor.getGenerator(network, piSaLink1Configuration.getPiSaLinkFictiveNodeFr()).getTargetP());
        assertEquals(-1000, PiSaLinkProcessor.getGenerator(network, piSaLink1Configuration.getPiSaLinkFictiveNodeIt()).getTargetP());
        assertEquals(500, PiSaLinkProcessor.getGenerator(network, piSaLink2Configuration.getPiSaLinkFictiveNodeFr()).getTargetP());
        assertEquals(-500, PiSaLinkProcessor.getGenerator(network, piSaLink2Configuration.getPiSaLinkFictiveNodeIt()).getTargetP());
    }

    @Test
    void testLinkMode() {
        String networkFilename = "20210901_2230_test_network_different_link_config_test.uct";
        Network network = Importers.loadNetwork(networkFilename, getClass().getResourceAsStream(networkFilename));

        assertFalse(piSaLink1Processor.isLinkInACEmulation(network));
        assertTrue(piSaLink2Processor.isLinkInACEmulation(network));
    }

    @Test
    void testForcingSetpointMode() {
        String networkFilename = "20210901_2230_test_network_different_link_config_test.uct";
        Network network = Importers.loadNetwork(networkFilename, getClass().getResourceAsStream(networkFilename));

        piSaLink2Processor.setLinkInSetpointMode(network);
        assertFalse(piSaLink2Processor.isLinkInACEmulation(network));
        assertEquals(600, PiSaLinkProcessor.getGenerator(network, piSaLink2Configuration.getPiSaLinkFictiveNodeIt()).getTargetP(), 1);
    }
}
