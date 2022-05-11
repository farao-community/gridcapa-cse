/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.network_processing.pisa_change.PiSaLinkConfiguration;
import com.farao_community.farao.cse.network_processing.pisa_change.PiSaLinkProcessor;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class PiSaService {
    private final PiSaLinkProcessor piSaLink1Processor;
    private final PiSaLinkProcessor piSaLink2Processor;

    public PiSaService(PiSaLinkConfiguration piSaLink1Configuration, PiSaLinkConfiguration piSaLink2Configuration) {
        this.piSaLink1Processor = new PiSaLinkProcessor(piSaLink1Configuration);
        this.piSaLink2Processor = new PiSaLinkProcessor(piSaLink2Configuration);
    }

    PiSaLinkProcessor getPiSaLink1Processor() {
        return piSaLink1Processor;
    }

    PiSaLinkProcessor getPiSaLink2Processor() {
        return piSaLink2Processor;
    }

    public void alignGenerators(Network network) {
        alignGenerators(network, piSaLink1Processor);
        alignGenerators(network, piSaLink2Processor);
    }

    static void alignGenerators(Network network, PiSaLinkProcessor piSaLinkProcessor) {
        if (piSaLinkProcessor.isLinkPresent(network) && piSaLinkProcessor.isLinkConnected(network)) {
            piSaLinkProcessor.alignFictiveGenerators(network);
        }
    }
}
