/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.import_runner.app.services.FileExporter;
import com.farao_community.farao.cse.import_runner.app.services.FileImporter;
import com.farao_community.farao.cse.import_runner.app.services.ForcedPrasHandler;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.dichotomy.api.exceptions.RaoFailureException;
import com.farao_community.farao.dichotomy.api.exceptions.RaoInterruptionException;
import com.farao_community.farao.dichotomy.api.exceptions.ValidationException;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.openrao.data.cracapi.Crac;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class RaoRunnerValidatorTest {

    private static final String NETWORK_PRE_SIGNED_URL = "http://network.url";
    private static final String BASE_DIR_PATH = "/base/dir/path";
    private static final String RAO_PARAMETERS_URL = "http://parameters.url";
    private static final String REQUEST_ID = "requestId";
    private static final String RUN_ID = "runId";
    private static final String CRAC_URL = "http://crac.url";

    @MockBean
    private FileExporter fileExporter;

    @MockBean
    private FileImporter fileImporter;

    @MockBean
    private Logger businessLogger;

    @Test
    void buildRaoRequestWithEmptyPreviousActionsShouldNotSaveParameters() {
        List<String> previousActions = Collections.singletonList("Action1");

        RaoRunnerValidator raoRunnerValidator = new RaoRunnerValidator(getCseRequest(ProcessType.D2CC, null), CRAC_URL, RAO_PARAMETERS_URL, null, null, null, null, null, false, businessLogger);
        RaoRequest result = raoRunnerValidator.buildRaoRequest(NETWORK_PRE_SIGNED_URL, BASE_DIR_PATH, previousActions);

        verify(fileExporter, never()).saveRaoParameters(anyString(), anyList(), any(), any(), anyBoolean());
        assertNotNull(result);
        assertEquals(RAO_PARAMETERS_URL, result.getRaoParametersFileUrl());
    }

    @Test
    void validateNetworkInterruptionException() {
        ProcessType processType = ProcessType.IDCC;
        OffsetDateTime processTargetDateTime = OffsetDateTime.now();
        RaoRunnerClient raoRunnerClient = mock(RaoRunnerClient.class);
        ForcedPrasHandler forcedPrasHandler = mock(ForcedPrasHandler.class);
        Set<String> forcedPrasIds = Set.of();
        Network network = mock(Network.class);

        RaoRunnerValidator raoRunnerValidator = new RaoRunnerValidator(
                getCseRequest(processType, processTargetDateTime),
                CRAC_URL,
                RAO_PARAMETERS_URL,
                raoRunnerClient,
                fileExporter,
                fileImporter,
                forcedPrasHandler,
                forcedPrasIds,
                false,
                businessLogger);

        when(fileExporter.getZoneId()).thenReturn("UTC");
        VariantManager variantManager = mock(VariantManager.class);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn("variantId");
        when(network.getNameOrId()).thenReturn("networkName");
        when(fileExporter.saveNetworkInArtifact(any(), any(), any(), any(), any(), anyBoolean())).thenReturn(NETWORK_PRE_SIGNED_URL);
        Crac crac = mock(Crac.class);
        when(fileImporter.importCracFromJson(CRAC_URL, network)).thenReturn(crac);
        RaoSuccessResponse raoResponse = mock(RaoSuccessResponse.class);
        when(raoRunnerClient.runRao(any())).thenReturn(raoResponse);
        when(raoResponse.isInterrupted()).thenReturn(true);

        assertThrows(RaoInterruptionException.class, () -> raoRunnerValidator.validateNetwork(network, null));
    }

    @Test
    void validateNetworkRaoFailureException() {
        ProcessType processType = ProcessType.IDCC;
        OffsetDateTime processTargetDateTime = OffsetDateTime.now();
        RaoRunnerClient raoRunnerClient = mock(RaoRunnerClient.class);
        ForcedPrasHandler forcedPrasHandler = mock(ForcedPrasHandler.class);
        Set<String> forcedPrasIds = Set.of();
        Network network = mock(Network.class);

        RaoRunnerValidator raoRunnerValidator = new RaoRunnerValidator(
                getCseRequest(processType, processTargetDateTime),
                CRAC_URL,
                RAO_PARAMETERS_URL,
                raoRunnerClient,
                fileExporter,
                fileImporter,
                forcedPrasHandler,
                forcedPrasIds,
                false,
                businessLogger);

        when(fileExporter.getZoneId()).thenReturn("UTC");
        VariantManager variantManager = mock(VariantManager.class);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn("variantId");
        when(network.getNameOrId()).thenReturn("networkName");
        when(fileExporter.saveNetworkInArtifact(any(), any(), any(), any(), any(), anyBoolean())).thenReturn(NETWORK_PRE_SIGNED_URL);
        Crac crac = mock(Crac.class);
        when(fileImporter.importCracFromJson(CRAC_URL, network)).thenReturn(crac);
        RaoFailureResponse raoResponse = new RaoFailureResponse.Builder().withErrorMessage("error").build();
        when(raoRunnerClient.runRao(any())).thenReturn(raoResponse);

        final RaoFailureException exception = assertThrows(RaoFailureException.class, () -> raoRunnerValidator.validateNetwork(network, null));
        assertEquals("error", exception.getMessage());
    }

    @Test
    void validateNetworkValidationException() {
        ProcessType processType = ProcessType.IDCC;
        OffsetDateTime processTargetDateTime = OffsetDateTime.now();
        RaoRunnerClient raoRunnerClient = mock(RaoRunnerClient.class);
        ForcedPrasHandler forcedPrasHandler = mock(ForcedPrasHandler.class);
        Set<String> forcedPrasIds = Set.of(); //
        Network network = mock(Network.class);

        RaoRunnerValidator raoRunnerValidator = new RaoRunnerValidator(
                getCseRequest(processType, processTargetDateTime),
                CRAC_URL,
                RAO_PARAMETERS_URL,
                raoRunnerClient,
                fileExporter,
                fileImporter,
                forcedPrasHandler,
                forcedPrasIds,
                false,
                businessLogger);

        when(fileExporter.getZoneId()).thenReturn("UTC");
        VariantManager variantManager = mock(VariantManager.class);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn("variantId");
        when(network.getNameOrId()).thenReturn("networkName");
        when(fileExporter.saveNetworkInArtifact(any(), any(), any(), any(), any(), anyBoolean())).thenReturn(NETWORK_PRE_SIGNED_URL);
        when(fileImporter.importCracFromJson(CRAC_URL, network)).thenThrow(RuntimeException.class);
        assertThrows(ValidationException.class, () -> raoRunnerValidator.validateNetwork(network, null));
    }

    private CseRequest getCseRequest(ProcessType type, OffsetDateTime timestamp) {
        CseRequest cseRequest = mock(CseRequest.class);
        when(cseRequest.getId()).thenReturn(REQUEST_ID);
        when(cseRequest.getCurrentRunId()).thenReturn(RUN_ID);
        when(cseRequest.getTargetProcessDateTime()).thenReturn(timestamp);
        when(cseRequest.getProcessType()).thenReturn(type);
        return cseRequest;
    }
}
