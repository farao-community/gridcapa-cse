/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.ttc_rao;

import com.farao_community.farao.cse.data.cnec.CnecCommon;
import com.farao_community.farao.cse.data.cnec.CracResultsHelper;
import com.farao_community.farao.cse.data.cnec.CnecPreventive;
import com.farao_community.farao.cse.data.cnec.MergedCnec;
import com.farao_community.farao.cse.data.xsd.ttc_rao.*;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_creation.creator.api.ElementaryCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.RemedialActionCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.outage.CseOutageCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.remedial_action.CseHvdcCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.remedial_action.CsePstCreationContext;
import com.farao_community.farao.data.rao_result_api.OptimizationState;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class TtcRao {

    private TtcRao() {
        // Should not be instantiated
    }

    public static CseRaoResult generate(OffsetDateTime timestamp, CracResultsHelper cracResultsHelper) {
        CseRaoResult cseRaoResult = new CseRaoResult();
        addTime(cseRaoResult, timestamp.toString());

        if (cracResultsHelper.getRaoResult().getFunctionalCost(OptimizationState.AFTER_CRA) <= 0) {
            addStatus(cseRaoResult, Status.SECURE);
        } else {
            addStatus(cseRaoResult, Status.UNSECURE);
        }
        addResults(cseRaoResult, cracResultsHelper);
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

    private static void addResults(CseRaoResult cseRaoResult, CracResultsHelper cracResultsHelper) {
        CseRaoResult.Results results = new CseRaoResult.Results();
        List<Action> preventiveActions = getPreventiveActions(cracResultsHelper);
        List<PreventiveBranchResult> preventiveBranchResults = getPreventiveBranchResults(cracResultsHelper);
        PreventiveResult preventiveResult = getPreventiveResult(preventiveActions, preventiveBranchResults);
        List<OutageResult> outageResults = getOutageResults(cracResultsHelper);
        addPreventiveResult(results, preventiveResult);
        addOutageResult(results, outageResults);
        cseRaoResult.setResults(results);
    }

    private static List<OutageResult> getOutageResults(CracResultsHelper cracResultsHelper) {
        List<OutageResult> outageResultList = new ArrayList<>();
        List<CseOutageCreationContext> cseOutageCreationContexts = cracResultsHelper.getOutageCreationContext();
        cseOutageCreationContexts.forEach(cseOutageCreationContext -> {
            OutageResult outageResult = new OutageResult();
            Outage outage = new Outage();
            outage.setName(cseOutageCreationContext.getNativeId());
            outageResult.setOutage(outage);
            Contingency contingency = cracResultsHelper.getCrac().getContingency(cseOutageCreationContext.getCreatedContingencyId());
            contingency.getNetworkElements().forEach(contingencyNetworkElement -> {
                Branch branch = new Branch();
                branch.setCode(getStringValue(cracResultsHelper.getOrderCode(contingencyNetworkElement)));
                branch.setFromNode(getStringValue(cracResultsHelper.getNodeFrom(contingencyNetworkElement)));
                branch.setToNode(getStringValue(cracResultsHelper.getNodeTo(contingencyNetworkElement)));
                outage.getBranch().add(branch);
            });

            OutageResult.CurativeActions curativeActions = new OutageResult.CurativeActions();
            List<Action> curativeActions1 = getCurativeActions(contingency.getId(), cracResultsHelper);
            curativeActions.getAction().addAll(curativeActions1);
            outageResult.setCurativeActions(curativeActions);

            List<AfterOutageBranchResult> afterOutageBranchResults = new ArrayList<>();
            Map<String, MergedCnec> mergedMonitoredCnecs = cracResultsHelper.getMergedCnecs(contingency.getId());
            mergedMonitoredCnecs.values().forEach(mergedCnec -> {
                AfterOutageBranchResult afterOutageBranchResult = new AfterOutageBranchResult();
                afterOutageBranchResult.setName(mergedCnec.getCnecCommon().getName());
                fillBranchCommonPropertiesFromCnecCommon(mergedCnec.getCnecCommon(), afterOutageBranchResult);

                afterOutageBranchResult.setIMaxAfterOutage(getIValue((int) mergedCnec.getiMaxAfterOutage()));
                afterOutageBranchResult.setIAfterOutageAfterOptimization(getIValue((int) mergedCnec.getiAfterOutage()));
                afterOutageBranchResult.setIAfterOutageBeforeOptimization(getIValue((int) mergedCnec.getiAfterOutageBeforeOptimisation()));

                afterOutageBranchResult.setIMaxAfterCRA(getIValue((int) mergedCnec.getiMaxAfterCra()));
                afterOutageBranchResult.setIAfterCRAAfterOptimization(getIValue((int) mergedCnec.getiAfterCra()));
                afterOutageBranchResults.add(afterOutageBranchResult);
            });
            outageResult.getMonitoredElement().addAll(afterOutageBranchResults);
            outageResultList.add(outageResult);
        });
        return outageResultList;
    }

    private static void fillBranchCommonPropertiesFromCnecCommon(CnecCommon cnecCommon, Branch branch) {
        branch.setCode(getStringValue(cnecCommon.getOrderCode()));
        branch.setFromNode(getStringValue(cnecCommon.getNodeFrom()));
        branch.setToNode(getStringValue(cnecCommon.getNodeTo()));
        branch.setSelected(getBooleanValue(cnecCommon.isSelected()));
    }

    static void addPreventiveResult(CseRaoResult.Results results, PreventiveResult preventiveResult) {
        results.setPreventiveResult(preventiveResult);
    }

    static void addOutageResult(CseRaoResult.Results results, List<OutageResult> outageResults) {
        results.getOutageResult().addAll(outageResults);
    }

    static PreventiveResult getPreventiveResult(List<Action> preventiveActions, List<PreventiveBranchResult> preventiveBranchResults) {
        PreventiveResult preventiveResult = new PreventiveResult();
        PreventiveResult.PreventiveActions preventiveActions1 = new PreventiveResult.PreventiveActions();
        preventiveActions1.getAction().addAll(preventiveActions);
        preventiveResult.setPreventiveActions(preventiveActions1);
        preventiveResult.getMonitoredElement().addAll(preventiveBranchResults);
        return preventiveResult;
    }

    private static List<Action> getPreventiveActions(CracResultsHelper cracResultsHelper) {
        List<Action> preventiveActions = new ArrayList<>();
        List<RemedialActionCreationContext> importedRemedialActionCreationContext = cracResultsHelper.getCseCracCreationContext()
            .getRemedialActionCreationContexts()
            .stream()
            .filter(ElementaryCreationContext::isImported)
            .collect(Collectors.toList());

        importedRemedialActionCreationContext.stream()
            .filter(remedialActionCreationContext -> cracResultsHelper.getPreventiveNetworkActionIds().contains(remedialActionCreationContext.getCreatedRAId()))
            .forEach(remedialActionCreationContext -> addTopologicalAction(preventiveActions, remedialActionCreationContext));

        importedRemedialActionCreationContext.stream()
            .filter(CsePstCreationContext.class::isInstance)
            .map(CsePstCreationContext.class::cast)
            .filter(csePstCreationContext -> cracResultsHelper.getPreventivePstRangeActionIds().contains(csePstCreationContext.getCreatedRAId()))
            .forEach(csePstCreationContext -> addPstAction(preventiveActions, csePstCreationContext, cracResultsHelper::getTapOfPstRangeActionInPreventive));

        importedRemedialActionCreationContext.stream()
            .filter(CseHvdcCreationContext.class::isInstance)
            .map(CseHvdcCreationContext.class::cast)
            .filter(csePstCreationContext -> cracResultsHelper.getPreventiveHvdcRangeActionIds().contains(csePstCreationContext.getCreatedRAId()))
            .forEach(cseHvdcCreationContext -> addHvdcAction(preventiveActions, cseHvdcCreationContext, cracResultsHelper::getSetpointOfHvdcRangeActionInPreventive));

        return preventiveActions;
    }

    private static List<Action> getCurativeActions(String contingencyId, CracResultsHelper cracResultsHelper) {
        List<Action> curativeActions = new ArrayList<>();
        List<RemedialActionCreationContext> importedRemedialActionCreationContext = cracResultsHelper.getCseCracCreationContext()
            .getRemedialActionCreationContexts()
            .stream()
            .filter(ElementaryCreationContext::isImported)
            .collect(Collectors.toList());

        importedRemedialActionCreationContext.stream()
            .filter(remedialActionCreationContext -> cracResultsHelper.getCurativeNetworkActionIds(contingencyId).contains(remedialActionCreationContext.getCreatedRAId()))
            .forEach(remedialActionCreationContext -> addTopologicalAction(curativeActions, remedialActionCreationContext));

        importedRemedialActionCreationContext.stream()
            .filter(CsePstCreationContext.class::isInstance)
            .map(CsePstCreationContext.class::cast)
            .filter(csePstCreationContext -> cracResultsHelper.getCurativePstRangeActionIds(contingencyId).contains(csePstCreationContext.getCreatedRAId()))
            .forEach(csePstCreationContext -> addPstAction(curativeActions, csePstCreationContext, raId -> cracResultsHelper.getTapOfPstRangeActionInCurative(contingencyId, raId)));

        importedRemedialActionCreationContext.stream()
            .filter(CseHvdcCreationContext.class::isInstance)
            .map(CseHvdcCreationContext.class::cast)
            .filter(csePstCreationContext -> cracResultsHelper.getCurativeHvdcRangeActionIds(contingencyId).contains(csePstCreationContext.getCreatedRAId()))
            .forEach(cseHvdcCreationContext -> addHvdcAction(curativeActions, cseHvdcCreationContext, raId -> cracResultsHelper.getSetpointOfHvdcRangeActionInCurative(contingencyId, raId)));

        return curativeActions;
    }

    private static List<PreventiveBranchResult> getPreventiveBranchResults(CracResultsHelper cracResultsHelper) {
        List<PreventiveBranchResult> preventiveBranchResults = new ArrayList<>();
        List<CnecPreventive> preventiveCnecs = cracResultsHelper.getPreventiveCnecs();
        preventiveCnecs.forEach(cnecPrev -> {
            PreventiveBranchResult preventiveBranchResult = new PreventiveBranchResult();
            preventiveBranchResult.setName(cnecPrev.getCnecCommon().getName());
            fillBranchCommonPropertiesFromCnecCommon(cnecPrev.getCnecCommon(), preventiveBranchResult);

            preventiveBranchResult.setIMax(getIValue((int) cnecPrev.getiMax()));
            preventiveBranchResult.setIBeforeOptimization(getIValue((int) cnecPrev.getiBeforeOptimisation()));
            preventiveBranchResult.setIAfterOptimization(getIValue((int) cnecPrev.getI()));
            preventiveBranchResults.add(preventiveBranchResult);
        });
        return preventiveBranchResults;
    }

    private static void addTopologicalAction(List<Action> actions, RemedialActionCreationContext remedialActionCreationContext) {
        Action action = new Action();
        action.setName(remedialActionCreationContext.getNativeId());
        actions.add(action);
    }

    interface SetPointFinder {
        int findSetPoint(String raId);
    }

    private static void addPstAction(List<Action> actions, CsePstCreationContext csePstCreationContext, SetPointFinder finder) {
        String nativeId = csePstCreationContext.getNativeId();
        int tap = csePstCreationContext.isInverted() ?
                -finder.findSetPoint(csePstCreationContext.getCreatedRAId()) :
                finder.findSetPoint(csePstCreationContext.getCreatedRAId());
        Action action = new Action();
        action.setName(nativeId);
        action.setPSTtap(getIntValue(tap));
        actions.add(action);
    }

    private static void addHvdcAction(List<Action>  actions, CseHvdcCreationContext cseHvdcCreationContext, SetPointFinder finder) {
        String nativeId = cseHvdcCreationContext.getNativeId();
        int setPoint = finder.findSetPoint(cseHvdcCreationContext.getCreatedRAId());
        Action action = new Action();
        action.setName(nativeId);
        action.setSetpoint(getIntValue(setPoint));
        actions.add(action);
    }

    private static StringValue getStringValue(String str) {
        StringValue strValue = new StringValue();
        strValue.setV(str);
        return strValue;
    }

    private static IntValue getIntValue(int value) {
        IntValue intValue = new IntValue();
        intValue.setV(value);
        return intValue;
    }

    private static BooleanValue getBooleanValue(boolean bool) {
        BooleanValue boolValue = new BooleanValue();
        boolValue.setV(bool);
        return boolValue;
    }

    private static IValue getIValue(int i) {
        IValue iValue = new IValue();
        iValue.setV(i);
        iValue.setUnit("A");
        return iValue;
    }
}
