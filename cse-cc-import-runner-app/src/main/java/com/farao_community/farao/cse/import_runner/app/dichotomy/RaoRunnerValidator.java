/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.import_runner.app.services.ForcedPrasHandler;
import com.farao_community.farao.cse.import_runner.app.util.FlowEvaluator;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.cse.import_runner.app.services.FileExporter;
import com.farao_community.farao.cse.import_runner.app.services.FileImporter;
import com.farao_community.farao.cse.import_runner.app.util.MinioStorageHelper;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.exceptions.ValidationException;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
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
    public DichotomyStepResult<DichotomyRaoResponse> validateNetwork(Network network, DichotomyStepResult lastDichotomyStepResult) throws ValidationException {
        String baseDirPathForCurrentStep = generateBaseDirPathFromScaledNetwork(network);
        String scaledNetworkName = network.getNameOrId() + ".xiidm";
        String networkPresignedUrl = fileExporter.saveNetworkInArtifact(network, baseDirPathForCurrentStep + scaledNetworkName, "", processTargetDateTime, processType);

        try {
            Crac crac = fileImporter.importCracFromJson(cracUrl);
            List<String> appliedRemedialActionInPreviousStep = lastDichotomyStepResult != null && lastDichotomyStepResult.getRaoResult() != null ? lastDichotomyStepResult.getRaoResult().getActivatedNetworkActionsDuringState(crac.getPreventiveState())
                     .stream().map(NetworkAction::getId).collect(Collectors.toList()) : Collections.emptyList();
            RaoRequest raoRequest = buildRaoRequest(networkPresignedUrl, baseDirPathForCurrentStep, appliedRemedialActionInPreviousStep);
            // We don't stop computation even if there are no applied RAs, because we cannot be sure in the case where
            // the RAs are applicable on constraint, that it won't be applicable later on in the dichotomy (higher index)
            // So if we throw validation exception for example when no RAs are applied index will go lower, and it will
            // downgrade dichotomy results. So it could represent extra unnecessary RAOs, but otherwise it would cause
            // high losses of TTC.
            Set<String> appliedForcedPras = applyForcedPras(crac, network);

            LOGGER.info("RAO request sent: {}", raoRequest);
            RaoResponse raoResponse = raoRunnerClient.runRao(raoRequest);
            LOGGER.info("RAO response received: {}", raoResponse);
            RaoResult raoResult = fileImporter.importRaoResult(raoResponse.getRaoResultFileUrl(), crac);

            DichotomyRaoResponse dichotomyRaoResponse = new DichotomyRaoResponse(raoResponse, appliedForcedPras);
            return DichotomyStepResult.fromNetworkValidationResult(raoResult, dichotomyRaoResponse);
        } catch (RuntimeException e) {
            LOGGER.error("Exception occured during validation", e);
            throw new ValidationException("RAO run failed. Nested exception: " + e.getMessage());
        }
    }

    private Set<String> applyForcedPras(Crac crac, Network network) {
        if (!forcedPrasIds.isEmpty()) {
            // It computes reference flows on the network to be able to evaluate PRAs availability
            FlowResult flowResult = FlowEvaluator.evaluate(crac, network);

            // We get only the RAs that have been actually applied
            // Even if the set is empty we still do the computation, in worst case scenario the computation is useless
            return forcedPrasHandler.forcePras(forcedPrasIds, network, crac, flowResult);
        } else {
            return Collections.emptySet();
        }
    }

    private RaoRequest buildRaoRequest(String networkPreSignedUrl, String baseDirPathForCurrentStep, List<String> appliedRemedialActionInPreviousStep) {
        if (appliedRemedialActionInPreviousStep.isEmpty() || appliedRemedialActionInPreviousStep.size() == 1) {
            return new RaoRequest(requestId, networkPreSignedUrl, cracUrl, raoParametersUrl, baseDirPathForCurrentStep);
        } else {
            List<List<String>> networkActionIdCombinationsAppliedInPreviousStep = List.of(appliedRemedialActionInPreviousStep);
            String raoParametersWithAppliedRemedialActionInPreviousStepUrl = fileExporter.saveRaoParameters(baseDirPathForCurrentStep, networkActionIdCombinationsAppliedInPreviousStep, processTargetDateTime, processType);
            return new RaoRequest(requestId, networkPreSignedUrl, cracUrl, raoParametersWithAppliedRemedialActionInPreviousStepUrl, baseDirPathForCurrentStep);
        }
    }

    private String generateBaseDirPathFromScaledNetwork(Network network) {
        String basePath = MinioStorageHelper.makeDestinationMinioPath(processTargetDateTime, processType, MinioStorageHelper.FileKind.ARTIFACTS, ZoneId.of(fileExporter.getZoneId()));
        String variantName = network.getVariantManager().getWorkingVariantId();
        return String.format("%s/%s-%s/", basePath, ++variantCounter, variantName);
    }
}
