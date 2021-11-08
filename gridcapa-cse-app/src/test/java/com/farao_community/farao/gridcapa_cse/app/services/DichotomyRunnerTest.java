/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_cse.app.services;

import com.farao_community.farao.cse.data.CseReferenceExchanges;
import com.farao_community.farao.cse.data.ntc2.Ntc2;
import com.farao_community.farao.dichotomy_runner.api.resource.CseIdccShiftDispatcherConfiguration;
import com.farao_community.farao.dichotomy_runner.api.resource.DichotomyRequest;
import com.farao_community.farao.dichotomy_runner.api.resource.SplittingFactorsConfiguration;
import com.farao_community.farao.gridcapa_cse.api.resource.CseRequest;
import com.farao_community.farao.gridcapa_cse.api.resource.ProcessType;
import com.farao_community.farao.gridcapa_cse.app.CseData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class DichotomyRunnerTest {

    @MockBean
    private FileExporter fileExporter;

    @Autowired
    private DichotomyRunner dichotomyRunner;

    private CseData cseData;
    private CseRequest cseRequest;

    @BeforeEach
    void setUp() {
        when(fileExporter.saveRaoParameters()).thenReturn("raoParametersUrl");

        cseData = Mockito.mock(CseData.class);
        when(cseData.getPreProcesedNetworkUrl()).thenReturn("preProcessedNetworkUrl");
        when(cseData.getJsonCracUrl()).thenReturn("jsonCracUrl");
        when(cseData.getReducedSplittingFactors()).thenReturn(Map.of(
            "10YFR-RTE------C", 0.2,
            "10YAT-APG------L", 0.3,
            "10YSI-ELES-----O", 0.4,
            "10YCH-SWISSGRIDZ", 0.1,
            "10YIT-GRTN-----B", -1.
        ));

        cseRequest = Mockito.mock(CseRequest.class);
        when(cseRequest.getDichotomyPrecision()).thenReturn(50.);
        when(cseRequest.getInitialDichotomyStep()).thenReturn(650.);
        when(cseRequest.getInitialDichotomyIndex()).thenReturn(null);
        when(cseRequest.getProcessType()).thenReturn(ProcessType.D2CC);
        when(cseRequest.getMergedGlskUrl()).thenReturn("glskUrl");
    }

    private void setUpForProcess(ProcessType processType) {
        when(cseRequest.getProcessType()).thenReturn(processType);
        if (processType == ProcessType.IDCC) {
            Map<String, Double> exchanges = Map.of(
                "10YFR-RTE------C", 1000.,
                "10YAT-APG------L", 700.,
                "10YSI-ELES-----O", 800.,
                "10YCH-SWISSGRIDZ", 200.
            );
            CseReferenceExchanges cseReferenceExchanges = Mockito.mock(CseReferenceExchanges.class);
            when(cseReferenceExchanges.getExchanges()).thenReturn(exchanges);
            Ntc2 ntc2 = Mockito.mock(Ntc2.class);
            when(ntc2.getExchanges()).thenReturn(exchanges);
            when(cseData.getCseReferenceExchanges()).thenReturn(cseReferenceExchanges);
            when(cseData.getNtc2()).thenReturn(ntc2);
        }
    }

    @Test
    void testForD2cc() {
        setUpForProcess(ProcessType.D2CC);
        DichotomyRequest dichotomyRequest = dichotomyRunner.getDichotomyRequest(
            cseRequest,
            cseData,
            2000
        );

        assertEquals("jsonCracUrl", dichotomyRequest.getCrac().getUrl());
        assertEquals("preProcessedNetworkUrl", dichotomyRequest.getNetwork().getUrl());
        assertEquals("glskUrl", dichotomyRequest.getGlsk().getUrl());
        assertEquals("raoParametersUrl", dichotomyRequest.getRaoParameters().getUrl());
        assertEquals(50, dichotomyRequest.getParameters().getPrecision());
        assertEquals(2000, dichotomyRequest.getParameters().getMinValue());
        assertEquals(19999, dichotomyRequest.getParameters().getMaxValue());
        assertTrue(dichotomyRequest.getParameters().getShiftDispatcherConfiguration() instanceof SplittingFactorsConfiguration);
    }

    @Test
    void testForIdcc() {
        setUpForProcess(ProcessType.IDCC);
        DichotomyRequest dichotomyRequest = dichotomyRunner.getDichotomyRequest(
            cseRequest,
            cseData,
            2000
        );

        assertEquals("jsonCracUrl", dichotomyRequest.getCrac().getUrl());
        assertEquals("preProcessedNetworkUrl", dichotomyRequest.getNetwork().getUrl());
        assertEquals("glskUrl", dichotomyRequest.getGlsk().getUrl());
        assertEquals("raoParametersUrl", dichotomyRequest.getRaoParameters().getUrl());
        assertEquals(50, dichotomyRequest.getParameters().getPrecision());
        assertEquals(2000, dichotomyRequest.getParameters().getMinValue());
        assertEquals(19999, dichotomyRequest.getParameters().getMaxValue());
        assertTrue(dichotomyRequest.getParameters().getShiftDispatcherConfiguration() instanceof CseIdccShiftDispatcherConfiguration);
    }
}
