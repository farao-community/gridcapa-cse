/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class DichotomyResultHelperTest {

    @Autowired
    DichotomyResultHelper dichotomyResultHelper;

    @Test
    void testLimitingElement() {
        DichotomyResult<DichotomyRaoResponse> dichotomyResult = Mockito.mock(DichotomyResult.class);
        DichotomyStepResult<DichotomyRaoResponse> highestValidStep = Mockito.mock(DichotomyStepResult.class);
        DichotomyRaoResponse dichotomyRaoResponse = Mockito.mock(DichotomyRaoResponse.class);
        RaoSuccessResponse raoResponse = Mockito.mock(RaoSuccessResponse.class);

        String networkFilename = "TestCase16Nodes.xiidm";
        Network network = Network.read(networkFilename, getClass().getResourceAsStream(networkFilename));
        Mockito.when(dichotomyResult.getHighestValidStep()).thenReturn(highestValidStep);
        Mockito.when(highestValidStep.getValidationData()).thenReturn(dichotomyRaoResponse);
        Mockito.when(dichotomyRaoResponse.getRaoResponse()).thenReturn(raoResponse);
        Mockito.when(raoResponse.getRaoResultFileUrl()).thenReturn("file://" + Objects.requireNonNull(getClass().getResource("raoResult.json")).getPath());
        Mockito.when(raoResponse.getCracFileUrl()).thenReturn("file://" + Objects.requireNonNull(getClass().getResource("SL_ep13us3case1.json")).getPath());
        String limitingElement = dichotomyResultHelper.getLimitingElement(dichotomyResult, network);

        assertEquals("fr1_fr4", limitingElement);
    }

    @Test
    void testComputeLowestUnsecureItalianImport() {
        DichotomyResult<DichotomyRaoResponse> dichotomyResult = Mockito.mock(DichotomyResult.class);
        DichotomyStepResult<DichotomyRaoResponse> lowestInvalidStep = Mockito.mock(DichotomyStepResult.class);
        DichotomyRaoResponse dichotomyRaoResponse = Mockito.mock(DichotomyRaoResponse.class);
        RaoSuccessResponse raoResponse = Mockito.mock(RaoSuccessResponse.class);

        Mockito.when(dichotomyResult.getLowestInvalidStep()).thenReturn(lowestInvalidStep);
        Mockito.when(lowestInvalidStep.getValidationData()).thenReturn(dichotomyRaoResponse);
        Mockito.when(dichotomyRaoResponse.getRaoResponse()).thenReturn(raoResponse);
        Mockito.when(raoResponse.getNetworkWithPraFileUrl()).thenReturn("file://" + Objects.requireNonNull(getClass().getResource("CSE_no_normal_glsk_variation.uct")).getPath());

        double lowestUnsecureItalianImport = dichotomyResultHelper.computeLowestUnsecureItalianImport(dichotomyResult);

        assertEquals(6000, lowestUnsecureItalianImport, 1);
    }

    @Test
    void testComputeHighestSecureItalianImport() {
        DichotomyResult<DichotomyRaoResponse> dichotomyResult = Mockito.mock(DichotomyResult.class);
        DichotomyStepResult<DichotomyRaoResponse> highestValidStep = Mockito.mock(DichotomyStepResult.class);
        DichotomyRaoResponse dichotomyRaoResponse = Mockito.mock(DichotomyRaoResponse.class);
        RaoSuccessResponse raoResponse = Mockito.mock(RaoSuccessResponse.class);

        Mockito.when(dichotomyResult.getHighestValidStep()).thenReturn(highestValidStep);
        Mockito.when(highestValidStep.getValidationData()).thenReturn(dichotomyRaoResponse);
        Mockito.when(dichotomyRaoResponse.getRaoResponse()).thenReturn(raoResponse);
        Mockito.when(raoResponse.getNetworkWithPraFileUrl()).thenReturn("file://" + Objects.requireNonNull(getClass().getResource("CSE_no_normal_glsk_variation.uct")).getPath());

        double highestSecureItalianImport = dichotomyResultHelper.computeHighestSecureItalianImport(dichotomyResult);

        assertEquals(6000, highestSecureItalianImport, 1);
    }
}
