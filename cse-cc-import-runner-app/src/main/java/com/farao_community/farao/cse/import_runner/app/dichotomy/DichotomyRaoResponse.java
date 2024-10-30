/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;

import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class DichotomyRaoResponse {

    private final RaoSuccessResponse raoResponse;
    private final Set<String> forcedPrasIds;

    public DichotomyRaoResponse(RaoSuccessResponse raoResponse, Set<String> forcedPrasIds) {
        this.raoResponse = raoResponse;
        this.forcedPrasIds = forcedPrasIds;
    }

    public RaoSuccessResponse getRaoResponse() {
        return raoResponse;
    }

    public Set<String> getForcedPrasIds() {
        return forcedPrasIds;
    }
}
