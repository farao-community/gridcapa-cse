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
        CseRequest cseRequest = new CseRequest("id", "2017-07-18T08:18Z", "cgmUrl", "mergedCracUrl", "mergedGlskUrl", "ntcReductionsUrl",
                "ntc2AtItUrl", "ntc2ChItItUrl", "ntc2FrItUrl", "ntc2SiItUrl", "targetChUrl", "vulcanusUrl", "yearlyNtcUrl");
        assertNotNull(cseRequest);
        assertEquals("cgmUrl", cseRequest.getCgmUrl());
        assertEquals("ntc2FrItUrl", cseRequest.getNtc2FrItUrl());
        assertEquals("ntc2AtItUrl", cseRequest.getNtc2AtItUrl());
        assertEquals("yearlyNtcUrl", cseRequest.getYearlyNtcUrl());
    }

    @Test
    void checkCseRequestForIdccProcess() {
        CseRequest cseRequest = new CseRequest("id", "2017-07-18T08:18Z", "cgmUrl", "mergedCracUrl", "mergedGlskUrl", "ntcReductionsUrl",
                "vulcanusUrl", "yearlyNtcUrl");
        assertNotNull(cseRequest);
        assertEquals("cgmUrl", cseRequest.getCgmUrl());
        assertEquals("mergedCracUrl", cseRequest.getMergedCracUrl());
        assertEquals("vulcanusUrl", cseRequest.getVulcanusUrl());
        assertEquals("yearlyNtcUrl", cseRequest.getYearlyNtcUrl());
    }
}
