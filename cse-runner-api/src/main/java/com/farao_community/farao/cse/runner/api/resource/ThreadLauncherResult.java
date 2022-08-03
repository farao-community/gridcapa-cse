/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.api.resource;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Optional;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class ThreadLauncherResult<U> {

    Optional<U> result;
    boolean error;
    Exception exception;

    public static <U> ThreadLauncherResult<U> success(U result) {
        return new ThreadLauncherResult<>(Optional.of(result), false, null);
    }

    public static <U> ThreadLauncherResult<U> interrupt() {
        return new ThreadLauncherResult<>(Optional.empty(), false, null);
    }

    public static <U> ThreadLauncherResult<U> error(Exception e) {
        return new ThreadLauncherResult<>(Optional.empty(), true, e);
    }

}
