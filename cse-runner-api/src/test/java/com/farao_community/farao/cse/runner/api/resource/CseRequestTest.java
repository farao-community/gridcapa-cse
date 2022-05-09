/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.api.resource;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
class CseRequestTest {

    @Test
    void checkCseRequestForD2ccProcess() {
        CseRequest cseRequest = CseRequest.d2ccProcess(
            "id",
            OffsetDateTime.parse("2017-07-18T08:18Z"),
            "cgmUrl",
            "mergedCracUrl",
            "mergedGlskUrl",
            "ntcReductionsUrl",
            "targetChUrl",
            "yearlyNtcUrl",
            "forcedPrasUrl",
            50,
            650,
            null);
        assertNotNull(cseRequest);
        assertEquals("cgmUrl", cseRequest.getCgmUrl());
        assertEquals("ntcReductionsUrl", cseRequest.getNtcReductionsUrl());
        assertEquals("targetChUrl", cseRequest.getTargetChUrl());
        assertEquals("yearlyNtcUrl", cseRequest.getYearlyNtcUrl());
        assertEquals(ProcessType.D2CC, cseRequest.getProcessType());
        assertEquals(50, cseRequest.getDichotomyPrecision());
        assertEquals(650, cseRequest.getInitialDichotomyStep());
        assertNull(cseRequest.getInitialDichotomyIndex());
    }

    @Test
    void checkCseRequestForIdccProcess() {
        CseRequest cseRequest = CseRequest.idccProcess(
            "id",
            OffsetDateTime.parse("2017-07-18T08:18Z"),
            "cgmUrl",
            "mergedCracUrl",
            "mergedGlskUrl",
            "ntcReductionsUrl",
            "ntc2AtItUrl",
            "ntc2ChItUrl",
            "ntc2FrItUrl",
            "ntc2SiItUrl",
            "vulcanusUrl",
            "yearlyNtcUrl",
            "forcedPrasUrl",
            50,
            650,
            2500.);
        assertNotNull(cseRequest);
        assertEquals("cgmUrl", cseRequest.getCgmUrl());
        assertEquals("mergedCracUrl", cseRequest.getMergedCracUrl());
        assertEquals("vulcanusUrl", cseRequest.getVulcanusUrl());
        assertEquals("yearlyNtcUrl", cseRequest.getYearlyNtcUrl());
        assertEquals(ProcessType.IDCC, cseRequest.getProcessType());
        assertEquals(50, cseRequest.getDichotomyPrecision());
        assertEquals(650, cseRequest.getInitialDichotomyStep());
        assertEquals(2500, cseRequest.getInitialDichotomyIndex());
    }
}
