/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
class CseInternalExceptionTest {

    @Test
    void checkException() {
        AbstractCseException cseException = new CseInternalException("Exception message");
        assertEquals("Exception message", cseException.getMessage());
        assertEquals(500, cseException.getStatus());

        Exception cause = new RuntimeException("Cause");
        AbstractCseException exception = new CseInternalException("Exception message", cause);
        assertEquals("Exception message", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
