/*
 *
 *  * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.cse.runner.app.dichotomy;

import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.network.NetworkValidationResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RaoResponseValidation implements NetworkValidationResult<RaoResponse> {
    private final RaoResult raoResult;
    private final RaoResponse raoResponse;

    public RaoResponseValidation(RaoResult raoResult, RaoResponse raoResponse) {
        this.raoResult = raoResult;
        this.raoResponse = raoResponse;
    }

    @Override
    public RaoResult getRaoResult() {
        return raoResult;
    }

    @Override
    public RaoResponse getValidationData() {
        return raoResponse;
    }
}
