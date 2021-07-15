/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.ntc;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public class NtcInformation {
    private final String variationType;
    private final double flow;

    public NtcInformation(String variationType, double flow) {
        this.variationType = variationType;
        this.flow = flow;
    }

    public String getVariationType() {
        return variationType;
    }

    public double getFlow() {
        return flow;
    }
}
