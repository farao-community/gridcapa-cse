/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.import_runner.app.services.ForcedPrasHandler;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.cse.import_runner.app.services.FileExporter;
import com.farao_community.farao.cse.import_runner.app.services.FileImporter;
import com.farao_community.farao.cse.import_runner.app.util.MinioStorageHelper;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.exceptions.ValidationException;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RaoRunnerValidator implements NetworkValidator<DichotomyRaoResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaoRunnerValidator.class);

    private final ProcessType processType;
    private final String requestId;
    private final OffsetDateTime processTargetDateTime;
    private final String cracUrl;
    private final String raoParametersUrl;
    private final RaoRunnerClient raoRunnerClient;
    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final ForcedPrasHandler forcedPrasHandler;
    private final Set<String> forcedPrasIds;
    private int variantCounter = 0;

    public RaoRunnerValidator(ProcessType processType,
                              String requestId,
                              OffsetDateTime processTargetDateTime,
                              String cracUrl,
                              String raoParametersUrl,
                              RaoRunnerClient raoRunnerClient,
                              FileExporter fileExporter,
                              FileImporter fileImporter,
                              ForcedPrasHandler forcedPrasHandler,
                              Set<String> forcedPrasIds) {
        this.processType = processType;
        this.requestId = requestId;
        this.processTargetDateTime = processTargetDateTime;
        this.cracUrl = cracUrl;
        this.raoParametersUrl = raoParametersUrl;
        this.raoRunnerClient = raoRunnerClient;
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.forcedPrasHandler = forcedPrasHandler;
        this.forcedPrasIds = forcedPrasIds;
    }

    @Override
    public DichotomyStepResult<DichotomyRaoResponse> validateNetwork(Network network) throws ValidationException {
        String scaledNetworkDirPath = generateScaledNetworkDirPath(network);
        String scaledNetworkName = network.getNameOrId() + ".xiidm";
        String networkPresignedUrl = fileExporter.saveNetworkInArtifact(network, scaledNetworkDirPath + scaledNetworkName, "", processTargetDateTime, processType);
        RaoRequest raoRequest = buildRaoRequest(networkPresignedUrl, scaledNetworkDirPath);

        try {
            Crac crac = fileImporter.importCracFromJson(cracUrl);

            // We get only the RAs that have been actually applied
            // Even if the set is empty we still do the computation, in worst case scenario the computation is useless
            Set<String> appliedForcedPras = forcedPrasHandler.forcePras(forcedPrasIds, network, crac);

            LOGGER.info("RAO request sent: {}", raoRequest);
            RaoResponse raoResponse = raoRunnerClient.runRao(raoRequest);
            LOGGER.info("RAO response received: {}", raoResponse);
            RaoResult raoResult = fileImporter.importRaoResult(raoResponse.getRaoResultFileUrl(), crac);

            DichotomyRaoResponse dichotomyRaoResponse = new DichotomyRaoResponse(raoResponse, appliedForcedPras);
            return DichotomyStepResult.fromNetworkValidationResult(raoResult, dichotomyRaoResponse);
        } catch (RuntimeException | IOException e) {
            throw new ValidationException("RAO run failed. Nested exception: " + e.getMessage());
        }
    }

    private RaoRequest buildRaoRequest(String networkPresignedUrl, String scaledNetworkDirPath) {
        return new RaoRequest(requestId, networkPresignedUrl, cracUrl, raoParametersUrl, scaledNetworkDirPath);
    }

    private String generateScaledNetworkDirPath(Network network) {
        String basePath = MinioStorageHelper.makeDestinationMinioPath(processTargetDateTime, processType, MinioStorageHelper.FileKind.ARTIFACTS, ZoneId.of(fileExporter.getZoneId()));
        String variantName = network.getVariantManager().getWorkingVariantId();
        return String.format("%s/%s-%s/", basePath, ++variantCounter, variantName);
    }
}
