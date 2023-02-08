/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.target_ch;

import com.farao_community.farao.cse.data.xsd.target_ch.TOutage;

class OutageInformation {
    private final String name;
    private final String fromNode;
    private final String toNode;
    private final String orderCode;
    private final double fixedFlow;

    OutageInformation(String name, String fromNode, String toNode, String orderCode, double fixedFlow) {
        this.name = name;
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.orderCode = orderCode;
        this.fixedFlow = fixedFlow;
    }

    OutageInformation(TOutage tOutage, double fixedFlow) {
        this(tOutage.getName(), tOutage.getNodeFrom(), tOutage.getNodeTo(), String.valueOf(tOutage.getOrder()), fixedFlow);
    }

    public OutageInformation(com.farao_community.farao.cse.data.xsd.target_ch_adapted.TOutage tOutageadapted, double fixedFlow) {
        this(tOutageadapted.getName(), tOutageadapted.getNodeFrom(), tOutageadapted.getNodeTo(), String.valueOf(tOutageadapted.getOrder()), fixedFlow);
    }

    String getName() {
        return name;
    }

    String getFromNode() {
        return fromNode;
    }

    String getToNode() {
        return toNode;
    }

    String getOrderCode() {
        return orderCode;
    }

    double getFixedFlow() {
        return fixedFlow;
    }
}
