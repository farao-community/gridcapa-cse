/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.dichotomy.api.results;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public enum ReasonInvalid {
    GLSK_LIMITATION,
    UNSECURE_AFTER_VALIDATION,
    VALIDATION_FAILED,
    NONE
}
