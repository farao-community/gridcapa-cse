/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.configurations;

import com.farao_community.farao.cse.network_processing.pisa_change.PiSaLinkConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@ConfigurationProperties(prefix = "cse-cc-runner.pisa")
public record PiSaConfiguration(List<String> alignedRaNames,
                                PisaLinkProperties link1,
                                PisaLinkProperties link2) {

    @Bean(name = "piSaLink1Configuration")
    public PiSaLinkConfiguration getPiSaLink1Configuration() {
        return new PiSaLinkConfiguration(
            link1().nodeFr(),
            link1().nodeIt(),
            link1().fictiveLines(),
            link1().praName()
        );
    }

    @Bean(name = "piSaLink2Configuration")
    public PiSaLinkConfiguration getPiSaLink2Configuration() {
        return new PiSaLinkConfiguration(
            link2().nodeFr(),
            link2().nodeIt(),
            link2().fictiveLines(),
            link2().praName()
        );
    }
}
