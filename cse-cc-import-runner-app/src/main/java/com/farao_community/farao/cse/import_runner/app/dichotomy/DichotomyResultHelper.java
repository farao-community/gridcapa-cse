/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.cse.computation.BorderExchanges;
import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.import_runner.app.services.FileImporter;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public final class DichotomyResultHelper {

    private final FileImporter fileImporter;

    public DichotomyResultHelper(FileImporter fileImporter) {
        this.fileImporter = fileImporter;
    }

    public String getLimitingElement(DichotomyResult<DichotomyRaoResponse> dichotomyResult) throws IOException {
        DichotomyStepResult<DichotomyRaoResponse> highestValidStepResult = dichotomyResult.getHighestValidStep();
        double worstMargin = Double.MAX_VALUE;
        Optional<FlowCnec> worstCnec = Optional.empty();
        Crac crac = fileImporter.importCracFromJson(highestValidStepResult.getValidationData()
            .getRaoResponse().getCracFileUrl());
        RaoResult raoResult = fileImporter.importRaoResult(highestValidStepResult.getValidationData()
            .getRaoResponse().getRaoResultFileUrl(), crac);
        for (FlowCnec flowCnec : crac.getFlowCnecs()) {
            double margin = computeFlowMargin(raoResult, flowCnec);
            if (margin < worstMargin) {
                worstMargin = margin;
                worstCnec = Optional.of(flowCnec);
            }
        }

        return worstCnec.orElseThrow(() -> new CseDataException("Exception occurred while retrieving the most limiting element in preventive state.")).getName();
    }

    private static double computeFlowMargin(RaoResult raoResult, FlowCnec flowCnec) {
        if (flowCnec.getState().getInstant() == Instant.CURATIVE) {
            return raoResult.getMargin(OptimizationState.AFTER_CRA, flowCnec, Unit.AMPERE);
        } else {
            return raoResult.getMargin(OptimizationState.AFTER_PRA, flowCnec, Unit.AMPERE);
        }
    }

    public double computeLowestUnsecureItalianImport(DichotomyResult<DichotomyRaoResponse> dichotomyResult) throws IOException {
        Network network = fileImporter.importNetwork(dichotomyResult.getLowestInvalidStep().getValidationData()
            .getRaoResponse().getNetworkWithPraFileUrl());
        return BorderExchanges.computeItalianImport(network);
    }

    public double computeHighestSecureItalianImport(DichotomyResult<DichotomyRaoResponse> dichotomyResult) throws IOException {
        Network network = fileImporter.importNetwork(dichotomyResult.getHighestValidStep().getValidationData()
            .getRaoResponse().getNetworkWithPraFileUrl());
        return BorderExchanges.computeItalianImport(network);
    }
}
