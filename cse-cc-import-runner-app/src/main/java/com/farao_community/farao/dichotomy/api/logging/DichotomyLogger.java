/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.dichotomy.api.logging;

/**
 * An interface for custom logging
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface DichotomyLogger {
    void trace(String format, Object... arguments);

    void info(String format, Object... arguments);

    void warn(String format, Object... arguments);

    void error(String format, Object... arguments);

    void debug(String format, Object... arguments);

    boolean isInfoEnabled();
}
