/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.configurations;

import com.farao_community.farao.cse.network_processing.pisa_change.PiSaLinkConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Bean;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@ConfigurationProperties(prefix = "cse-cc-runner.pisa")
@ConstructorBinding
public class PiSaConfiguration {
    private final PisaLinkProperties link1;
    private final PisaLinkProperties link2;

    public PiSaConfiguration(PisaLinkProperties link1, PisaLinkProperties link2) {
        this.link1 = link1;
        this.link2 = link2;
    }

    public PisaLinkProperties getLink1() {
        return link1;
    }

    public PisaLinkProperties getLink2() {
        return link2;
    }

    @Bean(name = "piSaLink1Configuration")
    public PiSaLinkConfiguration getPiSaLink1Configuration() {
        return new PiSaLinkConfiguration(
                getLink1().getNodeFr(),
                getLink1().getNodeIt(),
                getLink1().getFictiveLines());
    }

    @Bean(name = "piSaLink2Configuration")
    public PiSaLinkConfiguration getPiSaLink2Configuration() {
        return new PiSaLinkConfiguration(
                getLink2().getNodeFr(),
                getLink2().getNodeIt(),
                getLink2().getFictiveLines());
    }
}
