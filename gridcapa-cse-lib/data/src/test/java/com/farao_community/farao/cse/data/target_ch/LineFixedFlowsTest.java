/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.target_ch;

import com.farao_community.farao.data.crac_creation_util.UcteNetworkHelper;
import com.farao_community.farao.data.crac_creation_util.UcteNetworkHelperProperties;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LineFixedFlowsTest {

    @Test
    public void testFixedFlowWhenNoLinesAreInOutage() throws JAXBException {
        String filename = "TestCase12NodesNoOutages.uct";
        Network network = Importers.loadNetwork(filename, getClass().getResourceAsStream(filename));

        LineFixedFlows lineFixedFlows = LineFixedFlows.create(
                OffsetDateTime.parse("2021-09-13T12:30Z"),
                getClass().getResourceAsStream("simple_target_ch.xml")
        );

        UcteNetworkHelper ucteNetworkHelper = new UcteNetworkHelper(network, new UcteNetworkHelperProperties(UcteNetworkHelperProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
        Optional<Double> fixedFlow = lineFixedFlows.getFixedFlow("BBE2AA1  BBE3AA1  1", network, ucteNetworkHelper);
        assertTrue(fixedFlow.isEmpty());
    }

    @Test
    public void testFixedFlowWhenOneLineIsInOutage() throws JAXBException {
        String filename = "TestCase12NodesOneOutage.uct";
        Network network = Importers.loadNetwork(filename, getClass().getResourceAsStream(filename));

        LineFixedFlows lineFixedFlows = LineFixedFlows.create(
                OffsetDateTime.parse("2021-09-13T12:30Z"),
                getClass().getResourceAsStream("simple_target_ch.xml")
        );

        UcteNetworkHelper ucteNetworkHelper = new UcteNetworkHelper(network, new UcteNetworkHelperProperties(UcteNetworkHelperProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
        Optional<Double> fixedFlow = lineFixedFlows.getFixedFlow("BBE2AA1  BBE3AA1  1", network, ucteNetworkHelper);
        assertTrue(fixedFlow.isPresent());
        assertEquals(100, fixedFlow.get());
    }

    @Test
    public void testFixedFlowWhenTwoLinesAreInOutage() throws JAXBException {
        String filename = "TestCase12NodesTwoOutages.uct";
        Network network = Importers.loadNetwork(filename, getClass().getResourceAsStream(filename));

        LineFixedFlows lineFixedFlows = LineFixedFlows.create(
                OffsetDateTime.parse("2021-09-13T12:30Z"),
                getClass().getResourceAsStream("simple_target_ch.xml")
        );

        UcteNetworkHelper ucteNetworkHelper = new UcteNetworkHelper(network, new UcteNetworkHelperProperties(UcteNetworkHelperProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
        Optional<Double> fixedFlow = lineFixedFlows.getFixedFlow("BBE2AA1  BBE3AA1  1", network, ucteNetworkHelper);
        assertTrue(fixedFlow.isPresent());
        assertEquals(50, fixedFlow.get());
    }
}
