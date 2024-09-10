/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.import_runner.app.services.FileExporter;
import com.farao_community.farao.cse.import_runner.app.services.FileImporter;
import com.farao_community.farao.cse.import_runner.app.services.ForcedPrasHandler;
import com.farao_community.farao.cse.import_runner.app.util.FlowEvaluator;
import com.farao_community.farao.cse.import_runner.app.util.MinioStorageHelper;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.dichotomy.api.exceptions.RaoInterruptionException;
import com.farao_community.farao.minio_adapter.starter.GridcapaFileGroup;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.exceptions.ValidationException;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public class RaoRunnerValidator implements NetworkValidator<DichotomyRaoResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaoRunnerValidator.class);

    private static final String UCTE_EXTENSION = "uct";
    private static final String UCTE_FORMAT = "UCTE";
    private static final String XIIDM_EXTENSION = "xiidm";
    private final ProcessType processType;
    private final String requestId;
    private final String currentRunId;
    private final OffsetDateTime processTargetDateTime;
    private final String cracUrl;
    private final String raoParametersUrl;
    private final RaoRunnerClient raoRunnerClient;
    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final ForcedPrasHandler forcedPrasHandler;
    private final Set<String> forcedPrasIds;
    private final boolean isImportEcProcess;
    private int variantCounter = 0;

    public RaoRunnerValidator(CseRequest cseRequest,
                              String cracUrl,
                              String raoParametersUrl,
                              RaoRunnerClient raoRunnerClient,
                              FileExporter fileExporter,
                              FileImporter fileImporter,
                              ForcedPrasHandler forcedPrasHandler,
                              Set<String> forcedPrasIds,
                              boolean isImportEcProcess) {
        this.processType = cseRequest.getProcessType();
        this.requestId = cseRequest.getId();
        this.currentRunId = cseRequest.getCurrentRunId();
        this.processTargetDateTime = cseRequest.getTargetProcessDateTime();
        this.cracUrl = cracUrl;
        this.raoParametersUrl = raoParametersUrl;
        this.raoRunnerClient = raoRunnerClient;
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.forcedPrasHandler = forcedPrasHandler;
        this.forcedPrasIds = forcedPrasIds;
        this.isImportEcProcess = isImportEcProcess;
    }

    @Override
    public DichotomyStepResult<DichotomyRaoResponse> validateNetwork(Network network, DichotomyStepResult lastDichotomyStepResult) throws ValidationException, RaoInterruptionException {
        String baseDirPathForCurrentStep = generateBaseDirPathFromScaledNetwork(network);
        String scaledNetworkName = network.getNameOrId();
        String scaledNetworkInXiidmFormatName = scaledNetworkName + "." + XIIDM_EXTENSION;
        String networkPresignedUrl = fileExporter.saveNetworkInArtifact(network, baseDirPathForCurrentStep + scaledNetworkInXiidmFormatName, "", processTargetDateTime, processType, isImportEcProcess);
        // coreso request to export to Minio the intermediate network in the  UCTE format
        String scaledNetworkInUcteFormatName = scaledNetworkName + "." + UCTE_EXTENSION;
        fileExporter.exportAndUploadNetwork(network, UCTE_FORMAT, GridcapaFileGroup.ARTIFACT, baseDirPathForCurrentStep + scaledNetworkInUcteFormatName, "", processTargetDateTime, processType, isImportEcProcess);
        try {
            Crac crac = fileImporter.importCracFromJson(cracUrl, network);
            List<String> appliedRemedialActionInPreviousStep = lastDichotomyStepResult != null && lastDichotomyStepResult.getRaoResult() != null ? lastDichotomyStepResult.getRaoResult().getActivatedNetworkActionsDuringState(crac.getPreventiveState())
                     .stream().map(NetworkAction::getId).toList() : Collections.emptyList();
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
            if (raoResponse.isInterrupted()) {
                throw new RaoInterruptionException("RAO has been interrupted");
            }
            RaoResult raoResult = fileImporter.importRaoResult(raoResponse.getRaoResultFileUrl(), crac);

            DichotomyRaoResponse dichotomyRaoResponse = new DichotomyRaoResponse(raoResponse, appliedForcedPras);
            return DichotomyStepResult.fromNetworkValidationResult(raoResult, dichotomyRaoResponse);
        } catch (RuntimeException e) {
            String errorMessage = "Exception occurred during validation. Nested exception: {}" + e.getMessage();
            LOGGER.error(errorMessage);
            throw new ValidationException("RAO run failed", e);
        }
    }

    private Set<String> applyForcedPras(Crac crac, Network network) {
        if (!forcedPrasIds.isEmpty()) {
            Unit unit = fileExporter.loadRaoParameters().getObjectiveFunctionParameters().getType().getUnit();
            // It computes reference flows on the network to be able to evaluate PRAs availability
            FlowResult flowResult = FlowEvaluator.evaluate(crac, network, unit);

            // We get only the RAs that have been actually applied
            // Even if the set is empty we still do the computation, in worst case scenario the computation is useless
            return forcedPrasHandler.forcePras(forcedPrasIds, network, crac, flowResult, fileExporter.loadRaoParameters());
        } else {
            return Collections.emptySet();
        }
    }

    public RaoRequest buildRaoRequest(String networkPreSignedUrl, String baseDirPathForCurrentStep, List<String> appliedRemedialActionInPreviousStep) {
        RaoRequest.RaoRequestBuilder builder = new RaoRequest.RaoRequestBuilder()
                .withId(requestId)
                .withRunId(currentRunId)
                .withNetworkFileUrl(networkPreSignedUrl)
                .withCracFileUrl(cracUrl)
                .withResultsDestination(baseDirPathForCurrentStep);

        if (appliedRemedialActionInPreviousStep.size() == 1) {
            builder.withRaoParametersFileUrl(this.raoParametersUrl);
        } else {
            String raoParametersWithAppliedRemedialActionInPreviousStepUrl = saveRaoParametersIfNeeded(baseDirPathForCurrentStep, appliedRemedialActionInPreviousStep);
            builder.withRaoParametersFileUrl(raoParametersWithAppliedRemedialActionInPreviousStepUrl);
        }

        return builder.build();
    }

    private String saveRaoParametersIfNeeded(String baseDirPathForCurrentStep, List<String> appliedRemedialActionInPreviousStep) {
        return fileExporter.saveRaoParameters(baseDirPathForCurrentStep, appliedRemedialActionInPreviousStep, processTargetDateTime, processType, isImportEcProcess);
    }

    private String generateBaseDirPathFromScaledNetwork(Network network) {
        String basePath = MinioStorageHelper.makeDestinationMinioPath(processTargetDateTime, processType, MinioStorageHelper.FileKind.ARTIFACTS, ZoneId.of(fileExporter.getZoneId()), isImportEcProcess);
        String variantName = network.getVariantManager().getWorkingVariantId();
        return String.format("%s/%s-%s/", basePath, ++variantCounter, variantName);
    }
}
