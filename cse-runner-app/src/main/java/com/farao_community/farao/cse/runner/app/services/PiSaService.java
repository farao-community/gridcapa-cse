/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.services;

import com.farao_community.farao.cse.runner.app.configurations.PiSaConfiguration;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class PiSaService {

    private final PiSaConfiguration piSaConfiguration;

    public PiSaService(PiSaConfiguration piSaConfiguration) {
        this.piSaConfiguration = piSaConfiguration;
    }

    public void alignFictiveGenerators(Network network) {
        Generator piSaGeneratorLink1NodeFr = getGenerator(network, piSaConfiguration.getPiSaLink1NodeFr());
        Generator piSaGeneratorLink1NodeIt = getGenerator(network, piSaConfiguration.getPiSaLink1NodeIt());
        Generator piSaGeneratorLink2NodeFr = getGenerator(network, piSaConfiguration.getPiSaLink2NodeFr());
        Generator piSaGeneratorLink2NodeIt = getGenerator(network, piSaConfiguration.getPiSaLink2NodeIt());

        adjustGenerators(piSaGeneratorLink1NodeFr, piSaGeneratorLink1NodeIt);
        adjustGenerators(piSaGeneratorLink2NodeFr, piSaGeneratorLink2NodeIt);
    }

    static Generator getGenerator(Network network, String nodeId) {
        return network.getGenerator(nodeId + "_generator");
    }

    /**
     * Put both generators at same absolute value of target P but opposite sign. It is aligned on the highest
     * absolute value of target P.
     *
     * @param generator1: First generator to align.
     * @param generator2: Second generator to align.
     */
    static void adjustGenerators(Generator generator1, Generator generator2) {
        if (Math.abs(generator1.getTargetP()) > Math.abs(generator2.getTargetP())) {
            generator2.setTargetP(-generator1.getTargetP());
        } else {
            generator1.setTargetP(-generator2.getTargetP());
        }
    }
}
