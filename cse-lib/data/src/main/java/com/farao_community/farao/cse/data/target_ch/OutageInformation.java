/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.target_ch;

import com.farao_community.farao.cse.data.xsd.target_ch.TOutage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class OutageInformation {
    String name;
    String fromNode;
    String toNode;
    String orderCode;
    double fixedFlow;

    OutageInformation(TOutage tOutage, double fixedFlow) {
        this(tOutage.getName(), tOutage.getNodeFrom(), tOutage.getNodeTo(), String.valueOf(tOutage.getOrder()), fixedFlow);
    }
}
