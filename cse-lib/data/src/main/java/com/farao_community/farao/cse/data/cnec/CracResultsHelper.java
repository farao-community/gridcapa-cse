/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.cnec;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.data.xnode.XNode;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_creation.creator.api.ElementaryCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.BranchCnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracCreationContext;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.powsybl.ucte.network.UcteCountryCode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class CracResultsHelper {
    public static final String PREVENTIVE_OUTAGE_NAME = "N Situation";

    private final CseCracCreationContext cseCracCreationContext;
    private final Crac crac;
    private final RaoResult raoResult;
    private final List<XNode> xNodeList;

    public CracResultsHelper(CseCracCreationContext cseCracCreationContext, RaoResult result, List<XNode> xNodeList) {
        this.cseCracCreationContext = cseCracCreationContext;
        this.crac = cseCracCreationContext.getCrac();
        this.raoResult = result;
        this.xNodeList = xNodeList;
    }

    public Crac getCrac() {
        return crac;
    }

    public RaoResult getRaoResult() {
        return raoResult;
    }

    public CseCracCreationContext getCseCracCreationContext() {
        return cseCracCreationContext;
    }

    public List<String> getPreventiveNetworkActionIds() {
        return raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()).stream()
            .map(Identifiable::getId)
            .collect(Collectors.toList());
    }

    public List<String> getPreventivePstRangeActionIds() {
        return raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState()).stream()
                .filter(PstRangeAction.class::isInstance)
                .map(Identifiable::getId)
                .collect(Collectors.toList());
    }

    public List<String> getPreventiveHvdcRangeActionIds() {
        return raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState()).stream()
                .filter(InjectionRangeAction.class::isInstance)
                .map(Identifiable::getId)
                .collect(Collectors.toList());
    }

    public int getTapOfPstRangeActionInPreventive(String pstRangeActionId) {
        return raoResult.getOptimizedTapOnState(crac.getPreventiveState(), crac.getPstRangeAction(pstRangeActionId));
    }

    public int getTapOfPstRangeActionInCurative(String contingencyId, String pstRangeActionId) {
        return raoResult.getOptimizedTapOnState(crac.getState(contingencyId, Instant.CURATIVE), crac.getPstRangeAction(pstRangeActionId));
    }

    public int getSetpointOfHvdcRangeActionInPreventive(String hvdcRangeActionId) {
        return (int) raoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), crac.getInjectionRangeAction(hvdcRangeActionId));
    }

    public List<String> getCurativeNetworkActionIds(String contingencyId) {
        return raoResult.getActivatedNetworkActionsDuringState(crac.getState(contingencyId, Instant.CURATIVE)).stream()
            .map(Identifiable::getId)
            .collect(Collectors.toList());
    }

    public List<String> getCurativePstRangeActionIds(String contingencyId) {
        return raoResult.getActivatedRangeActionsDuringState(crac.getState(contingencyId, Instant.CURATIVE)).stream()
            .filter(PstRangeAction.class::isInstance)
            .map(Identifiable::getId)
            .collect(Collectors.toList());
    }

    public List<String> getCurativeHvdcRangeActionIds(String contingencyId) {
        return raoResult.getActivatedRangeActionsDuringState(crac.getState(contingencyId, Instant.CURATIVE)).stream()
            .filter(InjectionRangeAction.class::isInstance)
            .map(Identifiable::getId)
            .collect(Collectors.toList());
    }

    public int getSetpointOfHvdcRangeActionInCurative(String contingencyId, String hvdcRangeActionId) {
        return (int) raoResult.getOptimizedSetPointOnState(crac.getState(contingencyId, Instant.CURATIVE), crac.getInjectionRangeAction(hvdcRangeActionId));
    }

    public List<BranchCnecCreationContext> getMonitoredBranchesForOutage(String contingencyId) {
        return cseCracCreationContext.getBranchCnecCreationContexts().stream()
            .filter(ElementaryCreationContext::isImported)
            .filter(branchCCC -> branchCCC.getContingencyId().orElse("").equals(contingencyId))
            .collect(Collectors.toList());

    }

    public List<CnecPreventive> getPreventiveCnecs() {
        List<CnecPreventive> cnecPreventives = new ArrayList<>();
        crac.getFlowCnecs(crac.getPreventiveState())
                .stream()
                .sorted(Comparator.comparing(FlowCnec::getName))
                .forEach(cnecPreventive -> {
                    CnecCommon cnecCommon = new CnecCommon();
                    cnecCommon.setName(cnecPreventive.getName());
                    cnecCommon.setCode(cnecPreventive.getNetworkElement().getName());
                    cnecCommon.setAreaFrom(getAreaFrom(cnecPreventive.getNetworkElement()));
                    cnecCommon.setAreaTo(getAreaTo(cnecPreventive.getNetworkElement()));
                    cnecCommon.setOrderCode(getOrderCode(cnecPreventive.getNetworkElement()));
                    cnecCommon.setNodeFrom(getNodeFrom(cnecPreventive.getNetworkElement()));
                    cnecCommon.setNodeTo(getNodeTo(cnecPreventive.getNetworkElement()));
                    CnecPreventive cnecPrev = new CnecPreventive();
                    cnecPrev.setCnecCommon(cnecCommon);
                    FlowCnecResult flowCnecResult = getFlowCnecResultInAmpere(cnecPreventive, OptimizationState.AFTER_PRA);
                    cnecPrev.setI(flowCnecResult.getFlow());
                    cnecPrev.setiMax(flowCnecResult.getiMax());
                    FlowCnecResult flowCnecResultBeforeOptim = getFlowCnecResultInAmpere(cnecPreventive, OptimizationState.INITIAL);
                    cnecPrev.setiBeforeOptimisation(flowCnecResultBeforeOptim.getFlow());
                    cnecPreventives.add(cnecPrev);
                });
        return cnecPreventives;
    }

    public Map<String, MergedCnec> getMergedCnecs(String contingencyId) {
        Map<String, MergedCnec> mergedCnecs = new HashMap<>();
        List<BranchCnecCreationContext> branchCnecCreationContexts = getMonitoredBranchesForOutage(contingencyId);

        branchCnecCreationContexts.forEach(branchCnecCreationContext -> {
            List<FlowCnec> flowCnecs = Stream.of(branchCnecCreationContext.getCreatedCnecsIds()).flatMap(x -> x.values().stream()).map(crac::getFlowCnec).collect(Collectors.toList());
            flowCnecs.forEach(cnec -> {

                MergedCnec mergedCnec;
                if (!mergedCnecs.containsKey(cnec.getName())) {
                    mergedCnec = new MergedCnec();
                    CnecCommon cnecCommon = new CnecCommon();
                    cnecCommon.setName(branchCnecCreationContext.getNativeId());
                    cnecCommon.setCode(cnec.getNetworkElement().getName());
                    cnecCommon.setAreaFrom(getAreaFrom(cnec.getNetworkElement()));
                    cnecCommon.setAreaTo(getAreaTo(cnec.getNetworkElement()));
                    cnecCommon.setNodeFrom(getNodeFrom(cnec.getNetworkElement()));
                    cnecCommon.setNodeTo(getNodeTo(cnec.getNetworkElement()));
                    cnecCommon.setOrderCode(getOrderCode(cnec.getNetworkElement()));
                    mergedCnec.setCnecCommon(cnecCommon);
                    mergedCnecs.put(cnec.getName(), mergedCnec);
                } else {
                    mergedCnec = mergedCnecs.get(cnec.getName());
                }
                if (cnec.getState().getInstant().equals(Instant.OUTAGE)) {
                    FlowCnecResult flowCnecResult = getFlowCnecResultInAmpere(cnec, OptimizationState.AFTER_PRA);
                    mergedCnec.setiAfterOutage(flowCnecResult.getFlow());
                    mergedCnec.setiMaxAfterOutage(flowCnecResult.getiMax());
                    FlowCnecResult flowCnecResultBeforeOptim = getFlowCnecResultInAmpere(cnec, OptimizationState.INITIAL);
                    mergedCnec.setiAfterOutageBeforeOptimisation(flowCnecResultBeforeOptim.getFlow());

                } else if (cnec.getState().getInstant().equals(Instant.CURATIVE)) {
                    FlowCnecResult flowCnecResult = getFlowCnecResultInAmpere(cnec, OptimizationState.AFTER_CRA);
                    mergedCnec.setiAfterCra(flowCnecResult.getFlow());
                    mergedCnec.setiMaxAfterCra(flowCnecResult.getiMax());
                } else if (cnec.getState().getInstant().equals(Instant.AUTO)) {
                    FlowCnecResult flowCnecResult = getFlowCnecResultInAmpere(cnec, OptimizationState.AFTER_PRA);
                    mergedCnec.setiAfterSps(flowCnecResult.getFlow());
                    mergedCnec.setiMaxAfterSps(flowCnecResult.getiMax());
                } else {
                    throw new CseDataException("Couldn't find Cnec type in cnec Id : " + cnec.getId());
                }
            });

        });
        return mergedCnecs;
    }

    public FlowCnecResult getFlowCnecResultInAmpere(FlowCnec flowCnec, OptimizationState optimizationState) {
        Optional<Double> upperBound = flowCnec.getUpperBound(Side.LEFT, Unit.AMPERE);
        Optional<Double> lowerBound = flowCnec.getLowerBound(Side.LEFT, Unit.AMPERE);
        double flow;
        double iMax;
        if (upperBound.isPresent() && lowerBound.isEmpty()) {
            flow = raoResult.getFlow(optimizationState, flowCnec, Unit.AMPERE);
            iMax = upperBound.get();
        } else if (upperBound.isEmpty() && lowerBound.isPresent()) {
            // Case where it is limited in opposite direction so the flow is inverted
            flow = -raoResult.getFlow(optimizationState, flowCnec, Unit.AMPERE);
            iMax = Math.abs(lowerBound.get());
        } else if (upperBound.isPresent() && lowerBound.isPresent()) {
            double flowTemp = raoResult.getFlow(optimizationState, flowCnec, Unit.AMPERE);
            flow = Math.abs(flowTemp);
            if (flowTemp >= 0) {
                iMax = upperBound.get();
            } else {
                iMax = Math.abs(lowerBound.get());
            }
        } else {
            throw new CseDataException(String.format("Cnec %s is defined with no thresholds", flowCnec.getName()));
        }
        return new FlowCnecResult(flow, iMax);
    }

    public String getAreaFrom(NetworkElement networkElement) {
        String nodeFrom = getNodeFrom(networkElement);
        String nodeTo = getNodeTo(networkElement);
        String countryFrom = UcteCountryCode.fromUcteCode(nodeFrom.charAt(0)).toString();
        String countryTo = UcteCountryCode.fromUcteCode(nodeTo.charAt(0)).toString();
        return getCountryOfNode(nodeFrom, countryFrom, countryTo);
    }

    public String getAreaTo(NetworkElement networkElement) {
        String nodeFrom = getNodeFrom(networkElement);
        String nodeTo = getNodeTo(networkElement);
        String countryFrom = UcteCountryCode.fromUcteCode(nodeFrom.charAt(0)).toString();
        String countryTo = UcteCountryCode.fromUcteCode(nodeTo.charAt(0)).toString();
        return getCountryOfNode(nodeTo, countryTo, countryFrom);
    }

    private String getCountryOfNode(String nodeFrom, String countryFrom, String countryTo) {
        if (!countryFrom.equals(UcteCountryCode.XX.toString())) {
            return countryFrom;
        } else {
            Optional<XNode> xNodeOpt = xNodeList.stream().filter(xNode -> xNode.getName().equals(nodeFrom)).findFirst();
            if (xNodeOpt.isPresent()) {
                String area1 = xNodeOpt.get().getArea1();
                String area2 = xNodeOpt.get().getArea2();
                if (area1.equals(countryTo)) {
                    return area2;
                } else {
                    return area1;
                }
            } else {
                throw new CseDataException("XNode " + nodeFrom + " Not found in XNodes configuration file");
            }
        }
    }

    public String getNodeFrom(NetworkElement networkElement) {
        return networkElement.getId().substring(0, 8);
    }

    public String getNodeTo(NetworkElement networkElement) {
        return networkElement.getId().substring(9, 17);
    }

    public String getOrderCode(NetworkElement networkElement) {
        return networkElement.getId().substring(18);
    }

    public FlowCnec getWorstCnec() {
        double worstMargin = Double.MAX_VALUE;
        Optional<FlowCnec> worstCnec = Optional.empty();
        for (FlowCnec flowCnec : crac.getFlowCnecs()) {
            double margin = computeFlowMargin(flowCnec);
            if (margin < worstMargin) {
                worstMargin = margin;
                worstCnec = Optional.of(flowCnec);
            }
        }
        return worstCnec.orElseThrow(() -> new CseDataException("Exception occurred while retrieving the most limiting element."));
    }

    private double computeFlowMargin(FlowCnec flowCnec) {
        if (flowCnec.getState().getInstant() == Instant.CURATIVE) {
            return raoResult.getMargin(OptimizationState.AFTER_CRA, flowCnec, Unit.AMPERE);
        } else {
            return raoResult.getMargin(OptimizationState.AFTER_PRA, flowCnec, Unit.AMPERE);
        }
    }

    public static String getOutageName(FlowCnec flowCnec) {
        Optional<Contingency> contingencyOpt = flowCnec.getState().getContingency();
        if (contingencyOpt.isPresent()) {
            return contingencyOpt.get().getName();
        } else {
            return PREVENTIVE_OUTAGE_NAME;
        }
    }
}