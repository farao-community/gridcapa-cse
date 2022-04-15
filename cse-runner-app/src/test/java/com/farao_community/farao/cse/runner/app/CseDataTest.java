/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app;

import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.cse.runner.app.services.FileImporter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class CseDataTest {

    private CseData cseData;

    @MockBean
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
    void testReferenceExchangesGetterFailsForD2CC() {
        CseRequest cseRequest = mockCseRequest(ProcessType.D2CC);
        cseData = new CseData(cseRequest, fileImporter);
        assertThrows(CseInternalException.class, () -> cseData.getCseReferenceExchanges());
    }

    @Test
    void testLineFixedFlowsGetterFailsForIDCC() {
        CseRequest cseRequest = mockCseRequest(ProcessType.IDCC);
        cseData = new CseData(cseRequest, fileImporter);
        assertThrows(CseInternalException.class, () -> cseData.getLineFixedFlows());
    }
}
