/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app;

import com.farao_community.farao.cse.data.ntc.Ntc;
import com.farao_community.farao.cse.data.target_ch.LineFixedFlows;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.cse.import_runner.app.services.FileImporter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class CseDataTest {

    private CseData cseData;

    @Autowired
    FileImporter fileImporter;

    private static CseRequest mockCseRequest(ProcessType processType) {
        CseRequest cseRequest = Mockito.mock(CseRequest.class);
        Mockito.when(cseRequest.getProcessType()).thenReturn(processType);
        return cseRequest;
    }

    @Test
    void testNtc2GetterFailsForD2CC() {
        CseRequest cseRequest = mockCseRequest(ProcessType.D2CC);
        cseData = new CseData(cseRequest, fileImporter);
        assertThrows(CseInternalException.class, () -> cseData.getNtc2());
    }

    @Test
    void testLineFixedFlowsGetterFailsForIDCC() {
        CseRequest cseRequest = mockCseRequest(ProcessType.IDCC);
        cseData = new CseData(cseRequest, fileImporter);
        assertThrows(CseInternalException.class, () -> cseData.getLineFixedFlows());
    }

    @Test
    void testLineFixedFlowsGetterForD2CC() {
        CseRequest cseRequest = mockCseRequest(ProcessType.D2CC);
        Mockito.when(cseRequest.getTargetProcessDateTime()).thenReturn(OffsetDateTime.parse("2021-01-01T00:00Z"));
        Mockito.when(cseRequest.getTargetChUrl()).thenReturn(Objects.requireNonNull(getClass().getResource("services/20210901_2230_213_CRAC_CO_CSE1.xml")).toString());
        Mockito.when(cseRequest.isImportEcProcess()).thenReturn(false);
        cseData = new CseData(cseRequest, fileImporter);
        assertNotNull(cseData);
        LineFixedFlows lines = cseData.getLineFixedFlows();
        assertNotNull(lines);
    }

    @Test
    void testLineFixedFlowsGetterForD2CCAdapted() {
        CseRequest cseRequest = mockCseRequest(ProcessType.D2CC);
        Mockito.when(cseRequest.getTargetProcessDateTime()).thenReturn(OffsetDateTime.parse("2021-01-01T00:00Z"));
        Mockito.when(cseRequest.getTargetChUrl()).thenReturn(Objects.requireNonNull(getClass().getResource("services/20210901_2230_213_CRAC_CO_CSE1.xml")).toString());
        Mockito.when(cseRequest.isImportEcProcess()).thenReturn(true);
        cseData = new CseData(cseRequest, fileImporter);
        assertNotNull(cseData);
        LineFixedFlows lines = cseData.getLineFixedFlows();
        assertNotNull(lines);
    }

    @Test
    void testGetNtc() {
        CseRequest cseRequest = mockCseRequest(ProcessType.D2CC);
        Mockito.when(cseRequest.getTargetProcessDateTime()).thenReturn(OffsetDateTime.parse("2021-01-01T00:00Z"));
        Mockito.when(cseRequest.getYearlyNtcUrl()).thenReturn(Objects.requireNonNull(getClass().getResource("services/20210901_2230_213_CRAC_CO_CSE1.xml")).toString());
        Mockito.when(cseRequest.getNtcReductionsUrl()).thenReturn(Objects.requireNonNull(getClass().getResource("services/20210901_2230_213_CRAC_CO_CSE1.xml")).toString());
        Mockito.when(cseRequest.isImportEcProcess()).thenReturn(false);
        cseData = new CseData(cseRequest, fileImporter);
        assertNotNull(cseData);
        Ntc ntc = cseData.getNtc();
        assertNotNull(ntc);
        ntc = cseData.getNtc();
        assertNotNull(ntc);
    }

    @Test
    void testGetNtcAdapted() {
        CseRequest cseRequest = mockCseRequest(ProcessType.D2CC);
        Mockito.when(cseRequest.getTargetProcessDateTime()).thenReturn(OffsetDateTime.parse("2021-01-01T00:00Z"));
        Mockito.when(cseRequest.getYearlyNtcUrl()).thenReturn(Objects.requireNonNull(getClass().getResource("services/20210901_2230_213_CRAC_CO_CSE1.xml")).toString());
        Mockito.when(cseRequest.getNtcReductionsUrl()).thenReturn(Objects.requireNonNull(getClass().getResource("services/20210901_2230_213_CRAC_CO_CSE1.xml")).toString());
        Mockito.when(cseRequest.isImportEcProcess()).thenReturn(true);
        cseData = new CseData(cseRequest, fileImporter);
        assertNotNull(cseData);
        Ntc ntc = cseData.getNtc();
        assertNotNull(ntc);
        ntc = cseData.getNtc();
        assertNotNull(ntc);
    }
}
