/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.computation.BorderExchanges;
import com.farao_community.farao.cse.data.ttc_res.TtcResult;
import com.farao_community.farao.cse.data.xnode.XNodeReader;
import com.farao_community.farao.cse.data.xsd.ttc_res.Timestamp;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.import_runner.app.configurations.XNodesConfiguration;
import com.farao_community.farao.cse.import_runner.app.dichotomy.DichotomyRaoResponse;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.results.LimitingCause;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class TtcResultServiceTest {

    @Autowired
    TtcResultService ttcResultService;

    @MockBean
    private FileExporter fileExporter;
    @MockBean
    private FileImporter fileImporter;
    @MockBean
    private XNodesConfiguration xNodesConfiguration;

    @Test
    void saveFailedTtcResult() {
        // GIVEN
        OffsetDateTime now = OffsetDateTime.now();
        String firstShiftNetworkName = "networkName";
        TtcResult.FailedProcessData.FailedProcessReason failedProcessReason = TtcResult.FailedProcessData.FailedProcessReason.LOAD_FLOW_FAILURE;
        CseRequest cseRequest = mock(CseRequest.class);
        when(cseRequest.getCgmUrl()).thenReturn("file://cgmUrl");
        when(cseRequest.getMergedCracUrl()).thenReturn("file://mergedCracUrl");
        when(cseRequest.getMergedGlskUrl()).thenReturn("file://mergedGlskUrl");
        when(cseRequest.getNtcReductionsUrl()).thenReturn("file://ntcReductionsUrl");
        when(cseRequest.getTargetProcessDateTime()).thenReturn(now);
        when(cseRequest.getProcessType()).thenReturn(ProcessType.IDCC);
        when(cseRequest.isImportEcProcess()).thenReturn(false);

        when(fileExporter.saveTtcResult(any(), eq(now), eq(ProcessType.IDCC), eq(false))).thenReturn("ttcResultUrl");

        // WHEN
        String result = ttcResultService.saveFailedTtcResult(cseRequest, firstShiftNetworkName, failedProcessReason);

        // THEN
        assertNotNull(result);
        assertEquals("ttcResultUrl", result);
        verify(fileExporter, times(1)).saveTtcResult(any(), eq(now), eq(ProcessType.IDCC), eq(false));
    }

    @Test
    void saveTtcResult() {
        try (MockedStatic<BorderExchanges> borderExchangesMock = mockStatic(BorderExchanges.class);
            MockedStatic<XNodeReader> xNodeReaderMock = mockStatic(XNodeReader.class);
            MockedStatic<TtcResult> ttcResultMock = mockStatic(TtcResult.class)) {
            // GIVEN
            OffsetDateTime now = OffsetDateTime.now();
            String firstShiftNetworkName = "firstNetworkName";
            String finalNetworkName = "finalNetworkName";

            CseRequest cseRequest = mock(CseRequest.class);
            when(cseRequest.getCgmUrl()).thenReturn("file://cgmUrl");
            when(cseRequest.getMergedCracUrl()).thenReturn("file://mergedCracUrl");
            when(cseRequest.getMergedGlskUrl()).thenReturn("file://mergedGlskUrl");
            when(cseRequest.getNtcReductionsUrl()).thenReturn("file://ntcReductionsUrl");
            when(cseRequest.getTargetProcessDateTime()).thenReturn(now);
            when(cseRequest.getProcessType()).thenReturn(ProcessType.IDCC);
            when(cseRequest.isImportEcProcess()).thenReturn(false);

            CseData cseData = mock(CseData.class);
            when(cseData.getReducedSplittingFactors()).thenReturn(Map.of());
            when(cseData.getMniiOffset()).thenReturn(42.0);

            CseCracCreationContext cseCracCreationContext = mock(CseCracCreationContext.class);
            Crac crac = mock(Crac.class);
            when(cseCracCreationContext.getCrac()).thenReturn(crac);

            RaoResponse raoResponse = mock(RaoResponse.class);
            when(raoResponse.getNetworkWithPraFileUrl()).thenReturn("file://networkWithPraFileUrl");
            when(raoResponse.getRaoResultFileUrl()).thenReturn("file://raoResultFileUrl");

            DichotomyRaoResponse highestSecureStepRaoResponse = mock(DichotomyRaoResponse.class);
            when(highestSecureStepRaoResponse.getRaoResponse()).thenReturn(raoResponse);
            when(highestSecureStepRaoResponse.getForcedPrasIds()).thenReturn(Set.of());

            LimitingCause limitingCause = mock(LimitingCause.class);
            Map<String, Integer> preprocessedPsts = Map.of();
            Map<String, Double> preprocessedPisaLinks = Map.of();

            Network network = mock(Network.class);
            when(fileImporter.importNetwork("file://networkWithPraFileUrl")).thenReturn(network);
            borderExchangesMock.when(() -> BorderExchanges.computeItalianImport(network)).thenReturn(3.14);
            borderExchangesMock.when(() -> BorderExchanges.computeCseBordersExchanges(network)).thenReturn(Map.of());
            borderExchangesMock.when(() -> BorderExchanges.computeCseCountriesBalances(network)).thenReturn(Map.of());
            xNodeReaderMock.when(() -> XNodeReader.getXNodes(any())).thenReturn(List.of());
            Timestamp timestamp = new Timestamp();
            ttcResultMock.when(() -> TtcResult.generate(any(), any(), any(), any(), any())).thenReturn(timestamp);
            RaoResult raoResult = mock(RaoResult.class);
            when(fileImporter.importRaoResult("file://raoResultFileUrl", crac)).thenReturn(raoResult);
            when(fileExporter.saveTtcResult(timestamp, now, ProcessType.IDCC, false)).thenReturn("ttcResultUrl");

            // WHEN
            String result = ttcResultService.saveTtcResult(cseRequest, cseData, cseCracCreationContext, highestSecureStepRaoResponse,
                limitingCause, firstShiftNetworkName, finalNetworkName, preprocessedPsts, preprocessedPisaLinks);

            // THEN
            assertNotNull(result);
            assertEquals("ttcResultUrl", result);
            verify(fileExporter, times(1)).saveTtcResult(any(), eq(now), eq(ProcessType.IDCC), eq(false));
        }
    }
}
