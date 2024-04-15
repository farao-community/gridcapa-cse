/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.dichotomy.api.results.LimitingCause;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class MultipleDichotomyRunnerTest {

    // Thanks to this block we can visualize logging of forcing PRAs during the tests.
    @TestConfiguration
    public static class MultipleDichotomyRunnerTestConfiguration {

        @Bean
        @Primary
        public Logger getLoggerTest() {
            return LoggerFactory.getLogger(MultipleDichotomyRunnerTest.class);
        }
    }

    @Autowired
    private MultipleDichotomyRunner multipleDichotomyRunner;

    @MockBean
    private DichotomyResultHelper dichotomyResultHelper;

    @MockBean
    private DichotomyRunner dichotomyRunner;

    private Crac crac;
    private Network network;
    private CseData cseData;

    private Map<String, Double> referenceExchanges = Map.ofEntries(Map.entry("FR", 250.),
        Map.entry("SI", 250.),
        Map.entry("CH", 250.),
        Map.entry("AT", 250.));

    private Map<String, Double> ntcs = Map.ofEntries(Map.entry("FR", 100.),
        Map.entry("SI", 100.),
        Map.entry("CH", 100.),
        Map.entry("AT", 100.));

    @BeforeEach
    void setUp() {
        network = Mockito.mock(Network.class);
        crac = Mockito.mock(Crac.class);
        mockCrac();
        cseData = Mockito.mock(CseData.class);
    }

    private CseRequest getCseRequest(Map<String, List<Set<String>>> automatedForcedPras, int dichotomyNumber) {
        return CseRequest.d2ccProcess(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Collections.emptyList(),
            automatedForcedPras,
            dichotomyNumber,
            50,
            650,
            1000.,
            false);
    }

    private void mockCrac() {
        crac = Mockito.mock(Crac.class);

        // RA1 is available and has impact on the network
        NetworkAction ra1 = Mockito.mock(NetworkAction.class);
        Mockito.when(ra1.getUsageMethod(any())).thenReturn(UsageMethod.AVAILABLE);
        Mockito.when(ra1.hasImpactOnNetwork(network)).thenReturn(true);
        Mockito.when(crac.getNetworkAction("ra1")).thenReturn(ra1);

        // RA2 is available but has no impact on the network
        NetworkAction ra2 = Mockito.mock(NetworkAction.class);
        Mockito.when(ra2.getUsageMethod(any())).thenReturn(UsageMethod.AVAILABLE);
        Mockito.when(ra2.hasImpactOnNetwork(network)).thenReturn(false);
        Mockito.when(crac.getNetworkAction("ra2")).thenReturn(ra2);

        // RA3 is not available and has no impact on the network
        NetworkAction ra3 = Mockito.mock(NetworkAction.class);
        Mockito.when(ra3.getUsageMethod(any())).thenReturn(UsageMethod.UNAVAILABLE);
        Mockito.when(ra3.hasImpactOnNetwork(network)).thenReturn(false);
        Mockito.when(crac.getNetworkAction("ra3")).thenReturn(ra3);

        // RA4 is available and has impact on the network
        NetworkAction ra4 = Mockito.mock(NetworkAction.class);
        Mockito.when(ra4.getUsageMethod(any())).thenReturn(UsageMethod.AVAILABLE);
        Mockito.when(ra4.hasImpactOnNetwork(network)).thenReturn(true);
        Mockito.when(crac.getNetworkAction("ra4")).thenReturn(ra4);

        // RA5 is available and has impact on the network
        NetworkAction ra5 = Mockito.mock(NetworkAction.class);
        Mockito.when(ra5.getUsageMethod(any())).thenReturn(UsageMethod.AVAILABLE);
        Mockito.when(ra5.hasImpactOnNetwork(network)).thenReturn(true);
        Mockito.when(crac.getNetworkAction("ra5")).thenReturn(ra5);
    }

    private DichotomyResult<DichotomyRaoResponse> mockDichotomyResult(String limitingElement, double lowestUnsecureItalianImport) throws IOException {
        DichotomyStepResult<DichotomyRaoResponse> highestValidStep = Mockito.mock(DichotomyStepResult.class);
        DichotomyResult<DichotomyRaoResponse> dichotomyResult = Mockito.mock(DichotomyResult.class);

        Mockito.when(dichotomyResult.getHighestValidStep()).thenReturn(highestValidStep);
        Mockito.when(dichotomyResult.hasValidStep()).thenReturn(true);
        Mockito.when(dichotomyResultHelper.getLimitingElement(dichotomyResult, network)).thenReturn(limitingElement);
        Mockito.when(dichotomyResultHelper.computeLowestUnsecureItalianImport(dichotomyResult)).thenReturn(lowestUnsecureItalianImport);
        return dichotomyResult;
    }

    @Test
    void testMultipleDichotomyWithNoAutomatedForcedPras() throws IOException {
        CseRequest cseRequest = getCseRequest(
            Collections.emptyMap(),
            5
        );

        DichotomyResult<DichotomyRaoResponse> initialDichotomyResult = mockDichotomyResult("element1", 1000);
        DichotomyResult<DichotomyRaoResponse> secondDichotomyResult = mockDichotomyResult("element2", 2000);

        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), any(), any(), any())).thenReturn(initialDichotomyResult);
        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), anyDouble(), any(), any(), any())).thenReturn(secondDichotomyResult);
        Mockito.when(initialDichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);

        MultipleDichotomyResult<DichotomyRaoResponse> dichotomyResult =
            multipleDichotomyRunner.runMultipleDichotomy(cseRequest, cseData, network, crac, 1000., referenceExchanges, ntcs);

        assertEquals(initialDichotomyResult, dichotomyResult.getBestDichotomyResult());
        assertTrue(dichotomyResult.getBestForcedPrasIds().isEmpty());
    }

    @Test
    void testMultipleDichotomyLimitedByDichotomyNumber() throws IOException {
        CseRequest cseRequest = getCseRequest(
            Map.of("element1", List.of(Set.of("ra1")), "default", List.of(Set.of("ra1"))),
            1
        );

        DichotomyResult<DichotomyRaoResponse> initialDichotomyResult = mockDichotomyResult("element1", 1000);
        DichotomyResult<DichotomyRaoResponse> secondDichotomyResult = mockDichotomyResult("element2", 2000);

        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), any(), any(), any())).thenReturn(initialDichotomyResult);
        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), anyDouble(), any(), any(), any())).thenReturn(secondDichotomyResult);
        Mockito.when(initialDichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);

        MultipleDichotomyResult<DichotomyRaoResponse> dichotomyResult =
            multipleDichotomyRunner.runMultipleDichotomy(cseRequest, cseData, network, crac, 1000., referenceExchanges, ntcs);

        assertEquals(initialDichotomyResult, dichotomyResult.getBestDichotomyResult());
        assertTrue(dichotomyResult.getBestForcedPrasIds().isEmpty());
    }

    @Test
    void testMultipleDichotomyLimitedByDichotomyNumberAtSecondIteration() throws IOException {
        CseRequest cseRequest = getCseRequest(
            Map.of("element1", List.of(Set.of("ra1")), "default", List.of(Set.of("ra1"))),
            2
        );

        DichotomyResult<DichotomyRaoResponse> initialDichotomyResult = mockDichotomyResult("element1", 1000);
        DichotomyResult<DichotomyRaoResponse> secondDichotomyResult = mockDichotomyResult("element2", 2000);

        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), any(), any(), any())).thenReturn(initialDichotomyResult);
        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), anyDouble(), any(), any(), any())).thenReturn(secondDichotomyResult);
        Mockito.when(initialDichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);
        Mockito.when(secondDichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);
        MultipleDichotomyResult<DichotomyRaoResponse> dichotomyResult =
            multipleDichotomyRunner.runMultipleDichotomy(cseRequest, cseData, network, crac, 1000., referenceExchanges, ntcs);

        assertEquals(secondDichotomyResult, dichotomyResult.getBestDichotomyResult());
        assertEquals(1, dichotomyResult.getBestForcedPrasIds().size());
        assertTrue(dichotomyResult.getBestForcedPrasIds().contains("ra1"));
    }

    @Test
    void testMultipleDichotomyLimitedByUnavailableAdditionalPra() throws IOException {
        CseRequest cseRequest = getCseRequest(
            Map.of("element1", List.of(Set.of("ra1")), "default", List.of(Set.of("ra3"))),
            5
        );

        DichotomyResult<DichotomyRaoResponse> initialDichotomyResult = mockDichotomyResult("element1", 1000);
        DichotomyResult<DichotomyRaoResponse> secondDichotomyResult = mockDichotomyResult("element2", 2000);

        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), any(), any(), any())).thenReturn(initialDichotomyResult);
        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), anyDouble(), any(), any(), any())).thenReturn(secondDichotomyResult);
        Mockito.when(initialDichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);
        Mockito.when(secondDichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);

        MultipleDichotomyResult<DichotomyRaoResponse> dichotomyResult =
            multipleDichotomyRunner.runMultipleDichotomy(cseRequest, cseData, network, crac, 1000., referenceExchanges, ntcs);

        assertEquals(secondDichotomyResult, dichotomyResult.getBestDichotomyResult());
        assertEquals(1, dichotomyResult.getBestForcedPrasIds().size());
        assertTrue(dichotomyResult.getBestForcedPrasIds().contains("ra1"));
    }

    @Test
    void testMultipleDichotomyLimitedByUnavailableAdditionalPraAndBetterInitialResult() throws IOException {
        CseRequest cseRequest = getCseRequest(
            Map.of("element1", List.of(Set.of("ra1")), "default", List.of(Set.of("ra3"))),
            5
        );

        DichotomyResult<DichotomyRaoResponse> initialDichotomyResult = mockDichotomyResult("element1", 1000);
        DichotomyResult<DichotomyRaoResponse> secondDichotomyResult = mockDichotomyResult("element2", 500);

        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), any(), any(), any())).thenReturn(initialDichotomyResult);
        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), anyDouble(), any(), any(), any())).thenReturn(secondDichotomyResult);
        Mockito.when(initialDichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);
        Mockito.when(secondDichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);

        MultipleDichotomyResult<DichotomyRaoResponse> dichotomyResult =
            multipleDichotomyRunner.runMultipleDichotomy(cseRequest, cseData, network, crac, 1000., referenceExchanges, ntcs);

        assertEquals(initialDichotomyResult, dichotomyResult.getBestDichotomyResult());
        assertTrue(dichotomyResult.getBestForcedPrasIds().isEmpty());
    }

    @Test
    void testMultipleDichotomyWithSameLimitingElementAndIncreasingTTC() throws IOException {
        CseRequest cseRequest = getCseRequest(
            Map.of("element1", List.of(Set.of("ra1"), Set.of("ra4")), "default", List.of(Set.of("ra3"))),
            3
        );

        DichotomyResult<DichotomyRaoResponse> initialDichotomyResult = mockDichotomyResult("element1", 1000);
        DichotomyResult<DichotomyRaoResponse> secondDichotomyResult = mockDichotomyResult("element1", 1500);
        DichotomyResult<DichotomyRaoResponse> thirdDichotomyResult = mockDichotomyResult("element1", 2000);
        Mockito.when(initialDichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);
        Mockito.when(secondDichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);
        Mockito.when(thirdDichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);

        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), any(), any(), any())).thenReturn(initialDichotomyResult);
        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), anyDouble(), any(), any(), any())).thenReturn(secondDichotomyResult, thirdDichotomyResult);

        MultipleDichotomyResult<DichotomyRaoResponse> dichotomyResult =
            multipleDichotomyRunner.runMultipleDichotomy(cseRequest, cseData, network, crac, 1000., referenceExchanges, ntcs);

        assertEquals(thirdDichotomyResult, dichotomyResult.getBestDichotomyResult());
        assertEquals(1, dichotomyResult.getBestForcedPrasIds().size());
        assertTrue(dichotomyResult.getBestForcedPrasIds().contains("ra4"));
    }

    @Test
    void testMultipleDichotomyWithSameLimitingElementWithSecondTryTTCBelow() throws IOException {
        CseRequest cseRequest = getCseRequest(
            Map.of("element1", List.of(Set.of("ra1"), Set.of("ra4")), "element2", List.of(Set.of("ra4"), Set.of("ra5")), "default", List.of(Set.of("ra3"))),
            3
        );

        DichotomyResult<DichotomyRaoResponse> initialDichotomyResult = mockDichotomyResult("element1", 1000);
        DichotomyResult<DichotomyRaoResponse> secondDichotomyResult = mockDichotomyResult("element1", 500);
        DichotomyResult<DichotomyRaoResponse> thirdDichotomyResult = mockDichotomyResult("element1", 2000);
        Mockito.when(initialDichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);
        Mockito.when(secondDichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);
        Mockito.when(thirdDichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);
        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), any(), any(), any())).thenReturn(initialDichotomyResult);
        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), anyDouble(), any(), any(), any())).thenReturn(secondDichotomyResult, thirdDichotomyResult);

        MultipleDichotomyResult<DichotomyRaoResponse> dichotomyResult =
            multipleDichotomyRunner.runMultipleDichotomy(cseRequest, cseData, network, crac, 1000., referenceExchanges, ntcs);

        assertEquals(thirdDichotomyResult, dichotomyResult.getBestDichotomyResult());
        assertEquals(1, dichotomyResult.getBestForcedPrasIds().size());
        assertTrue(dichotomyResult.getBestForcedPrasIds().contains("ra4"));
    }

    @Test
    void testMultipleDichotomyWithSameLimitingElementWithChangingLimitingElement() throws IOException {
        CseRequest cseRequest = getCseRequest(
            Map.of("element1", List.of(Set.of("ra1"), Set.of("ra4")), "element2", List.of(Set.of("ra4"), Set.of("ra5")), "default", List.of(Set.of("ra3"))),
            5
        );

        DichotomyResult<DichotomyRaoResponse> initialDichotomyResult = mockDichotomyResult("element1", 1000);
        DichotomyResult<DichotomyRaoResponse> secondDichotomyResult = mockDichotomyResult("element2", 500);
        DichotomyResult<DichotomyRaoResponse> thirdDichotomyResult = mockDichotomyResult("element2", 800);
        Mockito.when(initialDichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);
        Mockito.when(secondDichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);
        Mockito.when(thirdDichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);

        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), any(), any(), any())).thenReturn(initialDichotomyResult);
        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), anyDouble(), any(), any(), any()))
            .thenReturn(secondDichotomyResult, thirdDichotomyResult);

        MultipleDichotomyResult<DichotomyRaoResponse> dichotomyResult =
            multipleDichotomyRunner.runMultipleDichotomy(cseRequest, cseData, network, crac, 1000., referenceExchanges, ntcs);

        assertEquals(initialDichotomyResult, dichotomyResult.getBestDichotomyResult());
        assertTrue(dichotomyResult.getBestForcedPrasIds().isEmpty());
    }
}
