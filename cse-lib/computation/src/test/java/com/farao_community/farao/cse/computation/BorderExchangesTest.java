/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.computation;

import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class BorderExchangesTest {
    private static final double TOLERANCE = 1;

    @Test
    void computeItalianImportTest() {
        String filename = "20210901_2230_test_network.uct";
        Network network = Network.read(filename, getClass().getResourceAsStream(filename));
        double itInitialImport = BorderExchanges.computeItalianImport(network);
        assertEquals(6000, itInitialImport, TOLERANCE);
    }

    @Test
    void testBorderExchanges() {
        String networkFileName = "20210901_2230_test_network.uct";
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));

        Map<String, Double> borderExchanges = BorderExchanges.computeCseBordersExchanges(network);
        assertEquals(0, borderExchanges.get("IT-SI"), TOLERANCE);
        assertEquals(-2837, borderExchanges.get("IT-CH"), TOLERANCE);
        assertEquals(0, borderExchanges.get("FR-DE"), TOLERANCE);
        assertEquals(-2463, borderExchanges.get("IT-FR"), TOLERANCE);
        assertEquals(-37, borderExchanges.get("CH-FR"), TOLERANCE);
        assertEquals(0, borderExchanges.get("CH-DE"), TOLERANCE);
        assertEquals(-699, borderExchanges.get("IT-AT"), TOLERANCE);
    }

    @Test
    void testCountryBalances() {
        String networkFileName = "20210901_2230_test_network.uct";
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));

        Map<String, Double> borderExchanges = BorderExchanges.computeCseCountriesBalances(network);
        assertEquals(2500, borderExchanges.get("FR"), TOLERANCE);
        assertEquals(2800, borderExchanges.get("CH"), TOLERANCE);
        assertEquals(0, borderExchanges.get("DE"), TOLERANCE);
        assertEquals(-6000, borderExchanges.get("IT"), TOLERANCE);
        assertEquals(0, borderExchanges.get("SI"), TOLERANCE);
        assertEquals(700, borderExchanges.get("AT"), TOLERANCE);
    }
}
