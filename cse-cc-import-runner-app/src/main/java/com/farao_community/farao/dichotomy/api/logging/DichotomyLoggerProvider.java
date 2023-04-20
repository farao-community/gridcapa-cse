/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.dichotomy.api.logging;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class DichotomyLoggerProvider {
    public static final DichotomyLogger BUSINESS_LOGS = new DichotomyBusinessLogs();
    public static final DichotomyLogger BUSINESS_WARNS = new DichotomyBusinessWarns();
    public static final DichotomyLogger TECHNICAL_LOGS = new TechnicalLogs();

    private DichotomyLoggerProvider() {
        // utility class
    }
}
