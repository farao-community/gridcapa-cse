/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.api.resource;

import com.farao_community.farao.gridcapa_cse.api.resource.CseRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
class CseRequestTest {

    @Test
    void checkCseRequestForD2ccProcess() {
        CseRequest cseRequest = CseRequest.d2ccProcess("id", "2017-07-18T08:18Z", "cgmUrl", "mergedCracUrl", "mergedGlskUrl", "ntcReductionsUrl", "targetChUrl", "vulcanusUrl", "yearlyNtcUrl");
        assertNotNull(cseRequest);
        assertEquals("cgmUrl", cseRequest.getCgmUrl());
        assertEquals("ntcReductionsUrl", cseRequest.getNtcReductionsUrl());
        assertEquals("targetChUrl", cseRequest.getTargetChUrl());
        assertEquals("yearlyNtcUrl", cseRequest.getYearlyNtcUrl());
    }

    @Test
    void checkCseRequestForIdccProcess() {
        CseRequest cseRequest = CseRequest.idccProcess("id", "2017-07-18T08:18Z", "cgmUrl", "mergedCracUrl", "mergedGlskUrl", "ntcReductionsUrl", "ntc2AtItUrl", "ntc2ChItUrl", "ntc2FrItUrl", "ntc2SiItUrl", "vulcanusUrl", "yearlyNtcUrl");
        assertNotNull(cseRequest);
        assertEquals("cgmUrl", cseRequest.getCgmUrl());
        assertEquals("mergedCracUrl", cseRequest.getMergedCracUrl());
        assertEquals("vulcanusUrl", cseRequest.getVulcanusUrl());
        assertEquals("yearlyNtcUrl", cseRequest.getYearlyNtcUrl());
    }
}
