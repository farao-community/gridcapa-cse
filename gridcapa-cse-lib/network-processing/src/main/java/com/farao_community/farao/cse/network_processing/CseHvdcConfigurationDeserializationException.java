/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing;

/**
 * @author Philippe Edwards {@literal <philippe.edwards@rte-france.com>}
 */
public class CseHvdcConfigurationDeserializationException extends RuntimeException {
    public CseHvdcConfigurationDeserializationException(String message) {
        super(message);
    }

    public CseHvdcConfigurationDeserializationException(Throwable cause) {
        super(cause);
    }
}
