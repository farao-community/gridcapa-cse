/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
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
            Collections.emptyList(),
            automatedForcedPras,
            dichotomyNumber,
            50,
            650,
            1000.);
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

    private DichotomyResult<RaoResponse> mockDichotomyResult(String limitingElement, double lowestUnsecureItalianImport) throws IOException {
        DichotomyStepResult<RaoResponse> highestValidStep = Mockito.mock(DichotomyStepResult.class);
        DichotomyResult<RaoResponse> dichotomyResult = Mockito.mock(DichotomyResult.class);
        Mockito.when(dichotomyResult.getHighestValidStep()).thenReturn(highestValidStep);
        Mockito.when(dichotomyResultHelper.getLimitingElement(dichotomyResult)).thenReturn(limitingElement);
        Mockito.when(dichotomyResultHelper.computeLowestUnsecureItalianImport(dichotomyResult)).thenReturn(lowestUnsecureItalianImport);
        return dichotomyResult;
    }

    @Test
    void testMultipleDichotomyWithNoAutomatedForcedPras() throws IOException {
        CseRequest cseRequest = getCseRequest(
            Collections.emptyMap(),
            5
        );

        DichotomyResult<RaoResponse> initialDichotomyResult = mockDichotomyResult("element1", 1000);
        DichotomyResult<RaoResponse> secondDichotomyResult = mockDichotomyResult("element2", 2000);

        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), any())).thenReturn(initialDichotomyResult);
        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), anyDouble(), any())).thenReturn(secondDichotomyResult);

        MultipleDichotomyResult dichotomyResult = multipleDichotomyRunner.runMultipleDichotomy(cseRequest, cseData, network, crac, 1000.);

        assertEquals(initialDichotomyResult, dichotomyResult.getBestDichotomyResult());
        assertTrue(dichotomyResult.getBestForcedPrasIds().isEmpty());
    }

    @Test
    void testMultipleDichotomyLimitedByDichotomyNumber() throws IOException {
        CseRequest cseRequest = getCseRequest(
            Map.of("element1", List.of(Set.of("ra1")), "default", List.of(Set.of("ra1"))),
            1
        );

        DichotomyResult<RaoResponse> initialDichotomyResult = mockDichotomyResult("element1", 1000);
        DichotomyResult<RaoResponse> secondDichotomyResult = mockDichotomyResult("element2", 2000);

        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), any())).thenReturn(initialDichotomyResult);
        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), anyDouble(), any())).thenReturn(secondDichotomyResult);

        MultipleDichotomyResult dichotomyResult = multipleDichotomyRunner.runMultipleDichotomy(cseRequest, cseData, network, crac, 1000.);

        assertEquals(initialDichotomyResult, dichotomyResult.getBestDichotomyResult());
        assertTrue(dichotomyResult.getBestForcedPrasIds().isEmpty());
    }

    @Test
    void testMultipleDichotomyLimitedByDichotomyNumberAtSecondIteration() throws IOException {
        CseRequest cseRequest = getCseRequest(
            Map.of("element1", List.of(Set.of("ra1")), "default", List.of(Set.of("ra1"))),
            2
        );

        DichotomyResult<RaoResponse> initialDichotomyResult = mockDichotomyResult("element1", 1000);
        DichotomyResult<RaoResponse> secondDichotomyResult = mockDichotomyResult("element2", 2000);

        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), any())).thenReturn(initialDichotomyResult);
        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), anyDouble(), any())).thenReturn(secondDichotomyResult);

        MultipleDichotomyResult dichotomyResult = multipleDichotomyRunner.runMultipleDichotomy(cseRequest, cseData, network, crac, 1000.);

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

        DichotomyResult<RaoResponse> initialDichotomyResult = mockDichotomyResult("element1", 1000);
        DichotomyResult<RaoResponse> secondDichotomyResult = mockDichotomyResult("element2", 2000);

        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), any())).thenReturn(initialDichotomyResult);
        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), anyDouble(), any())).thenReturn(secondDichotomyResult);

        MultipleDichotomyResult dichotomyResult = multipleDichotomyRunner.runMultipleDichotomy(cseRequest, cseData, network, crac, 1000.);

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

        DichotomyResult<RaoResponse> initialDichotomyResult = mockDichotomyResult("element1", 1000);
        DichotomyResult<RaoResponse> secondDichotomyResult = mockDichotomyResult("element2", 500);

        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), any())).thenReturn(initialDichotomyResult);
        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), anyDouble(), any())).thenReturn(secondDichotomyResult);

        MultipleDichotomyResult dichotomyResult = multipleDichotomyRunner.runMultipleDichotomy(cseRequest, cseData, network, crac, 1000.);

        assertEquals(initialDichotomyResult, dichotomyResult.getBestDichotomyResult());
        assertTrue(dichotomyResult.getBestForcedPrasIds().isEmpty());
    }

    @Test
    void testMultipleDichotomyWithSameLimitingElementAndIncreasingTTC() throws IOException {
        CseRequest cseRequest = getCseRequest(
            Map.of("element1", List.of(Set.of("ra1"), Set.of("ra4")), "default", List.of(Set.of("ra3"))),
            3
        );

        DichotomyResult<RaoResponse> initialDichotomyResult = mockDichotomyResult("element1", 1000);
        DichotomyResult<RaoResponse> secondDichotomyResult = mockDichotomyResult("element1", 1500);
        DichotomyResult<RaoResponse> thirdDichotomyResult = mockDichotomyResult("element1", 2000);

        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), any())).thenReturn(initialDichotomyResult);
        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), anyDouble(), any())).thenReturn(secondDichotomyResult, thirdDichotomyResult);

        MultipleDichotomyResult dichotomyResult = multipleDichotomyRunner.runMultipleDichotomy(cseRequest, cseData, network, crac, 1000.);

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

        DichotomyResult<RaoResponse> initialDichotomyResult = mockDichotomyResult("element1", 1000);
        DichotomyResult<RaoResponse> secondDichotomyResult = mockDichotomyResult("element1", 500);
        DichotomyResult<RaoResponse> thirdDichotomyResult = mockDichotomyResult("element1", 2000);

        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), any())).thenReturn(initialDichotomyResult);
        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), anyDouble(), any())).thenReturn(secondDichotomyResult, thirdDichotomyResult);

        MultipleDichotomyResult dichotomyResult = multipleDichotomyRunner.runMultipleDichotomy(cseRequest, cseData, network, crac, 1000.);

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

        DichotomyResult<RaoResponse> initialDichotomyResult = mockDichotomyResult("element1", 1000);
        DichotomyResult<RaoResponse> secondDichotomyResult = mockDichotomyResult("element2", 500);
        DichotomyResult<RaoResponse> thirdDichotomyResult = mockDichotomyResult("element2", 800);

        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), any())).thenReturn(initialDichotomyResult);
        Mockito.when(dichotomyRunner.runDichotomy(eq(cseRequest), eq(cseData), eq(network), anyDouble(), anyDouble(), any()))
            .thenReturn(secondDichotomyResult, thirdDichotomyResult);

        MultipleDichotomyResult dichotomyResult = multipleDichotomyRunner.runMultipleDichotomy(cseRequest, cseData, network, crac, 1000.);

        assertEquals(initialDichotomyResult, dichotomyResult.getBestDichotomyResult());
        assertTrue(dichotomyResult.getBestForcedPrasIds().isEmpty());
    }
}
