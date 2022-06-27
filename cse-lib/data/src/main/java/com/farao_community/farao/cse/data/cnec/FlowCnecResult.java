/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.cnec;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowCnecResult {
    private final double flow;
    private final double iMax;

    public FlowCnecResult(double flow, double iMax) {
        this.flow = flow;
        this.iMax = iMax;
    }

    public double getFlow() {
        return flow;
    }

    public double getiMax() {
        return iMax;
    }
}
