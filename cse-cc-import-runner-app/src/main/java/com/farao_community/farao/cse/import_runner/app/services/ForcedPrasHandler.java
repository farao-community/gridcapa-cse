/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

@Service
public class ForcedPrasHandler {

    private final Logger logger;

    public ForcedPrasHandler(Logger logger) {
        this.logger = logger;
    }

    /**
     * Method logs which of the remedial action list are either not present in the CRAC or not available to be applied.
     * With the remaining remedial action, it tries to apply them on the network and log if the application is
     * successful or not.
     *
     * @param forcedPrasIds: Set of PRAs to be applied on the network.
     * @param network: Network on which to apply these PRAs.
     * @param crac: CRAC on which to check definition and applicability.
     * @param flowResult: Flow result gathering power flows on the lines to evaluate RAs availability (flow constraint or country constraint)
     * @return The set of PRAs IDs that have been actually applied on the network
     */
    public Set<String> forcePras(Set<String> forcedPrasIds, Network network, Crac crac, FlowResult flowResult) {
        // Filters out those that are not present in the CRAC or that are not available
        return forcedPrasIds.stream()
            .filter(naId -> {
                if (crac.getNetworkAction(naId) == null) {
                    logger.info(String.format("Forced PRA %s is not defined in the CRAC as a network action", naId));
                    return false;
                }
                return true;
            })
            .map(crac::getNetworkAction)
            .filter(na -> {
                if (!RaoUtil.isRemedialActionAvailable(na, crac.getPreventiveState(), flowResult, crac.getFlowCnecs(crac.getPreventiveState()), network)) {
                    logger.info(String.format("Forced PRA %s is not available. It won't be applied.", na));
                    return false;
                }
                return true;
            })
            .<Optional<String>>map(networkAction -> {
                boolean applySuccess = networkAction.apply(network);
                if (applySuccess) {
                    logger.info("Network action {} has been forced", networkAction.getId());
                    return Optional.of(networkAction.getId());
                } else {
                    logger.warn("Network action {} will not be forced because not available", networkAction.getId());
                    return Optional.empty();
                }
            })
            .flatMap(Optional::stream)
            .collect(Collectors.toSet());
    }
}
