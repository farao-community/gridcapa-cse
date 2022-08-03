/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.configurations;

import com.farao_community.farao.cse.network_processing.pisa_change.PiSaLinkConfiguration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Bean;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@ConfigurationProperties(prefix = "cse-cc-runner.pisa")
@ConstructorBinding
@Data
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class PiSaConfiguration {
    PisaLinkProperties link1;
    PisaLinkProperties link2;

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
