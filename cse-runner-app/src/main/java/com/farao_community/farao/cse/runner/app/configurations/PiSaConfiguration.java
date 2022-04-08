/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.configurations;

import com.farao_community.farao.cse.network_processing.pisa_change.PiSaLinkConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@ConfigurationProperties("cse-cc-runner.pisa")
public class PiSaConfiguration {

    private Link link1;
    private Link link2;

    public Link getLink1() {
        return link1;
    }

    public Link getLink2() {
        return link2;
    }

    public void setLink1(Link link1) {
        this.link1 = link1;
    }

    public void setLink2(Link link2) {
        this.link2 = link2;
    }

    private static class Link {
        private String nodeFr;
        private String nodeIt;
        private List<String> fictiveLines;

        public String getNodeFr() {
            return nodeFr;
        }

        public String getNodeIt() {
            return nodeIt;
        }

        public List<String> getFictiveLines() {
            return fictiveLines;
        }

        public void setNodeFr(String nodeFr) {
            this.nodeFr = nodeFr;
        }

        public void setNodeIt(String nodeIt) {
            this.nodeIt = nodeIt;
        }

        public void setFictiveLines(List<String> fictiveLines) {
            this.fictiveLines = fictiveLines;
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
