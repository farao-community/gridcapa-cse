/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.import_runner.app.services.FileExporter;
import com.farao_community.farao.cse.import_runner.app.services.InterruptionService;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class DichotomyRunnerTest {
    @Autowired
    DichotomyRunner dichotomyRunner;

    @MockBean
    FileExporter fileExporter;
    @MockBean
    NetworkShifterProvider networkShifterProvider;
    @MockBean
    InterruptionService interruptionService;
    @Mock
    CseRequest cseRequest;
    @Mock
    CseData cseData;
    @Mock
    Network network;
    @Mock
    VariantManager variantManager;
    @Mock
    NetworkShifter networkShifter;

    @Test
    void testRunDichotomy() throws IOException {
        double initialIndexValue = 4.2;
        Map<String, Double> referenceExchanges = Map.of();
        Set<String> forcedPrasIds = Set.of();
        Map<String, Double> ntcsByEic = Map.of();
        OffsetDateTime time = OffsetDateTime.now();

        setUp(referenceExchanges, ntcsByEic, time);

        DichotomyResult<DichotomyRaoResponse> result = dichotomyRunner.runDichotomy(cseRequest, cseData, network, initialIndexValue, referenceExchanges, ntcsByEic, forcedPrasIds);

        assertNotNull(result);
        assertTrue(result.isInterrupted());
    }

    private void setUp(Map<String, Double> referenceExchanges, Map<String, Double> ntcsByEic, OffsetDateTime time) throws IOException {
        when(cseRequest.getInitialDichotomyIndex()).thenReturn(-500.0);
        when(cseRequest.getDichotomyPrecision()).thenReturn(200.0);
        when(cseRequest.isImportEcProcess()).thenReturn(false);
        when(cseRequest.getProcessType()).thenReturn(ProcessType.IDCC);
        when(cseRequest.getId()).thenReturn("id");
        when(cseRequest.getTargetProcessDateTime()).thenReturn(time);

        when(cseData.getJsonCracUrl()).thenReturn("jsonCracUrl");

        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn("variantId");

        when(fileExporter.saveRaoParameters(time, ProcessType.IDCC, false)).thenReturn("raoParametersUrl");
        when(networkShifterProvider.get(cseRequest, cseData, network, referenceExchanges, ntcsByEic)).thenReturn(networkShifter);
        when(interruptionService.shouldRunBeInterruptedSoftly(any())).thenReturn(true);
    }
}
