/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.dichotomy.api.index;

/**
 * Implementation of IndexStrategy that consists of a basic dichotomy between minimum index value and maximum one.
 * First, it will validate minimum and maximum value (depending on startWitnMin value) and then validate recursively
 * the middle of the dichotomy interval.
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class RangeDivisionIndexStrategy implements IndexStrategy {
    private final boolean startWithMin;

    public RangeDivisionIndexStrategy(boolean startWithMin) {
        this.startWithMin = startWithMin;
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
                return index.maxValue();
            }
        } else {
            if (index.lowestInvalidStep() == null) {
                return index.maxValue();
            }
            if (index.highestValidStep() == null) {
                return index.minValue();
            }
        }
        return (index.lowestInvalidStep().getLeft() + index.highestValidStep().getLeft()) / 2;
    }
}
