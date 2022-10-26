/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.network_processing.pisa_change;

import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PiSaLinkConfiguration {
    private final String piSaLinkFictiveNodeFr;
    private final String piSaLinkFictiveNodeIt;
    private final String piSaLinkPraName;
    private final List<String> piSaLinkFictiveLines;

    public PiSaLinkConfiguration(String piSaLinkFictiveNodeFr, String piSaLinkFictiveNodeIt, List<String> piSaLinkFictiveLines, String piSaLinkPraName) {
        this.piSaLinkFictiveNodeFr = piSaLinkFictiveNodeFr;
        this.piSaLinkFictiveNodeIt = piSaLinkFictiveNodeIt;
        this.piSaLinkFictiveLines = piSaLinkFictiveLines;
        this.piSaLinkPraName = piSaLinkPraName;
    }

    public String getPiSaLinkFictiveNodeFr() {
        return piSaLinkFictiveNodeFr;
    }

    public String getPiSaLinkFictiveNodeIt() {
        return piSaLinkFictiveNodeIt;
    }

    public List<String> getPiSaLinkFictiveLines() {
        return piSaLinkFictiveLines;
    }

    public String getPiSaLinkPraName() {
        return piSaLinkPraName;
    }
}
