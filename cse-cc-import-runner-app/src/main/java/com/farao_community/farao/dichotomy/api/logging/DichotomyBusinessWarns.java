/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.dichotomy.api.logging;

/**
 * Dichotomy business warnings, containing important information understandable by the end user.
 * Only WARN level is allowed
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class DichotomyBusinessWarns extends AbstractDichotomyLogger {
    private static final String LOG_LEVEL_NOT_ALLOWED = "Log level not allowed in this logger";

    public DichotomyBusinessWarns() {
        // nothing to do
    }

    @Override
    public void trace(String format, Object... arguments) {
        throw new IllegalCallerException(LOG_LEVEL_NOT_ALLOWED);
    }

    @Override
    public void info(String format, Object... arguments) {
        throw new IllegalCallerException(LOG_LEVEL_NOT_ALLOWED);
    }

    @Override
    public void error(String format, Object... arguments) {
        throw new IllegalCallerException(LOG_LEVEL_NOT_ALLOWED);
    }

    @Override
    public void debug(String format, Object... arguments) {
        throw new IllegalCallerException(LOG_LEVEL_NOT_ALLOWED);
    }
}
