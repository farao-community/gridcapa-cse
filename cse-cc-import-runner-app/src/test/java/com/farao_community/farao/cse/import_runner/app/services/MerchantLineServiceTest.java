/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.data.ntc.Ntc;
import com.farao_community.farao.cse.data.target_ch.LineFixedFlows;
import com.farao_community.farao.cse.import_runner.app.configurations.MendrisioConfiguration;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@SpringBootTest
class MerchantLineServiceTest {
    private static final double DOUBLE_PRECISION = 0.1;
    private static final double DOUBLE_PRECISION_FOR_REGULATED_FLOW = 20;

    private Network network;
    private Network networkWithMendrisioCagnoLine;

    @Mock
    CseData cseData;

    @Autowired
    private MendrisioConfiguration mendrisioConfiguration;

    @Autowired
    private MerchantLineService merchantLineService;

    @BeforeEach
    void setUp() {
        String filename = "network_with_mendrisio.uct";
        String filenameNetworkWithMendrisioCagnoLine = "network_with_mendrisio_cagno_line.uct";
        network = Importers.loadNetwork(filename, getClass().getResourceAsStream(filename));
        networkWithMendrisioCagnoLine = Importers.loadNetwork(filenameNetworkWithMendrisioCagnoLine, getClass().getResourceAsStream(filenameNetworkWithMendrisioCagnoLine));
        Map<String, Double> fixedFlowLines = Map.of(
                mendrisioConfiguration.getMendrisioCagnoNtcId(), 75.
        );
        Ntc ntc = mock(Ntc.class);
        LineFixedFlows lineFixedFlows = mock(LineFixedFlows.class);
        when(cseData.getNtc()).thenReturn(ntc);
        when(cseData.getNtc().getFlowOnFixedFlowLines()).thenReturn(fixedFlowLines);
        when(cseData.getLineFixedFlows()).thenReturn(lineFixedFlows);
    }

    @Test
    void testTransformerSettings() {
        merchantLineService.activateMerchantLine(ProcessType.IDCC, network, cseData);
        TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer(mendrisioConfiguration.getMendrisioPstId());
        PhaseTapChanger phaseTapChanger = twoWindingsTransformer.getPhaseTapChanger();
        assertEquals(PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL, phaseTapChanger.getRegulationMode());
        assertEquals(-300, phaseTapChanger.getRegulationValue(), DOUBLE_PRECISION);
        assertTrue(phaseTapChanger.isRegulating());
        assertEquals(twoWindingsTransformer.getTerminal1(), phaseTapChanger.getRegulationTerminal());
    }

    @Test
    void testWithLoadFlow() {
        merchantLineService.activateMerchantLine(ProcessType.IDCC, network, cseData);
        TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer(mendrisioConfiguration.getMendrisioPstId());
        PhaseTapChanger phaseTapChanger = twoWindingsTransformer.getPhaseTapChanger();
        assertEquals(0, phaseTapChanger.getTapPosition());
        LoadFlow.run(network, LoadFlowParameters.load());
        assertEquals(12, phaseTapChanger.getTapPosition());
        assertEquals(-300, twoWindingsTransformer.getTerminal1().getP(), DOUBLE_PRECISION_FOR_REGULATED_FLOW);
    }

    @Test
    void testTransformerSettingsForD2ccProcess() {
        TwoWindingsTransformer twoWindingsTransformer = networkWithMendrisioCagnoLine.getTwoWindingsTransformer(mendrisioConfiguration.getMendrisioPstId());
        PhaseTapChanger phaseTapChanger = twoWindingsTransformer.getPhaseTapChanger();
        when(cseData.getLineFixedFlows().getFixedFlow(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Optional.empty());
        merchantLineService.activateMerchantLine(ProcessType.D2CC, networkWithMendrisioCagnoLine, cseData);
        LoadFlow.run(networkWithMendrisioCagnoLine, LoadFlowParameters.load());
        assertEquals(175, phaseTapChanger.getRegulationValue(), DOUBLE_PRECISION);
        Line mendrisioCagnoLine = networkWithMendrisioCagnoLine.getLine("SMENDR11 XME_CA11 1 + XME_CA11 NNL1AA1  1");
        assertEquals(75, mendrisioCagnoLine.getTerminal1().getP(), DOUBLE_PRECISION_FOR_REGULATED_FLOW);
    }

    @Test
    void testMendrisioSetpointWorksWithDisconnectedTransformer() {
        String filename = "network_with_mendrisio_disconnected.uct";
        Network disconnectedTransformerNetwork = Importers.loadNetwork(filename, getClass().getResourceAsStream(filename));
        assertDoesNotThrow(() -> merchantLineService.activateMerchantLine(ProcessType.IDCC, disconnectedTransformerNetwork, cseData));
    }

    @Test
    void testMendrisioIdccWorksWithDefaultValue() {
        TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer(mendrisioConfiguration.getMendrisioPstId());
        PhaseTapChanger phaseTapChanger = twoWindingsTransformer.getPhaseTapChanger();
        phaseTapChanger.setRegulationValue(Double.NaN);
        assertDoesNotThrow(() -> merchantLineService.activateMerchantLine(ProcessType.IDCC, network, cseData));
        assertEquals(75.0, phaseTapChanger.getRegulationValue(), 0.1);
    }
}
