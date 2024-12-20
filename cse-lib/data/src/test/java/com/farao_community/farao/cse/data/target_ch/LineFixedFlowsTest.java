/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.target_ch;

import com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzer;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzerProperties;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import jakarta.xml.bind.JAXBException;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LineFixedFlowsTest {

    @Test
    void testFixedFlowWhenNoLinesAreInOutage() throws JAXBException {
        String filename = "TestCase12NodesNoOutages.uct";
        Network network = Network.read(filename, getClass().getResourceAsStream(filename));

        LineFixedFlows lineFixedFlows = LineFixedFlows.create(
                OffsetDateTime.parse("2021-09-13T12:30Z"),
                getClass().getResourceAsStream("simple_target_ch.xml"),
                false
        );

        LineFixedFlows lineFixedFlowsCreatedFromAdaptedTargetCH = LineFixedFlows.create(
                OffsetDateTime.parse("2021-09-13T12:30Z"),
                getClass().getResourceAsStream("simple_target_ch_adapted.xml"),
                true
        );

        UcteNetworkAnalyzer ucteNetworkHelper = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
        Optional<Double> fixedFlow = lineFixedFlows.getFixedFlow("BBE2AA1  BBE3AA1  1", network, ucteNetworkHelper);
        Optional<Double> fixedFlow1 = lineFixedFlowsCreatedFromAdaptedTargetCH.getFixedFlow("BBE2AA1  BBE3AA1  1", network, ucteNetworkHelper);
        assertTrue(fixedFlow.isEmpty());
        assertTrue(fixedFlow1.isEmpty());
    }

    @Test
    void testFixedFlowWhenOneLineIsInOutage() throws JAXBException {
        String filename = "TestCase12NodesOneOutage.uct";
        Network network = Network.read(filename, getClass().getResourceAsStream(filename));

        LineFixedFlows lineFixedFlows = LineFixedFlows.create(
                OffsetDateTime.parse("2021-09-13T12:30Z"),
                getClass().getResourceAsStream("simple_target_ch.xml"),
                false
        );

        LineFixedFlows lineFixedFlowsCreatedFromAdaptedTargetCH = LineFixedFlows.create(
                OffsetDateTime.parse("2021-09-13T12:30Z"),
                getClass().getResourceAsStream("simple_target_ch_adapted.xml"),
                true
        );

        UcteNetworkAnalyzer ucteNetworkHelper = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
        Optional<Double> fixedFlow = lineFixedFlows.getFixedFlow("BBE2AA1  BBE3AA1  1", network, ucteNetworkHelper);
        Optional<Double> fixedFlow1 = lineFixedFlowsCreatedFromAdaptedTargetCH.getFixedFlow("BBE2AA1  BBE3AA1  1", network, ucteNetworkHelper);
        assertTrue(fixedFlow.isPresent());
        assertTrue(fixedFlow1.isPresent());
        assertEquals(100, fixedFlow.get());
        assertEquals(100, fixedFlow1.get());
    }

    @Test
    void testFixedFlowWhenTwoLinesAreInOutage() throws JAXBException {
        String filename = "TestCase12NodesTwoOutages.uct";
        Network network = Network.read(filename, getClass().getResourceAsStream(filename));

        LineFixedFlows lineFixedFlows = LineFixedFlows.create(
                OffsetDateTime.parse("2021-09-13T12:30Z"),
                getClass().getResourceAsStream("simple_target_ch.xml"),
                false
        );

        LineFixedFlows lineFixedFlowsCreatedFromAdaptedTargetCH = LineFixedFlows.create(
                OffsetDateTime.parse("2021-09-13T12:30Z"),
                getClass().getResourceAsStream("simple_target_ch_adapted.xml"),
                true
        );

        UcteNetworkAnalyzer ucteNetworkHelper = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
        Optional<Double> fixedFlow = lineFixedFlows.getFixedFlow("BBE2AA1  BBE3AA1  1", network, ucteNetworkHelper);
        Optional<Double> fixedFlow1 = lineFixedFlowsCreatedFromAdaptedTargetCH.getFixedFlow("BBE2AA1  BBE3AA1  1", network, ucteNetworkHelper);
        assertTrue(fixedFlow.isPresent());
        assertTrue(fixedFlow1.isPresent());
        assertEquals(50, fixedFlow.get());
        assertEquals(50, fixedFlow1.get());
    }
}
