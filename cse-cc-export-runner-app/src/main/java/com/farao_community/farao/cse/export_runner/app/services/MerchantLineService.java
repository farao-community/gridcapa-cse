/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.export_runner.app.configurations.MendrisioConfiguration;
import com.farao_community.farao.cse.network_processing.ucte_pst_change.UctePstProcessor;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
public class MerchantLineService {

    private final UctePstProcessor uctePstProcessor;

    public MerchantLineService(MendrisioConfiguration mendrisioConfiguration) {

        this.uctePstProcessor = new UctePstProcessor(
                mendrisioConfiguration.getMendrisioPstId(),
                mendrisioConfiguration.getMendrisioNodeId());
    }

    public void setTransformerInActivePowerRegulation(Network network) {
        uctePstProcessor.setTransformerInActivePowerRegulation(network);

    }
}
