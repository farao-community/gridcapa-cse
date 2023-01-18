/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.network_processing.ucte_pst_change;

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
class UctePstProcessorTest {
    private static final double DOUBLE_PRECISION = 0.1;
    private static final double DOUBLE_PRECISION_FOR_REGULATED_FLOW = 33;

    private Network network;

    @BeforeEach
    void setUp() {
        String filename = "network_with_mendrisio.uct";
        network = Network.read(filename, getClass().getResourceAsStream(filename));
    }

    @Test
    void testTransformerSettings() {
        UctePstProcessor uctePstProcessor = new UctePstProcessor("SMENDR3T SMENDR32 1", "SMENDR3T");
        uctePstProcessor.forcePhaseTapChangerInActivePowerRegulationForIdcc(network, -150.0);
        TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer("SMENDR3T SMENDR32 1");
        PhaseTapChanger phaseTapChanger = twoWindingsTransformer.getPhaseTapChanger();
        assertEquals(PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL, phaseTapChanger.getRegulationMode());
        assertEquals(-300, phaseTapChanger.getRegulationValue(), DOUBLE_PRECISION);
        assertTrue(phaseTapChanger.isRegulating());
        assertEquals(twoWindingsTransformer.getTerminal1(), phaseTapChanger.getRegulationTerminal());
    }

    @Test
    void testTransformerSettingsWithSpecifiedValue() {
        UctePstProcessor uctePstProcessor = new UctePstProcessor("SMENDR3T SMENDR32 1", "SMENDR3T");
        uctePstProcessor.forcePhaseTapChangerInActivePowerRegulationForD2cc(network, 150);
        TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer("SMENDR3T SMENDR32 1");
        PhaseTapChanger phaseTapChanger = twoWindingsTransformer.getPhaseTapChanger();
        assertEquals(PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL, phaseTapChanger.getRegulationMode());
        assertEquals(150, phaseTapChanger.getRegulationValue(), DOUBLE_PRECISION);
        assertTrue(phaseTapChanger.isRegulating());
        assertEquals(twoWindingsTransformer.getTerminal1(), phaseTapChanger.getRegulationTerminal());
    }

    @Test
    void testWithLoadFlow() {
        UctePstProcessor uctePstProcessor = new UctePstProcessor("SMENDR3T SMENDR32 1", "SMENDR3T");
        uctePstProcessor.forcePhaseTapChangerInActivePowerRegulationForIdcc(network, -233.0);
        TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer("SMENDR3T SMENDR32 1");
        PhaseTapChanger phaseTapChanger = twoWindingsTransformer.getPhaseTapChanger();
        assertEquals(0, phaseTapChanger.getTapPosition());
        LoadFlow.run(network, LoadFlowParameters.load());
        assertEquals(16, phaseTapChanger.getTapPosition());
        assertEquals(-300, twoWindingsTransformer.getTerminal1().getP(), DOUBLE_PRECISION_FOR_REGULATED_FLOW);
    }

    @Test
    void testFailWithMissingTransformer() {
        UctePstProcessor uctePstProcessor = new UctePstProcessor("Fake transformer", "SMENDR3T");
        assertThrows(UctePstException.class, () -> uctePstProcessor.forcePhaseTapChangerInActivePowerRegulationForD2cc(network, 150));
    }

    @Test
    void testMendrisioSetpointWorksWithDisconnectedTransformer() {
        String filename = "network_with_mendrisio_disconnected.uct";
        Network disconnectedTransformerNetwork = Network.read(filename, getClass().getResourceAsStream(filename));
        UctePstProcessor uctePstProcessor = new UctePstProcessor("SMENDR3T SMENDR32 1", "SMENDR3T");
        assertDoesNotThrow(() -> uctePstProcessor.forcePhaseTapChangerInActivePowerRegulationForIdcc(disconnectedTransformerNetwork, 133.0));
    }

    @Test
    void testWithLoadFlowUsesDefaultRegulationValue() {
        TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer("SMENDR3T SMENDR32 1");
        PhaseTapChanger phaseTapChanger = twoWindingsTransformer.getPhaseTapChanger();
        phaseTapChanger.setRegulationValue(Double.NaN);
        UctePstProcessor uctePstProcessor = new UctePstProcessor("SMENDR3T SMENDR32 1", "SMENDR3T");
        uctePstProcessor.forcePhaseTapChangerInActivePowerRegulationForIdcc(network, 100.0);
        assertEquals(0, phaseTapChanger.getTapPosition());
        LoadFlow.run(network, LoadFlowParameters.load());
        assertEquals(16, phaseTapChanger.getTapPosition());
        assertEquals(-333.0, twoWindingsTransformer.getTerminal1().getP(), DOUBLE_PRECISION_FOR_REGULATED_FLOW);
    }
}
