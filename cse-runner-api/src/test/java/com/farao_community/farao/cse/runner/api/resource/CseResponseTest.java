/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.api.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
class CseResponseTest {

    @Test
    void checkCseResponse() {
        CseResponse cseResponse = new CseResponse("id", "ttcFileUrl");
        assertNotNull(cseResponse);
        assertEquals("id", cseResponse.getId());
        assertEquals("ttcFileUrl", cseResponse.getTtcFileUrl());
    }
}
