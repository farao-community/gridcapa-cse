/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.configurations;

import com.farao_community.farao.cse.network_processing.pisa_change.PiSaLinkConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@ConfigurationProperties(prefix = "cse-cc-runner.pisa")
@ConstructorBinding
public class PiSaConfiguration {
    private final Link link1;
    private final Link link2;

    public PiSaConfiguration(Link link1, Link link2) {
        this.link1 = link1;
        this.link2 = link2;
    }

    public Link getLink1() {
        return link1;
    }

    public Link getLink2() {
        return link2;
    }

    public static final class Link {
        private final String nodeFr;
        private final String nodeIt;
        private final List<String> fictiveLines;

        public Link(String nodeFr, String nodeIt, List<String> fictiveLines) {
            this.nodeFr = nodeFr;
            this.nodeIt = nodeIt;
            this.fictiveLines = fictiveLines;
        }

        public String getNodeFr() {
            return nodeFr;
        }

        public String getNodeIt() {
            return nodeIt;
        }

        public List<String> getFictiveLines() {
            return fictiveLines;
        }
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
