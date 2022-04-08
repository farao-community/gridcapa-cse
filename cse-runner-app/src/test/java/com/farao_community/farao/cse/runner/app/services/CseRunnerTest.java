/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class CseRunnerTest {

    @Autowired
    private CseRunner cseRunner;

    @Test
    void testCracImportAndBusbarPreprocess() throws IOException {
        String cracUrl = Objects.requireNonNull(getClass().getResource("20210901_2230_213_CRAC_CO_CSE1_busbar.xml")).toString();
        Network network = Importers.loadNetwork("20210901_2230_test_network.uct", getClass().getResourceAsStream("20210901_2230_test_network.uct"));
        // Initially only one bus in this voltage level ITALY111
        assertEquals(1, Stream.of(network.getVoltageLevel("ITALY11").getBusBreakerView().getBuses()).count());

        Crac crac = cseRunner.preProcessNetworkForBusBarsAndImportCrac(cracUrl, network, OffsetDateTime.parse("2021-09-01T20:30Z"));

        // After pre-processing 4 buses in this voltage level ITALY111, ITALY112, ITALY11Z and ITALY11Y
        assertEquals(4, StreamSupport.stream(network.getVoltageLevel("ITALY11").getBusBreakerView().getBuses().spliterator(), true).count());
        assertEquals(1, crac.getRemedialActions().size());
    }
}
