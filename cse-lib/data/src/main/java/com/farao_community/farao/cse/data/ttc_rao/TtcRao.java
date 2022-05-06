/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.ttc_rao;

import com.farao_community.farao.cse.data.xsd.ttc_rao.*;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class TtcRao {

    private TtcRao() {
        // Should not be instantiated
    }

    public static CseRaoResult generate(OffsetDateTime timestamp, RaoResult raoResult) {
        CseRaoResult cseRaoResult = new CseRaoResult();
        addTime(cseRaoResult, timestamp.toString());

        if (raoResult.getFunctionalCost(OptimizationState.AFTER_CRA) <= 0) {
            addStatus(cseRaoResult, Status.SECURE);
        } else {
            addStatus(cseRaoResult, Status.UNSECURE);
        }

        return cseRaoResult;
    }

    public static CseRaoResult failed(OffsetDateTime timestamp) {
        CseRaoResult cseRaoResult = new CseRaoResult();
        addTime(cseRaoResult, timestamp.toString());
        addStatus(cseRaoResult, Status.FAILED);
        return cseRaoResult;
    }

    static void addTime(CseRaoResult ttcRao, String timestamp) {
        StringValue time = new StringValue();
        time.setV(timestamp);
        ttcRao.setTime(time);
    }

    static void addStatus(CseRaoResult ttcRao, Status status) {
        CseRaoResult.Status cseStatus = new CseRaoResult.Status();
        cseStatus.setV(status);
        ttcRao.setStatus(cseStatus);
    }

    static void addPreventiveResult(CseRaoResult cseRaoResult, PreventiveResult preventiveResult) {
        CseRaoResult.Results results;
        if (cseRaoResult.getResults() == null) {
            results = new CseRaoResult.Results();
        } else {
            results = cseRaoResult.getResults();
        }
        results.setPreventiveResult(preventiveResult);
        cseRaoResult.setResults(results);
    }

    static void addOutageResult(CseRaoResult cseRaoResult, List<OutageResult> outageResults) {
        CseRaoResult.Results results;
        if (cseRaoResult.getResults() == null) {
            results = new CseRaoResult.Results();
        } else {
            results = cseRaoResult.getResults();
        }
        outageResults.forEach(outageResult -> results.getOutageResult().add(outageResult));
        cseRaoResult.setResults(results);
    }

    static void addPreventiveMneLimitation(CseRaoResult cseRaoResult, CseRaoResult.LimitingElements.PreventiveMNELimitation mneLimitation) {
        CseRaoResult.LimitingElements limitingElements;
        if (cseRaoResult.getLimitingElements() == null) {
            limitingElements = new CseRaoResult.LimitingElements();
        } else {
            limitingElements = cseRaoResult.getLimitingElements();
        }
        limitingElements.getPreventiveMNELimitation().add(mneLimitation);
        cseRaoResult.setLimitingElements(limitingElements);
    }

    static void addAfterOutageMneLimitation(CseRaoResult cseRaoResult, CseRaoResult.LimitingElements.AfterOutageMNELimitation mneLimitation) {
        CseRaoResult.LimitingElements limitingElements;
        if (cseRaoResult.getLimitingElements() == null) {
            limitingElements = new CseRaoResult.LimitingElements();
        } else {
            limitingElements = cseRaoResult.getLimitingElements();
        }
        limitingElements.getAfterOutageMNELimitation().add(mneLimitation);
        cseRaoResult.setLimitingElements(limitingElements);
    }

    private static StringValue getStringValue(String str) {
        StringValue strValue = new StringValue();
        strValue.setV(str);
        return strValue;
    }

    private static IntValue getIntValue(int innt) {
        IntValue intValue = new IntValue();
        intValue.setV(innt);
        return intValue;
    }

    private static IValue getIValue(int i) {
        IValue iValue = new IValue();
        iValue.setV(i);
        iValue.setUnit("A");
        return iValue;
    }

    static Branch getBranch(String fromNode, String toNode, int code) {
        Branch branch = new Branch();
        branch.setFromNode(getStringValue(fromNode));
        branch.setToNode(getStringValue(toNode));
        branch.setCode(getStringValue(String.valueOf(code)));
        return branch;
    }

    static Outage getOutage(String name, Branch... branches) {
        Outage outage = new Outage();
        outage.setName(name);
        for (Branch branch : branches) {
            outage.getBranch().add(branch);
        }
        return outage;
    }

    static Action getNetworkAction(String name) {
        Action action = new Action();
        action.setName(name);
        return action;
    }

    static Action getPstAction(String name, int pstTap) {
        Action action = new Action();
        action.setName(name);
        action.setPSTtap(getIntValue(pstTap));
        return action;
    }

    static Action getHvdcAction(String name, int setpoint) {
        Action action = new Action();
        action.setName(name);
        action.setSetpoint(getIntValue(setpoint));
        return action;
    }

    private static void copyBranchProperties(PreventiveBranchResult preventiveBranchResult, Branch branch) {
        preventiveBranchResult.setFromNode(branch.getFromNode());
        preventiveBranchResult.setToNode(branch.getToNode());
        preventiveBranchResult.setCode(branch.getCode());
    }

    private static void copyBranchProperties(AfterOutageBranchResult afterOutageBranchResult, Branch branch) {
        afterOutageBranchResult.setFromNode(branch.getFromNode());
        afterOutageBranchResult.setToNode(branch.getToNode());
        afterOutageBranchResult.setCode(branch.getCode());
    }

    static PreventiveBranchResult getPreventiveBranchResult(String name, Branch branch, int iMax, int iBeforeOptimization, int iAfterOptimization) {
        PreventiveBranchResult preventiveBranchResult = new PreventiveBranchResult();
        preventiveBranchResult.setName(name);
        copyBranchProperties(preventiveBranchResult, branch);
        preventiveBranchResult.setIMax(getIValue(iMax));
        preventiveBranchResult.setIBeforeOptimization(getIValue(iBeforeOptimization));
        preventiveBranchResult.setIAfterOptimization(getIValue(iAfterOptimization));
        return preventiveBranchResult;
    }

    static AfterOutageBranchResult getAfterOutageBranchResult(String name, Branch branch, int iAfterOutageBeforeOptimization, int iMaxAfterOutage, int iAfterOutageAfterOptimization, int iMaxAfterCRA, int iAfterCRAAfterOptimization) {
        AfterOutageBranchResult afterOutageBranchResult = new AfterOutageBranchResult();
        afterOutageBranchResult.setName(name);
        copyBranchProperties(afterOutageBranchResult, branch);
        afterOutageBranchResult.setIAfterOutageBeforeOptimization(getIValue(iAfterOutageBeforeOptimization));
        afterOutageBranchResult.setIMaxAfterOutage(getIValue(iMaxAfterOutage));
        afterOutageBranchResult.setIAfterOutageAfterOptimization(getIValue(iAfterOutageAfterOptimization));
        afterOutageBranchResult.setIAfterCRAAfterOptimization(getIValue(iAfterCRAAfterOptimization));
        afterOutageBranchResult.setIMaxAfterCRA(getIValue(iMaxAfterOutage));
        return afterOutageBranchResult;
    }

    static PreventiveResult getPreventiveResult(List<Action> preventiveActions, List<PreventiveBranchResult> preventiveBranchResults) {
        PreventiveResult preventiveResult = new PreventiveResult();
        PreventiveResult.PreventiveActions preventiveActions1 = new PreventiveResult.PreventiveActions();
        preventiveActions.forEach(preventiveAction -> preventiveActions1.getAction().add(preventiveAction));
        preventiveResult.setPreventiveActions(preventiveActions1);
        preventiveBranchResults.forEach(preventiveBranchResult -> preventiveResult.getMonitoredElement().add(preventiveBranchResult));
        return preventiveResult;
    }

    static OutageResult getOutageResult(Outage outage, List<Action> curativeActions, List<AfterOutageBranchResult> afterOutageBranchResults) {
        OutageResult outageResult = new OutageResult();
        outageResult.setOutage(outage);
        OutageResult.CurativeActions curativeActions1 = new OutageResult.CurativeActions();
        curativeActions.forEach(curativeAction -> curativeActions1.getAction().add(curativeAction));
        outageResult.setCurativeActions(curativeActions1);
        afterOutageBranchResults.forEach(afterOutageBranchResult -> outageResult.getMonitoredElement().add(afterOutageBranchResult));
        return outageResult;
    }

    static CseRaoResult.LimitingElements.PreventiveMNELimitation getPreventiveMneLimitation(List<PreventiveBranchResult> preventiveBranchResults) {
        CseRaoResult.LimitingElements.PreventiveMNELimitation mneLimitation = new CseRaoResult.LimitingElements.PreventiveMNELimitation();
        preventiveBranchResults.forEach(preventiveBranchResult -> mneLimitation.getMonitoredElement().add(preventiveBranchResult));
        return mneLimitation;
    }

    static CseRaoResult.LimitingElements.AfterOutageMNELimitation getAfterOutageMneLimitation(Outage outage, List<AfterOutageBranchResult> afterOutageBranchResults) {
        CseRaoResult.LimitingElements.AfterOutageMNELimitation mneLimitation = new CseRaoResult.LimitingElements.AfterOutageMNELimitation();
        mneLimitation.setOutage(outage);
        afterOutageBranchResults.forEach(afterOutageBranchResult -> mneLimitation.getMonitoredElement().add(afterOutageBranchResult));
        return mneLimitation;
    }
}
