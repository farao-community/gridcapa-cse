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
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.PhaseTapChangerStep;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.powsybl.iidm.network.IdentifiableType.GENERATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
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
        Network network = Mockito.mock(Network.class);
        Identifiable generator = Mockito.mock(Generator.class);
        Mockito.when(generator.getId()).thenReturn("anId");
        Mockito.when(network.getIdentifiable(Mockito.anyString())).thenReturn(generator);
        Mockito.when(generator.getType()).thenReturn(GENERATOR);

        Mockito.when(dichotomyResult.getHighestValidStep()).thenReturn(highestValidStep);
        Mockito.when(highestValidStep.getValidationData()).thenReturn(dichotomyRaoResponse);
        Mockito.when(dichotomyRaoResponse.getRaoResponse()).thenReturn(raoResponse);
        Mockito.when(raoResponse.getRaoResultFileUrl()).thenReturn("file://" + Objects.requireNonNull(getClass().getResource("rao-result-v1.1.json")).getPath());
        Mockito.when(raoResponse.getCracFileUrl()).thenReturn("file://" + Objects.requireNonNull(getClass().getResource("crac-for-rao-result-v1.1.json")).getPath());
        // Mock PST in networks to match data contained in crac, as it is compared to the ones in crac
        // pst 1
        TwoWindingsTransformer pst = Mockito.mock(TwoWindingsTransformer.class);
        PhaseTapChanger pstChanger = Mockito.mock(PhaseTapChanger.class);
        Mockito.when(pst.getPhaseTapChanger()).thenReturn(pstChanger);
        Mockito.when(pstChanger.getTapPosition()).thenReturn(2);
        Mockito.when(network.getTwoWindingsTransformer("pst")).thenReturn(pst);
        Map<Integer, PhaseTapChangerStep> mockedSteps = new HashMap<>();
        mockPhaseTapChangerStep(mockedSteps, 3, 3.);
        mockPhaseTapChangerStep(mockedSteps, 2, 2.5);
        mockPhaseTapChangerStep(mockedSteps, 1, 2.);
        mockPhaseTapChangerStep(mockedSteps, 0, 1.5);
        mockPhaseTapChangerStep(mockedSteps, -1, 1.);
        mockPhaseTapChangerStep(mockedSteps, -2, .5);
        mockPhaseTapChangerStep(mockedSteps, -3, .0);
        Mockito.when(pstChanger.getAllSteps()).thenReturn(mockedSteps);
        // pst 2
        TwoWindingsTransformer pst2 = Mockito.mock(TwoWindingsTransformer.class);
        PhaseTapChanger pstChanger2 = Mockito.mock(PhaseTapChanger.class);
        Mockito.when(pst2.getPhaseTapChanger()).thenReturn(pstChanger2);
        Mockito.when(pstChanger2.getTapPosition()).thenReturn(1);
        Mockito.when(network.getTwoWindingsTransformer("pst2")).thenReturn(pst2);
        Mockito.when(pstChanger2.getAllSteps()).thenReturn(mockedSteps);
        String limitingElement = dichotomyResultHelper.getLimitingElement(dichotomyResult, network);

        assertEquals("cnec1prevId", limitingElement);
    }

    private static void mockPhaseTapChangerStep(final Map<Integer, PhaseTapChangerStep> mockedSteps,
                                                final Integer tapPosition,
                                                final Double alpha) {
        PhaseTapChangerStep step03 = Mockito.mock(PhaseTapChangerStep.class);
        Mockito.when(step03.getAlpha()).thenReturn(alpha);
        mockedSteps.put(tapPosition, step03);
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
