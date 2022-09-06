/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.util;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.impl.FlowResultImpl;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;

import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class FlowEvaluator {

    private FlowEvaluator() {
        // Should not be instantiated
    }

    public static FlowResult evaluate(Crac crac, Network network) {
        SystematicSensitivityInterface systematicSensitivityInterface = SystematicSensitivityInterface.builder()
            .withLoadflow(crac.getFlowCnecs(), Set.of(Unit.MEGAWATT))
            .withDefaultParameters(SensitivityAnalysisParameters.load())
            .withSensitivityProviderName(SensitivityAnalysis.find().getName())
            .build();
        SystematicSensitivityResult sensitivityResult = systematicSensitivityInterface.run(network);
        return new FlowResultImpl(sensitivityResult, null, null);
    }
}
