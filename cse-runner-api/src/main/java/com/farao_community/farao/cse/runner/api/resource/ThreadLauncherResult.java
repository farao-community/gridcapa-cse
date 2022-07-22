/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.api.resource;

import java.util.Optional;

public class ThreadLauncherResult<U> {

    private final Optional<U> result;
    private final boolean hasError;
    private final Exception exception;

    public ThreadLauncherResult(Optional<U> result, boolean hasError, Exception exception) {
        this.result = result;
        this.hasError = hasError;
        this.exception = exception;
    }

    public Optional<U> getResult() {
        return result;
    }

    public boolean hasError() {
        return hasError;
    }

    public Exception getException() {
        return exception;
    }
}
