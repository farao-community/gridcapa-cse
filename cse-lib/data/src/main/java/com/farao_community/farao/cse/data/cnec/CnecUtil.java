/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.cnec;

import com.powsybl.openrao.commons.Unit;
import com.farao_community.farao.cse.data.CseDataException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public final class CnecUtil {

    private CnecUtil() {
    }

    public static FlowCnec getWorstCnec(Crac crac, RaoResult raoResult) {
        double worstMargin = Double.MAX_VALUE;
        Optional<FlowCnec> worstCnec = Optional.empty();
        Set<FlowCnec> optimizedFlowCnecs = crac.getFlowCnecs().stream().filter(Cnec::isOptimized).collect(Collectors.toSet());
        for (FlowCnec flowCnec : optimizedFlowCnecs) {
            double margin = computeFlowMargin(flowCnec, raoResult);
            if (margin < worstMargin) {
                worstMargin = margin;
                worstCnec = Optional.of(flowCnec);
            }
        }
        return worstCnec.orElseThrow(() -> new CseDataException("Exception occurred while retrieving the most limiting element."));
    }

    private static double computeFlowMargin(FlowCnec flowCnec, RaoResult raoResult) {
        return raoResult.getMargin(flowCnec.getState().getInstant(), flowCnec, Unit.AMPERE);
    }

}
