/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.network_processing.ucte_pst_change;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class UctePstException extends RuntimeException {

    public UctePstException(String message) {
        super(message);
    }
}
