/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.util;

import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class MerchantLineTest {
    private static final double DOUBLE_PRECISION = 0.1;
    private static final double DOUBLE_PRECISION_FOR_REGULATED_FLOW = 20;

    private Network network;
    private TwoWindingsTransformer twoWindingsTransformer;
    private PhaseTapChanger phaseTapChanger;

    @BeforeEach
    void setUp() {
        String filename = "network_with_mendrisio.uct";
        network = Importers.loadNetwork(filename, getClass().getResourceAsStream(filename));
        twoWindingsTransformer = network.getTwoWindingsTransformer(MerchantLine.MENDRISIO_ID);
        phaseTapChanger = twoWindingsTransformer.getPhaseTapChanger();
    }

    @Test
    void testTransformerSettings() {
        MerchantLine.activateMerchantLine(ProcessType.IDCC, network);

        assertEquals(PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL, phaseTapChanger.getRegulationMode());
        assertEquals(-300, phaseTapChanger.getRegulationValue(), DOUBLE_PRECISION);
        assertTrue(phaseTapChanger.isRegulating());
        assertEquals(twoWindingsTransformer.getTerminal1(), phaseTapChanger.getRegulationTerminal());
    }

    @Test
    void testWithLoadFlow() {
        MerchantLine.activateMerchantLine(ProcessType.IDCC, network);

        assertEquals(0, phaseTapChanger.getTapPosition());
        LoadFlow.run(network, LoadFlowParameters.load());
        assertEquals(12, phaseTapChanger.getTapPosition());
        assertEquals(-300, twoWindingsTransformer.getTerminal1().getP(), DOUBLE_PRECISION_FOR_REGULATED_FLOW);
    }
}
