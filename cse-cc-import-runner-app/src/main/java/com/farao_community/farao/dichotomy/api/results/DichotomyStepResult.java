/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.dichotomy.api.results;

import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class DichotomyStepResult<I> {
    private final boolean secure;
    private final RaoResult raoResult;
    private final I validationData;
    private final ReasonInvalid reasonInvalid;
    private final String failureMessage;

    private DichotomyStepResult(ReasonInvalid reasonInvalid, String failureMessage) {
        this.secure = false;
        this.raoResult = null;
        this.validationData = null;
        this.reasonInvalid = reasonInvalid;
        this.failureMessage = failureMessage;
    }

    private DichotomyStepResult(RaoResult raoResult, I validationData) {
        this.raoResult = raoResult;
        this.validationData = validationData;
        this.secure = raoResultIsSecure(raoResult);
        this.reasonInvalid = secure ? ReasonInvalid.NONE : ReasonInvalid.UNSECURE_AFTER_VALIDATION;
        this.failureMessage = "None";
    }

    /**
     * In case the network validation fails, we suppose that no {@link RaoResult} are available. To remain consistent
     * either {@code GLSK_LIMITATION} or {@code VALIDATION_FAILURE} must be used here in {@link ReasonInvalid} because
     * there are the two only failure reasons that could qualify the {@link DichotomyStepResult} within
     * {@link ReasonInvalid} enumeration.
     *
     * @param reasonInvalid: Qualify invalidity for failure, either GLSK_LIMITATION or VALIDATION_FAILURE
     * @param failureMessage: Additional information that could come from the original error or be created
     * @return An 'empty' {@link DichotomyStepResult} that contains only meta-information on invalidity reasons
     */
    public static <J> DichotomyStepResult<J> fromFailure(ReasonInvalid reasonInvalid,
                                                         String failureMessage) {
        return new DichotomyStepResult<>(reasonInvalid, failureMessage);
    }

    /**
     * In case network validation works properly there are only two wys out, either it is secured or unsecured.
     * According to that corresponding {@link ReasonInvalid} would be chosen: {@code NONE} for a secured network --
     * which is then valid -- and {@code UNSECURE_AFTER_VALIDATION} for an unsecured network.
     *
     * @return A network dichotomy step result containing validation data and meta-data about its security
     */
    public static <J> DichotomyStepResult<J> fromNetworkValidationResult(RaoResult raoResult,
                                                                         J validationData) {
        return new DichotomyStepResult<>(raoResult, validationData);
    }

    private static boolean raoResultIsSecure(RaoResult raoResult) {
        return raoResult.getFunctionalCost(OptimizationState.AFTER_CRA) <= 0;
    }

    public RaoResult getRaoResult() {
        return raoResult;
    }

    public I getValidationData() {
        return validationData;
    }

    public boolean isFailed() {
        return reasonInvalid == ReasonInvalid.GLSK_LIMITATION || reasonInvalid == ReasonInvalid.VALIDATION_FAILED;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public boolean isValid() {
        return reasonInvalid == ReasonInvalid.NONE;
    }

    public ReasonInvalid getReasonInvalid() {
        return reasonInvalid;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
