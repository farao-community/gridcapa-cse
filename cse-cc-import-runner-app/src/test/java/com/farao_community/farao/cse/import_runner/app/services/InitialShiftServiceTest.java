/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.data.ntc.Ntc;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.import_runner.app.dichotomy.CseCountry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@SpringBootTest
class InitialShiftServiceTest {

    @Autowired
    InitialShiftService initialShiftService;

//    @InjectMocks
//    private final Logger businessLogger;
//    private final FileExporter fileExporter;
//    private final FileImporter fileImporter;
//    private final ProcessConfiguration processConfiguration;

    @Test
    void getInitialShiftValues() {
        Ntc ntc = Mockito.mock(Ntc.class);
        CseData cseData = Mockito.mock(CseData.class);
        Map<String, Double> flowPerCountryOnNotModelizedLines = Map.of(
            "AT", 10.,
            "CH", 20.,
            "FR", 30.,
            "SI", 40.);
        Map<String, Double> referenceExchanges = Map.of(
            CseCountry.AT.getEiCode(), 1.,
            CseCountry.CH.getEiCode(), 2.,
            CseCountry.FR.getEiCode(), 3.,
            CseCountry.SI.getEiCode(), 4.);
        Map<String, Double> ntcsByEic = Map.of(
            CseCountry.AT.getEiCode(), 100.,
            CseCountry.CH.getEiCode(), 200.,
            CseCountry.FR.getEiCode(), 300.,
            CseCountry.SI.getEiCode(), 400.);

        Mockito.when(cseData.getNtc()).thenReturn(ntc);
        Mockito.when(ntc.getFlowPerCountryOnNotModelizedLines()).thenReturn(flowPerCountryOnNotModelizedLines);

        Map<String, Double> initialShiftValues = initialShiftService.getInitialShiftValues(cseData, referenceExchanges, ntcsByEic);

        Assertions.assertThat(initialShiftValues)
            .containsEntry(CseCountry.AT.getEiCode(), 89.)
            .containsEntry(CseCountry.CH.getEiCode(), 178.)
            .containsEntry(CseCountry.FR.getEiCode(), 267.)
            .containsEntry(CseCountry.SI.getEiCode(), 356.)
            .containsEntry(CseCountry.IT.getEiCode(), -890.);
    }

}
