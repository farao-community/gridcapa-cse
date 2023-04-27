/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.import_runner.app.configurations.ProcessConfiguration;
import com.farao_community.farao.cse.import_runner.app.services.FileExporter;
import com.farao_community.farao.cse.import_runner.app.services.FileImporter;
import com.farao_community.farao.cse.import_runner.app.services.ForcedPrasHandler;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.exceptions.RaoInterruptionException;
import com.farao_community.farao.dichotomy.api.exceptions.ValidationException;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class RaoRunnerValidatorTest {

    @Test
    void validateNetwork() throws ValidationException, RaoInterruptionException {
        ProcessType processType = ProcessType.IDCC;
        String requestId = "id";
        OffsetDateTime processTargetDateTime = OffsetDateTime.now();
        String cracUrl = "cracUrl";
        String raoParametersUrl = "raoParametersUrl";
        RaoRunnerClient raoRunnerClient = mock(RaoRunnerClient.class);
        FileExporter fileExporter = mock(FileExporter.class);
        FileImporter fileImporter = mock(FileImporter.class);
        ProcessConfiguration processConfiguration = mock(ProcessConfiguration.class);
        ForcedPrasHandler forcedPrasHandler = mock(ForcedPrasHandler.class);
        Set<String> forcedPrasIds = Set.of();
        boolean isImportEcProcess = false;
        String baseCaseCgmVersion = "cgmVersion";
        Network network = mock(Network.class);

        RaoRunnerValidator raoRunnerValidator = new RaoRunnerValidator(processType,
            requestId,
            processTargetDateTime,
            cracUrl,
            raoParametersUrl,
            raoRunnerClient,
            fileExporter,
            fileImporter,
            processConfiguration,
            forcedPrasHandler,
            forcedPrasIds,
            isImportEcProcess,
            baseCaseCgmVersion);

        when(fileExporter.getZoneId()).thenReturn("UTC");
        VariantManager variantManager = mock(VariantManager.class);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn("variantId");
        when(network.getNameOrId()).thenReturn("networkName");
        when(fileExporter.getNetworkFilePathByState(any(), any(), anyBoolean(), any(), any())).thenReturn("networkFilePath");
        when(fileExporter.saveNetworkInArtifact(any(), any(), any(), any(), any(), anyBoolean())).thenReturn("networkUrl");
        Crac crac = mock(Crac.class);
        when(fileImporter.importCracFromJson(cracUrl)).thenReturn(crac);
        RaoResponse raoResponse = mock(RaoResponse.class);
        when(raoRunnerClient.runRao(any())).thenReturn(raoResponse);
        when(raoResponse.isInterrupted()).thenReturn(false);
        when(raoResponse.getRaoResultFileUrl()).thenReturn("raoResultFileUrl");
        RaoResult raoResult = mock(RaoResult.class);
        when(fileImporter.importRaoResult("raoResultFileUrl", crac)).thenReturn(raoResult);

        DichotomyStepResult<DichotomyRaoResponse> result = raoRunnerValidator.validateNetwork(network, null);

        verify(fileExporter, times(1)).exportAndUploadNetwork(any(), any(), any(), any(), any(), any(), any(), anyBoolean());
        assertTrue(result.isValid());
        assertEquals(raoResult, result.getRaoResult());
    }

    @Test
    void validateNetworkInterruptionException() {
        ProcessType processType = ProcessType.IDCC;
        String requestId = "id";
        OffsetDateTime processTargetDateTime = OffsetDateTime.now();
        String cracUrl = "cracUrl";
        String raoParametersUrl = "raoParametersUrl";
        RaoRunnerClient raoRunnerClient = mock(RaoRunnerClient.class);
        FileExporter fileExporter = mock(FileExporter.class);
        FileImporter fileImporter = mock(FileImporter.class);
        ProcessConfiguration processConfiguration = mock(ProcessConfiguration.class);
        ForcedPrasHandler forcedPrasHandler = mock(ForcedPrasHandler.class);
        Set<String> forcedPrasIds = Set.of();
        boolean isImportEcProcess = false;
        String baseCaseCgmVersion = "cgmVersion";
        Network network = mock(Network.class);

        RaoRunnerValidator raoRunnerValidator = new RaoRunnerValidator(processType,
            requestId,
            processTargetDateTime,
            cracUrl,
            raoParametersUrl,
            raoRunnerClient,
            fileExporter,
            fileImporter,
            processConfiguration,
            forcedPrasHandler,
            forcedPrasIds,
            isImportEcProcess,
            baseCaseCgmVersion);

        when(fileExporter.getZoneId()).thenReturn("UTC");
        VariantManager variantManager = mock(VariantManager.class);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn("variantId");
        when(network.getNameOrId()).thenReturn("networkName");
        when(fileExporter.getNetworkFilePathByState(any(), any(), anyBoolean(), any(), any())).thenReturn("networkFilePath");
        when(fileExporter.saveNetworkInArtifact(any(), any(), any(), any(), any(), anyBoolean())).thenReturn("networkUrl");
        Crac crac = mock(Crac.class);
        when(fileImporter.importCracFromJson(cracUrl)).thenReturn(crac);
        RaoResponse raoResponse = mock(RaoResponse.class);
        when(raoRunnerClient.runRao(any())).thenReturn(raoResponse);
        when(raoResponse.isInterrupted()).thenReturn(true);

        assertThrows(RaoInterruptionException.class, () -> raoRunnerValidator.validateNetwork(network, null));
    }

    @Test
    void validateNetworkValidationException() {
        ProcessType processType = ProcessType.IDCC;
        String requestId = "id";
        OffsetDateTime processTargetDateTime = OffsetDateTime.now();
        String cracUrl = "cracUrl";
        String raoParametersUrl = "raoParametersUrl";
        RaoRunnerClient raoRunnerClient = mock(RaoRunnerClient.class);
        FileExporter fileExporter = mock(FileExporter.class);
        FileImporter fileImporter = mock(FileImporter.class);
        ProcessConfiguration processConfiguration = mock(ProcessConfiguration.class);
        ForcedPrasHandler forcedPrasHandler = mock(ForcedPrasHandler.class);
        Set<String> forcedPrasIds = Set.of();
        boolean isImportEcProcess = false;
        String baseCaseCgmVersion = "cgmVersion";
        Network network = mock(Network.class);

        RaoRunnerValidator raoRunnerValidator = new RaoRunnerValidator(processType,
            requestId,
            processTargetDateTime,
            cracUrl,
            raoParametersUrl,
            raoRunnerClient,
            fileExporter,
            fileImporter,
            processConfiguration,
            forcedPrasHandler,
            forcedPrasIds,
            isImportEcProcess,
            baseCaseCgmVersion);

        when(fileExporter.getZoneId()).thenReturn("UTC");
        VariantManager variantManager = mock(VariantManager.class);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn("variantId");
        when(network.getNameOrId()).thenReturn("networkName");
        when(fileExporter.getNetworkFilePathByState(any(), any(), anyBoolean(), any(), any())).thenReturn("networkFilePath");
        when(fileExporter.saveNetworkInArtifact(any(), any(), any(), any(), any(), anyBoolean())).thenReturn("networkUrl");
        when(fileImporter.importCracFromJson(cracUrl)).thenThrow(RuntimeException.class);

        assertThrows(ValidationException.class, () -> raoRunnerValidator.validateNetwork(network, null));
    }
}
