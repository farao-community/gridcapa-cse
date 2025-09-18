/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.computation.BorderExchanges;
import com.farao_community.farao.cse.computation.LoadflowComputationException;
import com.farao_community.farao.cse.data.ntc.Ntc;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.import_runner.app.dichotomy.CseCountry;
import com.farao_community.farao.cse.import_runner.app.dichotomy.CseNetworkExporter;
import com.farao_community.farao.cse.import_runner.app.dichotomy.NetworkShifterUtil;
import com.farao_community.farao.cse.import_runner.app.dichotomy.ZonalScalableProvider;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

@SpringBootTest
class InitialShiftServiceTest {

    @Autowired
    InitialShiftService initialShiftService;

    @MockitoBean
    ZonalScalableProvider zonalScalableProvider;

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

    @Test
    void loadflowDivergenceBeforeShift() {
        try (MockedStatic<BorderExchanges> mockedBorderExchanges = Mockito.mockStatic(BorderExchanges.class);
             MockedConstruction<CseNetworkExporter> mockedCseNetworkExporter = Mockito.mockConstruction(CseNetworkExporter.class)) {

            mockedBorderExchanges.when(() -> BorderExchanges.computeCseCountriesBalances(Mockito.any(Network.class)))
                    .thenThrow(LoadflowComputationException.class);

            final Network network = Mockito.mock(Network.class);

            Assertions.assertThatExceptionOfType(LoadflowComputationException.class)
                    .isThrownBy(() -> initialShiftService.performInitialShiftFromVulcanusLevelToNtcLevel(network, null, null, null, null));
            Assertions.assertThat(mockedCseNetworkExporter.constructed()).hasSize(1);
            Mockito.verify(mockedCseNetworkExporter.constructed().getFirst(), Mockito.times(1)).export(network, "beforeInitialShift");
        }
    }

    @Test
    void loadflowDivergenceAfterShift() {
        try (MockedStatic<BorderExchanges> mockedBorderExchanges = Mockito.mockStatic(BorderExchanges.class);
             MockedStatic<NetworkShifterUtil> mockedNetworkShifterUtil = Mockito.mockStatic(NetworkShifterUtil.class);
             MockedConstruction<CseNetworkExporter> mockedCseNetworkExporter = Mockito.mockConstruction(CseNetworkExporter.class)) {

            // Given
            final Map<String, Double> commonEiCodeMap = Map.of(
                    CseCountry.FR.getEiCode(), 0.,
                    CseCountry.CH.getEiCode(), 0.,
                    CseCountry.AT.getEiCode(), 0.,
                    CseCountry.SI.getEiCode(), 0.);
            mockedBorderExchanges.when(() -> BorderExchanges.computeCseCountriesBalances(Mockito.any(Network.class)))
                    .thenReturn(commonEiCodeMap)
                    .thenThrow(LoadflowComputationException.class);
            mockedNetworkShifterUtil.when(() -> NetworkShifterUtil.convertMapByCountryToMapByEic(Mockito.anyMap()))
                    .thenReturn(commonEiCodeMap);

            final Network network = Mockito.mock(Network.class);
            final VariantManager variantManager = Mockito.mock(VariantManager.class);
            Mockito.when(network.getVariantManager()).thenReturn(variantManager);
            Mockito.when(variantManager.getWorkingVariantId()).thenReturn("test");

            final CseRequest cseRequest = Mockito.mock(CseRequest.class);
            Mockito.when(cseRequest.getProcessType()).thenReturn(ProcessType.D2CC);

            final CseData cseData = Mockito.mock(CseData.class);
            final Ntc ntc = Mockito.mock(Ntc.class);
            Mockito.when(cseData.getNtc()).thenReturn(ntc);
            Mockito.when(ntc.getFlowPerCountryOnNotModelizedLines()).thenReturn(commonEiCodeMap);

            final ZonalData<Scalable> zonalData = Mockito.mock(ZonalData.class);
            final Scalable scalable = Mockito.mock(Scalable.class);
            Mockito.when(zonalScalableProvider.get(Mockito.any(), Mockito.any(), Mockito.any()))
                    .thenReturn(zonalData);
            Mockito.when(zonalData.getData(Mockito.any())).thenReturn(scalable);
            Mockito.when(scalable.scale(Mockito.any(), Mockito.anyDouble(), Mockito.any()))
                    .thenReturn(0.);

            // When-Then
            Assertions.assertThatExceptionOfType(LoadflowComputationException.class)
                    .isThrownBy(() -> initialShiftService.performInitialShiftFromVulcanusLevelToNtcLevel(network, cseData, cseRequest, commonEiCodeMap, commonEiCodeMap));
            Assertions.assertThat(mockedCseNetworkExporter.constructed()).hasSize(1);
            Mockito.verify(mockedCseNetworkExporter.constructed().getFirst(), Mockito.times(1)).export(network, "afterInitialShift");
        }
    }
}
