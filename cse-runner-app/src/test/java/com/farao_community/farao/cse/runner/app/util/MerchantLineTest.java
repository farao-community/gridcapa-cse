/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.util;

import com.farao_community.farao.cse.data.ntc.Ntc;
import com.farao_community.farao.cse.data.target_ch.LineFixedFlows;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.cse.runner.app.CseData;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@SpringBootTest
class MerchantLineTest {
    private static final double DOUBLE_PRECISION = 0.1;
    private static final double DOUBLE_PRECISION_FOR_REGULATED_FLOW = 20;

    private Network network;
    private Network networkWithMendrisioCagnoLine;

    @Mock
    CseData cseData;
    @Mock
    Ntc ntc;
    @Mock
    LineFixedFlows lineFixedFlows;

    @BeforeEach
    void setUp() {
        String filename = "network_with_mendrisio.uct";
        String filenameNetworkWithMendrisioCagnoLine = "network_with_mendrisio_cagno_line.uct";
        network = Importers.loadNetwork(filename, getClass().getResourceAsStream(filename));
        networkWithMendrisioCagnoLine = Importers.loadNetwork(filenameNetworkWithMendrisioCagnoLine, getClass().getResourceAsStream(filenameNetworkWithMendrisioCagnoLine));
    }

    @Test
    void testTransformerSettings() {
        MerchantLine.activateMerchantLine(ProcessType.IDCC, network, cseData);
        TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer(MerchantLine.MENDRISIO_ID);
        PhaseTapChanger phaseTapChanger = twoWindingsTransformer.getPhaseTapChanger();
        assertEquals(PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL, phaseTapChanger.getRegulationMode());
        assertEquals(-300, phaseTapChanger.getRegulationValue(), DOUBLE_PRECISION);
        assertTrue(phaseTapChanger.isRegulating());
        assertEquals(twoWindingsTransformer.getTerminal1(), phaseTapChanger.getRegulationTerminal());
    }

    @Test
    void testWithLoadFlow() {
        MerchantLine.activateMerchantLine(ProcessType.IDCC, network, cseData);
        TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer(MerchantLine.MENDRISIO_ID);
        PhaseTapChanger phaseTapChanger = twoWindingsTransformer.getPhaseTapChanger();
        assertEquals(0, phaseTapChanger.getTapPosition());
        LoadFlow.run(network, LoadFlowParameters.load());
        assertEquals(12, phaseTapChanger.getTapPosition());
        assertEquals(-300, twoWindingsTransformer.getTerminal1().getP(), DOUBLE_PRECISION_FOR_REGULATED_FLOW);
    }

    @Test
    void testTransformerSettingsForD2ccProcess() {
        Map<String, Double> fixedFlowLines = Map.of(
                MerchantLine.MENDRISIO_CAGNO_CODE_IN_NTC_FILE, 75.
        );
        TwoWindingsTransformer twoWindingsTransformer = networkWithMendrisioCagnoLine.getTwoWindingsTransformer(MerchantLine.MENDRISIO_ID);
        PhaseTapChanger phaseTapChanger = twoWindingsTransformer.getPhaseTapChanger();
        Mockito.when(cseData.getNtc()).thenReturn(ntc);
        Mockito.when(cseData.getNtc().getFlowOnFixedFlowLines()).thenReturn(fixedFlowLines);
        Mockito.when(cseData.getLineFixedFlows()).thenReturn(lineFixedFlows);
        when(cseData.getLineFixedFlows().getFixedFlow(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Optional.empty());
        MerchantLine.activateMerchantLine(ProcessType.D2CC, networkWithMendrisioCagnoLine, cseData);
        assertEquals(95.58, phaseTapChanger.getRegulationValue(), DOUBLE_PRECISION);
    }

}
