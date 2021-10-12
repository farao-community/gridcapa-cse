/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_cse.api.exception;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public class CseInternalException extends AbstractCseException {
    private static final int STATUS = 500;
    private static final String CODE = "500-Cse-Internal-Server-Exception";

    public CseInternalException(String message) {
        super(message);
    }

    public CseInternalException(String message, Throwable throwable) {
        super(message, throwable);
    }

    @Override
    public int getStatus() {
        return STATUS;
    }

    @Override
    public String getCode() {
        return CODE;
    }
}
