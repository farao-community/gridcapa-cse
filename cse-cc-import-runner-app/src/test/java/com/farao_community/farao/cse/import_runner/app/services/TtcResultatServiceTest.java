/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.data.xsd.ttc_res.Timestamp;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.import_runner.app.dichotomy.DichotomyRaoResponse;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.dichotomy.api.results.LimitingCause;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.io.cse.CseCracCreationContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
public class TtcResultatServiceTest {

    @Autowired
    private TtcResultService ttcResultService;

    @MockBean
    private FileImporter fileImporter;

    @MockBean
    private FileExporter fileExporter;

    @Test
    void testLoadflowDivergedOnNetworkWithPra() throws IOException, URISyntaxException {
        String filename = "networkKO.xiidm";
        Network network = Network.read(filename, getClass().getResourceAsStream(filename));
        CseRequest cseRequest = buildTestCseRequest();
        CseData cseData = Mockito.mock(CseData.class);
        CseCracCreationContext cseCracCreationContext = Mockito.mock(CseCracCreationContext.class);
        LimitingCause limitingCause = Mockito.mock(LimitingCause.class);
        DichotomyRaoResponse dichotomyRaoResponse = Mockito.mock(DichotomyRaoResponse.class);
        RaoSuccessResponse raoResponse = Mockito.mock(RaoSuccessResponse.class);

        // When
        Mockito.when(dichotomyRaoResponse.getRaoResponse()).thenReturn(raoResponse);
        Mockito.when(raoResponse.getNetworkWithPraFileUrl()).thenReturn("test_url");
        Mockito.when(fileExporter.saveTtcResult(new Timestamp(), OffsetDateTime.parse("2021-09-01T20:30Z"), ProcessType.IDCC, true)).thenReturn("failedRaoTTCFilePath");
        Mockito.when(fileImporter.importNetwork(any())).thenReturn(network);

       // Then
        Exception exception = assertThrows(CseInternalException.class, () -> ttcResultService.saveTtcResult(cseRequest, cseData, cseCracCreationContext, dichotomyRaoResponse, limitingCause, "firstShiftNetworkName", "finalNetworkName", Collections.emptyMap(), Collections.emptyMap()));
        assertEquals("Loadflow computation diverged on network phaseShifter", exception.getMessage());
    }

    private CseRequest buildTestCseRequest() throws MalformedURLException, URISyntaxException {
        return new CseRequest(
                "ID",
                "RUNID",
                ProcessType.IDCC,
                OffsetDateTime.parse("2021-09-01T20:30Z"),
                getClass().getResource("20210901_2230_test_network_pisa_test_both_links_connected_setpoint_and_emulation_ok_for_run.uct").toURI().toURL().toString(),
                getClass().getResource("20210901_2230_213_CRAC_CO_CSE1.xml").toURI().toURL().toString(),
                getClass().getResource("crac.xml").toURI().toURL().toString(),
                getClass().getResource("20210624_2D4_NTC_reductions_CSE1_Adapted_v8_8.xml").toURI().toURL().toString(),
                null,
                null,
                null,
                null,
                null,
                getClass().getResource("vulcanus_01032021_96.xls").toURI().toURL().toString(),
                getClass().getResource("2021_2Dp_NTC_annual_CSE1_Adapted_v8_8.xml").toURI().toURL().toString(),
                null,
                null,
                null,
                50.0,
                0.0,
                null,
                true
        );
    }

}
