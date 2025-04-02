/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.services;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ForcedPraHandlerTest {

    // Thanks to this block we can visualize logging of forcing PRAs during the tests.
    @TestConfiguration
    public static class ForcedPrasHandlerTestConfiguration {

        @Bean
        @Primary
        public Logger getLoggerTest() {
            return LoggerFactory.getLogger(ForcedPraHandlerTest.class);
        }
    }

    @Autowired
    ForcedPrasHandler forcedPrasHandler;

    @Test
    void checkCracAndForcedPrasAreConsistent() throws IOException {
        Network network = Network.read("network-for-forced-pras.xiidm", getClass().getResourceAsStream("network-for-forced-pras.xiidm"));
        Crac crac = Crac.readWithContext("crac-for-forced-pras.json", Objects.requireNonNull(getClass().getResourceAsStream("crac-for-forced-pras.json")), network).getCrac();
        Set<String> manualForcedPrasIds = Set.of("Open line NL1-NL2", "Open line BE2-FR3", "PRA_PST_BE");

        assertNull(crac.getNetworkAction("PRA_PST_BE"));
        assertTrue(crac.getNetworkAction("Open line NL1-NL2").hasImpactOnNetwork(network));
        assertTrue(crac.getNetworkAction("Open line BE2-FR3").hasImpactOnNetwork(network));

        Set<String> appliedForcedPras = forcedPrasHandler.forcePras(manualForcedPrasIds, network, crac, Mockito.mock(FlowResult.class), RaoParameters.load());

        assertFalse(crac.getNetworkAction("Open line NL1-NL2").hasImpactOnNetwork(network));
        assertFalse(crac.getNetworkAction("Open line BE2-FR3").hasImpactOnNetwork(network));
        assertEquals(2, appliedForcedPras.size());
    }
}
