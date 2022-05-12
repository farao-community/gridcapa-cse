/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.ttc_rao;

import com.farao_community.farao.cse.data.xsd.ttc_rao.*;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;

import java.time.OffsetDateTime;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class TtcRao {

    private TtcRao() {
        // Should not be instantiated
    }

    public static CseRaoResult generate(OffsetDateTime timestamp, RaoResult raoResult) {
        CseRaoResult cseRaoResult = new CseRaoResult();
        addTime(cseRaoResult, timestamp.toString());

        if (raoResult.getFunctionalCost(OptimizationState.AFTER_CRA) <= 0) {
            addStatus(cseRaoResult, Status.SECURE);
        } else {
            addStatus(cseRaoResult, Status.UNSECURE);
        }

        return cseRaoResult;
    }

    public static CseRaoResult failed(OffsetDateTime timestamp) {
        CseRaoResult cseRaoResult = new CseRaoResult();
        addTime(cseRaoResult, timestamp.toString());
        addStatus(cseRaoResult, Status.FAILED);
        return cseRaoResult;
    }

    static void addTime(CseRaoResult ttcRao, String timestamp) {
        StringValue time = new StringValue();
        time.setV(timestamp);
        ttcRao.setTime(time);
    }

    static void addStatus(CseRaoResult ttcRao, Status status) {
        CseRaoResult.Status cseStatus = new CseRaoResult.Status();
        cseStatus.setV(status);
        ttcRao.setStatus(cseStatus);
    }
}
