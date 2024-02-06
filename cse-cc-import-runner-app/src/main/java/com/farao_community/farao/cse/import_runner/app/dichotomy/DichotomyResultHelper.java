/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.computation.BorderExchanges;
import com.farao_community.farao.cse.data.cnec.CnecUtil;
import com.farao_community.farao.cse.import_runner.app.services.FileImporter;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public final class DichotomyResultHelper {

    private final FileImporter fileImporter;

    public DichotomyResultHelper(FileImporter fileImporter) {
        this.fileImporter = fileImporter;
    }

    public String getLimitingElement(DichotomyResult<DichotomyRaoResponse> dichotomyResult) {
        DichotomyStepResult<DichotomyRaoResponse> highestValidStepResult = dichotomyResult.getHighestValidStep();
        Crac crac = fileImporter.importCracFromJson(highestValidStepResult.getValidationData()
            .getRaoResponse().getCracFileUrl());
        RaoResult raoResult = fileImporter.importRaoResult(highestValidStepResult.getValidationData()
            .getRaoResponse().getRaoResultFileUrl(), crac);
        FlowCnec worstCnec = CnecUtil.getWorstCnec(crac, raoResult);
        return worstCnec.getName();
    }

    public double computeLowestUnsecureItalianImport(DichotomyResult<DichotomyRaoResponse> dichotomyResult) {
        Network network = fileImporter.importNetwork(dichotomyResult.getLowestInvalidStep().getValidationData()
            .getRaoResponse().getNetworkWithPraFileUrl());
        return BorderExchanges.computeItalianImport(network);
    }

    public double computeHighestSecureItalianImport(DichotomyResult<DichotomyRaoResponse> dichotomyResult) {
        Network network = fileImporter.importNetwork(dichotomyResult.getHighestValidStep().getValidationData()
            .getRaoResponse().getNetworkWithPraFileUrl());
        return BorderExchanges.computeItalianImport(network);
    }
}
