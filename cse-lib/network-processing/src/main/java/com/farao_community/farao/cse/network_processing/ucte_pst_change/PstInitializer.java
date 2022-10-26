/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.network_processing.ucte_pst_change;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.range.TapRange;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class PstInitializer {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(PstInitializer.class);

    private final Logger logger;

    private PstInitializer(Logger logger) {
        this.logger = logger;
    }

    public static PstInitializer withLogger(Logger logger) {
        return new PstInitializer(logger);
    }

    public static PstInitializer withDefaultLogger() {
        return new PstInitializer(DEFAULT_LOGGER);
    }

    public Map<String, Integer> initializePsts(Network network, Crac crac) {
        Map<String, Integer> preprocessedPsts =  new HashMap<>();
        crac.getRangeActions(crac.getPreventiveState(), UsageMethod.AVAILABLE).stream()
            .filter(PstRangeAction.class::isInstance)
            .map(PstRangeAction.class::cast)
            .forEach(pstRangeAction -> {
                PhaseTapChanger ptc = network.getTwoWindingsTransformer(pstRangeAction.getNetworkElement().getId()).getPhaseTapChanger();
                if (!pstRangeAction.getRanges().stream().allMatch(r -> ptc.getTapPosition() >= r.getMinTap() && ptc.getTapPosition() <= r.getMaxTap())) {
                    int newTapPosition = pstRangeAction.getRanges().stream()
                        .map(TapRange::getMinTap)
                        .max(Integer::compareTo)
                        .orElse(pstRangeAction.getInitialTap());
                    preprocessedPsts.put(pstRangeAction.getId(), newTapPosition);
                    logger.warn("PST tap has been changed from {} to {} because it was initially out of CRAC ranges: {}",
                        ptc.getTapPosition(), newTapPosition, pstRangeAction.getNetworkElement().getId());
                    ptc.setTapPosition(newTapPosition);
                }
            });
        return preprocessedPsts;
    }
}

