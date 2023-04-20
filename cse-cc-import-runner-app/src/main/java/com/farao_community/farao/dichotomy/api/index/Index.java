/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.dichotomy.api.index;

import com.farao_community.farao.dichotomy.api.exceptions.DichotomyException;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.dichotomy.api.results.ReasonInvalid;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Object responsible of storing the dichotomy index state during dichotomy calculation.
 * It stores the results of previously validated steps.
 * An important assumption for Dichotomy engine is that minimum value is supposed to be secure
 * and maximum value is supposed to be unsecure. More precisely, if minimum value is unsecure, the algorithm
 * suppose that the dichotomy is finished (same if maximum value is secure).
 *
 * This implementation should be enough for any dichotomy usage.
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class Index<T> {
    private static final double EPSILON = 1e-3;
    private final double minValue;
    private final double maxValue;
    private final double precision;
    private final List<Pair<Double, DichotomyStepResult<T>>> stepResults = new ArrayList<>();
    private Pair<Double, DichotomyStepResult<T>> highestValidStep;
    private Pair<Double, DichotomyStepResult<T>> lowestInvalidStep;

    public Index(double minValue, double maxValue, double precision) {
        if (minValue > maxValue) {
            throw new DichotomyException("Index creation impossible, minValue is supposed to be lower than maxValue.");
        }
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.precision = precision;
    }

    public double minValue() {
        return minValue;
    }

    public double maxValue() {
        return maxValue;
    }

    public double precision() {
        return precision;
    }

    public Pair<Double, DichotomyStepResult<T>> highestValidStep() {
        return highestValidStep;
    }

    public Pair<Double, DichotomyStepResult<T>> lowestInvalidStep() {
        return lowestInvalidStep;
    }

    public List<Pair<Double, DichotomyStepResult<T>>> testedSteps() {
        return new ArrayList<>(stepResults);
    }

    public void addDichotomyStepResult(double stepValue, DichotomyStepResult<T> stepResult) {
        if (stepResult.isValid()) {
            if (highestValidStep != null && highestValidStep.getLeft() > stepValue) {
                throw new AssertionError("Step result tested is secure but its value is lower than highest secure step one. Should not happen");
            }
            highestValidStep = Pair.of(stepValue, stepResult);
        } else {
            if (lowestInvalidStep != null && lowestInvalidStep.getRight().getReasonInvalid().equals(ReasonInvalid.UNSECURE_AFTER_VALIDATION)
                && lowestInvalidStep.getLeft() < stepValue) {
                throw new AssertionError("Step result tested is unsecure but its value is higher than lowest unsecure step one. Should not happen");
            }
            lowestInvalidStep = Pair.of(stepValue, stepResult);
        }
        stepResults.add(Pair.of(stepValue, stepResult));
    }

}
