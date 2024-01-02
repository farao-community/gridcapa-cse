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
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_creation.creator.api.ElementaryCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.BranchCnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.NativeBranch;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.critical_branch.CseCriticalBranchCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.outage.CseOutageCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.ucte.network.UcteCountryCode;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class CracResultsHelper {
    public static final String PREVENTIVE_OUTAGE_NAME = "N Situation";

    private final CseCracCreationContext cseCracCreationContext;
    private final Crac crac;
    private final RaoResult raoResult;
    private final Network network;

    public CracResultsHelper(CseCracCreationContext cseCracCreationContext, RaoResult result, Network network) {
        this.cseCracCreationContext = cseCracCreationContext;
        this.crac = cseCracCreationContext.getCrac();
        this.raoResult = result;
        this.network = network;
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
        cseCracCreationContext.getBranchCnecCreationContexts().stream()
            .filter(ElementaryCreationContext::isImported)
            .filter(BranchCnecCreationContext::isBaseCase)
            .sorted(Comparator.comparing(BranchCnecCreationContext::getNativeId))
            .forEach(branchCnecCreationContext -> {
                // Native ID is actually modified at import to be unique, the only way we can find back original
                // CNEC name is in the FlowCnec name
                FlowCnec flowCnecPrev = crac.getFlowCnec(branchCnecCreationContext.getCreatedCnecsIds().get(Instant.PREVENTIVE));
                if (flowCnecPrev != null) {
                    CnecCommon cnecCommon = makeCnecCommon(flowCnecPrev, branchCnecCreationContext.getNativeBranch(),
                            ((CseCriticalBranchCreationContext) branchCnecCreationContext).isSelected(), flowCnecPrev.isMonitored());
                    CnecPreventive cnecPrev = new CnecPreventive();
                    cnecPrev.setCnecCommon(cnecCommon);
                    FlowCnecResult flowCnecResult = getFlowCnecResultInAmpere(flowCnecPrev, Instant.PREVENTIVE);
                    cnecPrev.setI(flowCnecResult.getFlow());
                    cnecPrev.setiMax(flowCnecResult.getiMax());
                    FlowCnecResult flowCnecResultBeforeOptim = getFlowCnecResultInAmpere(flowCnecPrev, null);
                    cnecPrev.setiBeforeOptimisation(flowCnecResultBeforeOptim.getFlow());
                    cnecPreventives.add(cnecPrev);
                } else {
                    throw new CseDataException(String.format("No preventive cnec from the cnec creation context id %s", branchCnecCreationContext.getNativeId()));
                }
            });
        return cnecPreventives;
    }

    public Map<String, MergedCnec> getMergedCnecs(String contingencyId) {
        Map<String, MergedCnec> mergedCnecs = new HashMap<>();
        List<BranchCnecCreationContext> branchCnecCreationContexts = getMonitoredBranchesForOutage(contingencyId);
        branchCnecCreationContexts.forEach(branchCnecCreationContext -> {
            MergedCnec mergedCnec = new MergedCnec();
            mergedCnecs.put(branchCnecCreationContext.getNativeId(), mergedCnec);
            FlowCnec flowCnec = null;
            for (Map.Entry<Instant, String> entry : branchCnecCreationContext.getCreatedCnecsIds().entrySet()) {
                flowCnec = crac.getFlowCnec(entry.getValue());
                FlowCnecResult flowCnecResult;
                switch (entry.getKey()) {
                    case OUTAGE:
                        flowCnecResult = getFlowCnecResultInAmpere(flowCnec, Instant.PREVENTIVE);
                        mergedCnec.setiAfterOutage(flowCnecResult.getFlow());
                        mergedCnec.setiMaxAfterOutage(flowCnecResult.getiMax());
                        FlowCnecResult flowCnecResultBeforeOptim = getFlowCnecResultInAmpere(flowCnec, null);
                        mergedCnec.setiAfterOutageBeforeOptimisation(flowCnecResultBeforeOptim.getFlow());
                        break;
                    case CURATIVE:
                        flowCnecResult = getFlowCnecResultInAmpere(flowCnec, Instant.CURATIVE);
                        mergedCnec.setiAfterCra(flowCnecResult.getFlow());
                        mergedCnec.setiMaxAfterCra(flowCnecResult.getiMax());
                        break;
                    case AUTO:
                        flowCnecResult = getFlowCnecResultInAmpere(flowCnec, Instant.PREVENTIVE);
                        mergedCnec.setiAfterSps(flowCnecResult.getFlow());
                        mergedCnec.setiMaxAfterSps(flowCnecResult.getiMax());
                        break;

                    default:
                        throw new CseDataException("Couldn't find Cnec type in cnec Id : " + flowCnec.getId());
                }
            }
            CnecCommon cnecCommon = makeCnecCommon(flowCnec, branchCnecCreationContext.getNativeBranch(),
                ((CseCriticalBranchCreationContext) branchCnecCreationContext).isSelected(), flowCnec != null && flowCnec.isMonitored());
            mergedCnec.setCnecCommon(cnecCommon);
        });
        return mergedCnecs;
    }

    private CnecCommon makeCnecCommon(FlowCnec cnec, NativeBranch nativeBranch, boolean selected, boolean isMonitored) {
        CnecCommon cnecCommon = new CnecCommon();
        cnecCommon.setName(cnec.getName());
        cnecCommon.setCode(makeCode(nativeBranch));
        cnecCommon.setAreaFrom(getAreaFrom(networkElement, nativeBranch));
        cnecCommon.setAreaTo(getAreaTo(networkElement, nativeBranch));
        cnecCommon.setNodeFrom(nativeBranch.getFrom());
        cnecCommon.setNodeTo(nativeBranch.getTo());
        cnecCommon.setOrderCode(nativeBranch.getSuffix());
        cnecCommon.setSelected(selected);
        cnecCommon.setMonitored(isMonitored);
        return cnecCommon;
    }

    private String makeCode(NativeBranch nativeBranch) {
        return nativeBranch.getFrom() + " " + nativeBranch.getTo() + " " + nativeBranch.getSuffix();
    }

    public FlowCnecResult getFlowCnecResultInAmpere(FlowCnec flowCnec, Instant optimizedInstant) {
        Side monitoredSide = flowCnec.getMonitoredSides().contains(Side.LEFT) ? Side.LEFT : Side.RIGHT;
        Optional<Double> upperBound = flowCnec.getUpperBound(monitoredSide, Unit.AMPERE);
        Optional<Double> lowerBound = flowCnec.getLowerBound(monitoredSide, Unit.AMPERE);
        double flow;
        double iMax;
        if (upperBound.isPresent() && lowerBound.isEmpty()) {
            flow = raoResult.getFlow(optimizedInstant, flowCnec, monitoredSide, Unit.AMPERE);
            iMax = upperBound.get();
        } else if (upperBound.isEmpty() && lowerBound.isPresent()) {
            // Case where it is limited in opposite direction so the flow is inverted
            flow = -raoResult.getFlow(optimizedInstant, flowCnec, monitoredSide, Unit.AMPERE);
            iMax = Math.abs(lowerBound.get());
        } else if (upperBound.isPresent() && lowerBound.isPresent()) {
            double flowTemp = raoResult.getFlow(optimizedInstant, flowCnec, monitoredSide, Unit.AMPERE);
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
        return getCountryOfNode(networkElement, countryFrom, countryTo);
    }

    public String getAreaTo(NetworkElement networkElement) {
        String nodeFrom = getNodeFrom(networkElement);
        String nodeTo = getNodeTo(networkElement);
        String countryFrom = UcteCountryCode.fromUcteCode(nodeFrom.charAt(0)).toString();
        String countryTo = UcteCountryCode.fromUcteCode(nodeTo.charAt(0)).toString();
        return getCountryOfNode(networkElement, countryTo, countryFrom);
    }

    private String getAreaFrom(NetworkElement networkElement, NativeBranch nativeBranch) {
        String countryFrom = UcteCountryCode.fromUcteCode(nativeBranch.getFrom().charAt(0)).toString();
        String countryTo = UcteCountryCode.fromUcteCode(nativeBranch.getTo().charAt(0)).toString();
        return getCountryOfNode(networkElement, countryFrom, countryTo);
    }

    private String getAreaTo(NetworkElement networkElement, NativeBranch nativeBranch) {
        String countryFrom = UcteCountryCode.fromUcteCode(nativeBranch.getFrom().charAt(0)).toString();
        String countryTo = UcteCountryCode.fromUcteCode(nativeBranch.getTo().charAt(0)).toString();
        return getCountryOfNode(networkElement, countryTo, countryFrom);
    }

    private String getCountryOfNode(NetworkElement networkElement, String nodeCountry, String destinationNodeCountry) {
        if (!nodeCountry.equals(UcteCountryCode.XX.toString())) {
            return nodeCountry;
        } else {
            String area1 = getCountrySide1(networkElement);
            String area2 = getCountrySide2(networkElement);
            if (StringUtils.equals(area1, destinationNodeCountry)) {
                return area2;
            } else {
                return area1;
            }
        }
    }

    private String getCountrySide1(NetworkElement networkElement) {
        Optional<Substation> substationOpt = network.getBranch(networkElement.getId()).getTerminal1().getVoltageLevel().getSubstation();
        Optional<Country> country = getCountryOptionalFromSubstation(substationOpt);
        if (country.isPresent()) {
            return country.get().toString();
        } else {
            throw new CseDataException("NetworkElement " + networkElement.getId() + " has no country on side 1");
        }
    }

    private String getCountrySide2(NetworkElement networkElement) {
        Optional<Substation> substationOpt = network.getBranch(networkElement.getId()).getTerminal2().getVoltageLevel().getSubstation();
        Optional<Country> country = getCountryOptionalFromSubstation(substationOpt);
        if (country.isPresent()) {
            return country.get().toString();
        } else {
            throw new CseDataException("NetworkElement " + networkElement.getId() + " has no country on side 2");
        }
    }

    Optional<Country> getCountryOptionalFromSubstation(Optional<Substation> substation) {
        if (substation.isPresent()) {
            Optional<Country> countryOpt = substation.get().getCountry();
            if (countryOpt.isPresent()) {
                return countryOpt;
            }
        }
        return Optional.empty();
    }

    public String getNodeFrom(NetworkElement networkElement) {
        return networkElement.getId().substring(0, 8);
    }

    public String getNodeTo(NetworkElement networkElement) {
        return networkElement.getId().substring(9, 17);
    }

    public String getOrderCode(NetworkElement networkElement) {
        return networkElement.getId().substring(18, 19);
    }

    public static String getOutageName(FlowCnec flowCnec) {
        Optional<Contingency> contingencyOpt = flowCnec.getState().getContingency();
        if (contingencyOpt.isPresent()) {
            return contingencyOpt.get().getName();
        } else {
            return PREVENTIVE_OUTAGE_NAME;
        }
    }

    public static Set<NetworkElement> getOutageElements(FlowCnec flowCnec) {
        Optional<Contingency> contingencyOpt = flowCnec.getState().getContingency();
        if (contingencyOpt.isPresent()) {
            return contingencyOpt.get().getNetworkElements();
        } else {
            return Collections.emptySet();
        }
    }

    public List<CseOutageCreationContext> getOutageCreationContext() {
        return cseCracCreationContext.getOutageCreationContexts().stream()
                .filter(ElementaryCreationContext::isImported).collect(Collectors.toList());
    }
}
