/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.configurations;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MendrisioCagnoLineProperties {
    private final String targetChId;
    private final String ntcId;

    public MendrisioCagnoLineProperties(String targetChId, String ntcId) {
        this.targetChId = targetChId;
        this.ntcId = ntcId;
    }

    public String getTargetChId() {
        return targetChId;
    }

    public String getNtcId() {
        return ntcId;
    }
}
