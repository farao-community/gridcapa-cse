/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.computation;

import com.farao_community.farao.cse.network_processing.pisa_change.PiSaLinkConfiguration;
import com.farao_community.farao.cse.network_processing.pisa_change.PiSaLinkProcessor;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class BorderExchangesTest {
    private static final double TOLERANCE = 1;

    @Test
    void computeItalianImportTest() {
        String filename = "20210901_2230_test_network.uct";
        Network network = Importers.loadNetwork(filename, getClass().getResourceAsStream(filename));
        double itInitialImport = BorderExchanges.computeItalianImport(network, Collections.emptySet());
        assertEquals(6000, itInitialImport, TOLERANCE);
    }

    @Test
    void computeItalianImportWithHvdcTest() {
        String filename = "20210901_2230_test_network.uct";
        Network network = Importers.loadNetwork(filename, getClass().getResourceAsStream(filename));
        double itInitialImport = BorderExchanges.computeItalianImport(network, Set.of(new PiSaLinkProcessor(
            new PiSaLinkConfiguration("SWISS211", "ITALY311", List.of("SWISS211 XSWISS11 1"))
        )));
        assertEquals(5000, itInitialImport, TOLERANCE);
    }

    @Test
    void computeItalianImportWithNotExistingHvdcTest() {
        String filename = "20210901_2230_test_network.uct";
        Network network = Importers.loadNetwork(filename, getClass().getResourceAsStream(filename));
        double itInitialImport = BorderExchanges.computeItalianImport(network, Set.of(new PiSaLinkProcessor(
            new PiSaLinkConfiguration("FICTIV11", "FICTIV21", List.of("FICTIV11 FICTIV21 1"))
        )));
        assertEquals(6000, itInitialImport, TOLERANCE);
    }

    @Test
    void testBorderExchanges() {
        String networkFileName = "20210901_2230_test_network.uct";
        Network network = Importers.loadNetwork(networkFileName, getClass().getResourceAsStream(networkFileName));

        Map<String, Double> borderExchanges = BorderExchanges.computeCseBordersExchanges(network, Collections.emptySet());
        assertEquals(0, borderExchanges.get("IT-SI"), TOLERANCE);
        assertEquals(-2837, borderExchanges.get("IT-CH"), TOLERANCE);
        assertEquals(0, borderExchanges.get("FR-DE"), TOLERANCE);
        assertEquals(-2463, borderExchanges.get("IT-FR"), TOLERANCE);
        assertEquals(-37, borderExchanges.get("CH-FR"), TOLERANCE);
        assertEquals(0, borderExchanges.get("CH-DE"), TOLERANCE);
        assertEquals(-699, borderExchanges.get("IT-AT"), TOLERANCE);
    }

    @Test
    void testBorderExchangesWithHvdc() {
        String networkFileName = "20210901_2230_test_network.uct";
        Network network = Importers.loadNetwork(networkFileName, getClass().getResourceAsStream(networkFileName));

        Map<String, Double> borderExchanges = BorderExchanges.computeCseBordersExchanges(network, Set.of(new PiSaLinkProcessor(
            new PiSaLinkConfiguration("SWISS211", "ITALY311", List.of("SWISS211 XSWISS11 1"))
        )));
        assertEquals(0, borderExchanges.get("IT-SI"), TOLERANCE);
        assertEquals(-2837, borderExchanges.get("IT-CH"), TOLERANCE);
        assertEquals(0, borderExchanges.get("FR-DE"), TOLERANCE);
        assertEquals(-1463, borderExchanges.get("IT-FR"), TOLERANCE);
        assertEquals(-37, borderExchanges.get("CH-FR"), TOLERANCE);
        assertEquals(0, borderExchanges.get("CH-DE"), TOLERANCE);
        assertEquals(-699, borderExchanges.get("IT-AT"), TOLERANCE);
    }

    @Test
    void testCountryBalances() {
        String networkFileName = "20210901_2230_test_network.uct";
        Network network = Importers.loadNetwork(networkFileName, getClass().getResourceAsStream(networkFileName));

        Map<String, Double> borderExchanges = BorderExchanges.computeCseCountriesBalances(network, Collections.emptySet());
        assertEquals(2500, borderExchanges.get("FR"), TOLERANCE);
        assertEquals(2800, borderExchanges.get("CH"), TOLERANCE);
        assertEquals(0, borderExchanges.get("DE"), TOLERANCE);
        assertEquals(-6000, borderExchanges.get("IT"), TOLERANCE);
        assertEquals(0, borderExchanges.get("SI"), TOLERANCE);
        assertEquals(700, borderExchanges.get("AT"), TOLERANCE);
    }

    @Test
    void testCountryBalancesWithHvdc() {
        String networkFileName = "20210901_2230_test_network.uct";
        Network network = Importers.loadNetwork(networkFileName, getClass().getResourceAsStream(networkFileName));

        Map<String, Double> borderExchanges = BorderExchanges.computeCseCountriesBalances(network, Set.of(new PiSaLinkProcessor(
            new PiSaLinkConfiguration("SWISS211", "ITALY311", List.of("SWISS211 XSWISS11 1"))
        )));
        assertEquals(1500, borderExchanges.get("FR"), TOLERANCE);
        assertEquals(2800, borderExchanges.get("CH"), TOLERANCE);
        assertEquals(0, borderExchanges.get("DE"), TOLERANCE);
        assertEquals(-5000, borderExchanges.get("IT"), TOLERANCE);
        assertEquals(0, borderExchanges.get("SI"), TOLERANCE);
        assertEquals(700, borderExchanges.get("AT"), TOLERANCE);
    }
}
