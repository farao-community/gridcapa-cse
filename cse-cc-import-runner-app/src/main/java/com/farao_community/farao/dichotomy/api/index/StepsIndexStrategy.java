/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.dichotomy.api.index;

import com.farao_community.farao.dichotomy.api.exceptions.DichotomyException;

/**
 * Implementation of IndexStrategy that consist of increasing (or decreasing) checked value through steps until an actual.
 * research interval is found.
 *
 * First, it will validate minimum value (respectively maximum value) and then validate by ascending steps (respectively
 * descending steps) until an unsecure (respectively secure) index value is found. Then, an algorithm similar to the
 * RangeDivisionIndexStrategy one is used to reach the expected precision.
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class StepsIndexStrategy implements IndexStrategy {
    private final boolean startWithMin;
    private final double increaseValueBeforeDichotomy;

    public StepsIndexStrategy(boolean startWithMin, double stepsSize) {
        if (stepsSize <= 0) {
            throw new DichotomyException("Steps size should be positive");
        }
        this.startWithMin = startWithMin;
        this.increaseValueBeforeDichotomy = stepsSize;
    }

    @Override
    public double nextValue(Index<?> index) {
        if (precisionReached(index)) {
            throw new AssertionError("Dichotomy engine should not ask for next value if precision is reached");
        }

        if (startWithMin) {
            if (index.highestValidStep() == null) {
                return index.minValue();
            }
            if (index.lowestInvalidStep() == null) {
                return Math.min(index.highestValidStep().getLeft() + increaseValueBeforeDichotomy, index.maxValue());
            }
        } else {
            if (index.lowestInvalidStep() == null) {
                return index.maxValue();
            }
            if (index.highestValidStep() == null) {
                return Math.max(index.lowestInvalidStep().getLeft() - increaseValueBeforeDichotomy, index.minValue());
            }
        }
        return (index.lowestInvalidStep().getLeft() + index.highestValidStep().getLeft()) / 2;
    }
}
