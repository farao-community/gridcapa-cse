/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.configurations;

import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PisaLinkProperties {
    private final String nodeFr;
    private final String nodeIt;
    private final List<String> fictiveLines;
    private final String praName;

    public PisaLinkProperties(String nodeFr, String nodeIt, List<String> fictiveLines, String praName) {
        this.nodeFr = nodeFr;
        this.nodeIt = nodeIt;
        this.fictiveLines = fictiveLines;
        this.praName = praName;
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

    public String getPraName() {
        return praName;
    }
}
