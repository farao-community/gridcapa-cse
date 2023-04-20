/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.dichotomy.api.index;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class BiDirectionalStepsIndexStrategy implements IndexStrategy {
    private final double startIndex;
    private final double stepSize;

    public BiDirectionalStepsIndexStrategy(double startIndex, double stepSize) {
        this.startIndex = startIndex;
        this.stepSize = stepSize;
    }

    @Override
    public double nextValue(Index<?> index) {
        if (precisionReached(index)) {
            throw new AssertionError("Dichotomy engine should not ask for next value if precision is reached");
        }

        if (index.highestValidStep() == null && index.lowestInvalidStep() == null) {
            return startIndex;
        } else if (index.highestValidStep() == null) {
            return Math.max(index.minValue(), index.lowestInvalidStep().getLeft() - stepSize);
        } else if (index.lowestInvalidStep() == null) {
            return Math.min(index.maxValue(), index.highestValidStep().getLeft() + stepSize);
        } else {
            return (index.lowestInvalidStep().getLeft() + index.highestValidStep().getLeft()) / 2;
        }
    }
}
