/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@ConfigurationProperties(prefix = "cse-cc-runner.mendrisio")
@ConstructorBinding
public class MendrisioConfiguration {
    private final MendrisioCagnoLineProperties mendrisioCagnoLine;
    private final String mendrisioPstId;
    private final String mendrisioNodeId;

    public MendrisioConfiguration(MendrisioCagnoLineProperties mendrisioCagnoLine, String mendrisioPstId, String mendrisioNodeId) {
        this.mendrisioCagnoLine = mendrisioCagnoLine;
        this.mendrisioPstId = mendrisioPstId;
        this.mendrisioNodeId = mendrisioNodeId;
    }

    public MendrisioCagnoLineProperties getMendrisioCagnoLine() {
        return mendrisioCagnoLine;
    }

    public String getMendrisioPstId() {
        return mendrisioPstId;
    }

    public String getMendrisioNodeId() {
        return mendrisioNodeId;
    }

    public static final class MendrisioCagnoLineProperties {
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
}
