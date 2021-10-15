/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_cse.api.exception;

/**
 * Custom abstract exception to be extended by all application exceptions.
 * Any subclass may be automatically wrapped to a JSON API error message if needed
 *
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public abstract class AbstractCseException extends RuntimeException  {
    protected AbstractCseException(String message) {
        super(message);
    }

    protected AbstractCseException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public abstract int getStatus();

    public abstract String getCode();

    public final String getTitle() {
        return getMessage();
    }

    public final String getDetails() {
        String message = getMessage();
        Throwable cause = getCause();
        if (cause == null) {
            return message;
        }
        StringBuilder sb = new StringBuilder(64);
        if (message != null) {
            sb.append(message).append("; ");
        }
        sb.append("nested exception is ").append(cause);
        return sb.toString();
    }
}
