/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.cnec;

import com.farao_community.farao.cse.data.CseDataException;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.BranchCnecCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.NativeBranch;
import com.powsybl.openrao.data.crac.io.cse.CseCracCreationContext;
import com.powsybl.openrao.data.crac.io.cse.criticalbranch.CseCriticalBranchCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.ucte.network.UcteCountryCode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class CracResultsHelper {
    public static final String PREVENTIVE_OUTAGE_NAME = "N Situation";

    private final Logger businessLogger;
    private final CseCracCreationContext cseCracCreationContext;
    private final Crac crac;
    private final RaoResult raoResult;
    private final Network network;

    public CracResultsHelper(CseCracCreationContext cseCracCreationContext, RaoResult result, Network network, Logger businessLogger) {
        this.businessLogger = businessLogger;
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
                .toList();
    }

    public List<String> getPreventivePstRangeActionIds() {
        return raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState()).stream()
                .filter(PstRangeAction.class::isInstance)
                .map(Identifiable::getId)
                .toList();
    }

    public List<String> getPreventiveHvdcRangeActionIds() {
        return raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState()).stream()
                .filter(InjectionRangeAction.class::isInstance)
                .map(Identifiable::getId)
                .toList();
    }

    public int getTapOfPstRangeActionInPreventive(String pstRangeActionId) {
        return raoResult.getOptimizedTapOnState(crac.getPreventiveState(), crac.getPstRangeAction(pstRangeActionId));
    }

    public int getTapOfPstRangeActionInCurative(String contingencyId, String pstRangeActionId) {
        return raoResult.getOptimizedTapOnState(crac.getState(contingencyId, crac.getLastInstant()), crac.getPstRangeAction(pstRangeActionId));
    }

    public int getSetpointOfHvdcRangeActionInPreventive(String hvdcRangeActionId) {
        return (int) raoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), crac.getInjectionRangeAction(hvdcRangeActionId));
    }

    public List<String> getCurativeNetworkActionIds(String contingencyId) {
        return raoResult.getActivatedNetworkActionsDuringState(crac.getState(contingencyId, crac.getLastInstant())).stream()
                .map(Identifiable::getId)
                .toList();
    }

    public List<String> getCurativePstRangeActionIds(String contingencyId) {
        return raoResult.getActivatedRangeActionsDuringState(crac.getState(contingencyId, crac.getLastInstant())).stream()
                .filter(PstRangeAction.class::isInstance)
                .map(Identifiable::getId)
                .toList();
    }

    public List<String> getCurativeHvdcRangeActionIds(String contingencyId) {
        return raoResult.getActivatedRangeActionsDuringState(crac.getState(contingencyId, crac.getLastInstant())).stream()
                .filter(InjectionRangeAction.class::isInstance)
                .map(Identifiable::getId)
                .toList();
    }

    public int getSetpointOfHvdcRangeActionInCurative(String contingencyId, String hvdcRangeActionId) {
        return (int) raoResult.getOptimizedSetPointOnState(crac.getState(contingencyId, crac.getLastInstant()), crac.getInjectionRangeAction(hvdcRangeActionId));
    }

    public List<? extends BranchCnecCreationContext> getMonitoredBranchesForOutage(String contingencyId) {
        return cseCracCreationContext.getBranchCnecCreationContexts().stream()
                .filter(ElementaryCreationContext::isImported)
                .filter(branchCCC -> branchCCC.getContingencyId().orElse("").equals(contingencyId))
                .toList();

    }

    public List<CnecPreventive> getPreventiveCnecs() {
        List<CnecPreventive> cnecPreventives = new ArrayList<>();
        cseCracCreationContext.getBranchCnecCreationContexts().stream()
                .filter(ElementaryCreationContext::isImported)
                .filter(BranchCnecCreationContext::isBaseCase)
                .sorted(Comparator.comparing(BranchCnecCreationContext::getNativeObjectId))
                .forEach(branchCnecCreationContext -> {
                    // Native ID is actually modified at import to be unique, the only way we can find back original
                    // CNEC name is in the FlowCnec name
                    FlowCnec flowCnecPrev = crac.getFlowCnec(branchCnecCreationContext.getCreatedCnecsIds().get(crac.getPreventiveInstant().getId()));
                    if (flowCnecPrev != null) {
                        CnecCommon cnecCommon = makeCnecCommon(flowCnecPrev, branchCnecCreationContext.getNativeBranch(),
                                ((CseCriticalBranchCreationContext) branchCnecCreationContext).isSelected(), flowCnecPrev.isMonitored());
                        CnecPreventive cnecPrev = new CnecPreventive();
                        cnecPrev.setCnecCommon(cnecCommon);
                        FlowCnecResult flowCnecResult = getFlowCnecResultInAmpere(flowCnecPrev, crac.getPreventiveInstant());
                        cnecPrev.setI(flowCnecResult.getFlow());
                        cnecPrev.setiMax(flowCnecResult.getiMax());
                        FlowCnecResult flowCnecResultBeforeOptim = getFlowCnecResultInAmpere(flowCnecPrev, null);
                        cnecPrev.setiBeforeOptimisation(flowCnecResultBeforeOptim.getFlow());
                        cnecPreventives.add(cnecPrev);
                    } else {
                        throw new CseDataException(String.format("No preventive cnec from the cnec creation context id %s", branchCnecCreationContext.getNativeObjectId()));
                    }
                });
        return cnecPreventives;
    }

    public Map<String, MergedCnec> getMergedCnecs(String contingencyId) {
        Map<String, MergedCnec> mergedCnecs = new HashMap<>();
        List<? extends BranchCnecCreationContext> branchCnecCreationContexts = getMonitoredBranchesForOutage(contingencyId);
        branchCnecCreationContexts.forEach(branchCnecCreationContext -> {
            MergedCnec mergedCnec = new MergedCnec();
            FlowCnec flowCnec = null;
            for (Map.Entry<String, String> entry : branchCnecCreationContext.getCreatedCnecsIds().entrySet()) {
                flowCnec = crac.getFlowCnec(entry.getValue());
                FlowCnecResult flowCnecResult;
                switch (crac.getInstant(entry.getKey()).getKind()) {
                    case OUTAGE:
                        flowCnecResult = getFlowCnecResultInAmpere(flowCnec, crac.getPreventiveInstant());
                        mergedCnec.setiAfterOutage(flowCnecResult.getFlow());
                        mergedCnec.setiMaxAfterOutage(flowCnecResult.getiMax());
                        FlowCnecResult flowCnecResultBeforeOptim = getFlowCnecResultInAmpere(flowCnec, null);
                        mergedCnec.setiAfterOutageBeforeOptimisation(flowCnecResultBeforeOptim.getFlow());
                        break;
                    case CURATIVE:
                        flowCnecResult = getFlowCnecResultInAmpere(flowCnec, crac.getLastInstant());
                        mergedCnec.setiAfterCra(flowCnecResult.getFlow());
                        mergedCnec.setiMaxAfterCra(flowCnecResult.getiMax());
                        break;
                    case AUTO:
                        flowCnecResult = getFlowCnecResultInAmpere(flowCnec, crac.getPreventiveInstant());
                        mergedCnec.setiAfterSps(flowCnecResult.getFlow());
                        mergedCnec.setiMaxAfterSps(flowCnecResult.getiMax());
                        break;

                    default:
                        throw new CseDataException("Couldn't find Cnec type in cnec Id : " + flowCnec.getId());
                }
            }
            if (flowCnec != null) {
                mergedCnecs.put(branchCnecCreationContext.getNativeObjectId(), mergedCnec);
                CnecCommon cnecCommon = makeCnecCommon(flowCnec, branchCnecCreationContext.getNativeBranch(),
                        ((CseCriticalBranchCreationContext) branchCnecCreationContext).isSelected(),
                        flowCnec.isMonitored());
                mergedCnec.setCnecCommon(cnecCommon);
            } else {
                businessLogger.warn("Couldn't find flowCnec with native id : {}",
                        branchCnecCreationContext.getNativeObjectId());
            }
        });
        return mergedCnecs;
    }

    private CnecCommon makeCnecCommon(FlowCnec cnec, NativeBranch nativeBranch, boolean selected, boolean isMonitored) {
        NetworkElement networkElement = cnec.getNetworkElement();
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
        TwoSides monitoredSide = flowCnec.getMonitoredSides().contains(TwoSides.ONE) ? TwoSides.ONE : TwoSides.TWO;
        Optional<Double> upperBound = flowCnec.getUpperBound(monitoredSide, Unit.AMPERE);
        Optional<Double> lowerBound = flowCnec.getLowerBound(monitoredSide, Unit.AMPERE);
        double flow;
        double iMax;
        if (upperBound.isPresent()) {
            if (lowerBound.isPresent()) {
                double flowTemp = raoResult.getFlow(optimizedInstant, flowCnec, monitoredSide, Unit.AMPERE);
                flow = Math.abs(flowTemp);
                if (flowTemp >= 0) {
                    iMax = upperBound.get();
                } else {
                    iMax = Math.abs(lowerBound.get());
                }
            } else {
                flow = raoResult.getFlow(optimizedInstant, flowCnec, monitoredSide, Unit.AMPERE);
                iMax = upperBound.get();
            }
        } else {
            if (lowerBound.isPresent()) {
                // Case where it is limited in opposite direction so the flow is inverted
                flow = -raoResult.getFlow(optimizedInstant, flowCnec, monitoredSide, Unit.AMPERE);
                iMax = Math.abs(lowerBound.get());
            } else {
                throw new CseDataException(String.format("Cnec %s is defined with no thresholds", flowCnec.getName()));
            }
        }
        return new FlowCnecResult(flow, iMax);
    }

    public String getAreaFrom(ContingencyElement contingencyElement) {
        String nodeFrom = getNodeFrom(contingencyElement);
        String nodeTo = getNodeTo(contingencyElement);
        String countryFrom = UcteCountryCode.fromUcteCode(nodeFrom.charAt(0)).toString();
        String countryTo = UcteCountryCode.fromUcteCode(nodeTo.charAt(0)).toString();
        return getCountryOfNode(contingencyElement.getId(), countryFrom, countryTo);
    }

    public String getAreaTo(ContingencyElement contingencyElement) {
        String nodeFrom = getNodeFrom(contingencyElement);
        String nodeTo = getNodeTo(contingencyElement);
        String countryFrom = UcteCountryCode.fromUcteCode(nodeFrom.charAt(0)).toString();
        String countryTo = UcteCountryCode.fromUcteCode(nodeTo.charAt(0)).toString();
        return getCountryOfNode(contingencyElement.getId(), countryTo, countryFrom);
    }

    public String getAreaFrom(NetworkElement networkElement) {
        String nodeFrom = getNodeFrom(networkElement);
        String nodeTo = getNodeTo(networkElement);
        String countryFrom = UcteCountryCode.fromUcteCode(nodeFrom.charAt(0)).toString();
        String countryTo = UcteCountryCode.fromUcteCode(nodeTo.charAt(0)).toString();
        return getCountryOfNode(networkElement.getId(), countryFrom, countryTo);
    }

    public String getAreaTo(NetworkElement networkElement) {
        String nodeFrom = getNodeFrom(networkElement);
        String nodeTo = getNodeTo(networkElement);
        String countryFrom = UcteCountryCode.fromUcteCode(nodeFrom.charAt(0)).toString();
        String countryTo = UcteCountryCode.fromUcteCode(nodeTo.charAt(0)).toString();
        return getCountryOfNode(networkElement.getId(), countryTo, countryFrom);
    }

    private String getAreaFrom(NetworkElement networkElement, NativeBranch nativeBranch) {
        String countryFrom = UcteCountryCode.fromUcteCode(nativeBranch.getFrom().charAt(0)).toString();
        String countryTo = UcteCountryCode.fromUcteCode(nativeBranch.getTo().charAt(0)).toString();
        return getCountryOfNode(networkElement.getId(), countryFrom, countryTo);
    }

    private String getAreaTo(NetworkElement networkElement, NativeBranch nativeBranch) {
        String countryFrom = UcteCountryCode.fromUcteCode(nativeBranch.getFrom().charAt(0)).toString();
        String countryTo = UcteCountryCode.fromUcteCode(nativeBranch.getTo().charAt(0)).toString();
        return getCountryOfNode(networkElement.getId(), countryTo, countryFrom);
    }

    private String getCountryOfNode(String elementId, String nodeCountry, String destinationNodeCountry) {
        if (!nodeCountry.equals(UcteCountryCode.XX.toString())) {
            return nodeCountry;
        } else {
            String area1 = getCountrySide1(elementId);
            String area2 = getCountrySide2(elementId);
            if (StringUtils.equals(area1, destinationNodeCountry)) {
                return area2;
            } else {
                return area1;
            }
        }
    }

    private String getCountrySide1(String elementId) {
        Optional<Substation> substationOpt = network.getBranch(elementId).getTerminal1().getVoltageLevel().getSubstation();
        final String errorMessage = "NetworkElement " + elementId + " has no country on side 1";
        return getCountryFromSubstation(substationOpt, errorMessage);
    }

    private String getCountrySide2(String elementId) {
        Optional<Substation> substationOpt = network.getBranch(elementId).getTerminal2().getVoltageLevel().getSubstation();
        final String errorMessage = "NetworkElement " + elementId + " has no country on side 2";
        return getCountryFromSubstation(substationOpt, errorMessage);
    }

    private static String getCountryFromSubstation(final Optional<Substation> substationOpt, final String errorMessage) {
        if (substationOpt.isPresent()) {
            Optional<Country> country = substationOpt.get().getCountry();
            if (country.isPresent()) {
                return country.get().toString();
            } else {
                throw new CseDataException(errorMessage);
            }
        } else {
            throw new CseDataException(errorMessage);
        }
    }

    public String getNodeFrom(NetworkElement networkElement) {
        return networkElement.getId().substring(0, 8);
    }

    public String getNodeTo(NetworkElement networkElement) {
        return networkElement.getId().substring(9, 17);
    }

    public String getNodeFrom(ContingencyElement contingencyElement) {
        return contingencyElement.getId().substring(0, 8);
    }

    public String getNodeTo(ContingencyElement contingencyElement) {
        return contingencyElement.getId().substring(9, 17);
    }

    public String getOrderCode(ContingencyElement contingencyElement) {
        return contingencyElement.getId().substring(18, 19);
    }

    public static String getOutageName(FlowCnec flowCnec) {
        Optional<Contingency> contingencyOpt = flowCnec.getState().getContingency();
        if (contingencyOpt.isPresent()) {
            return contingencyOpt.get().getName().orElse(contingencyOpt.get().getId());
        } else {
            return PREVENTIVE_OUTAGE_NAME;
        }
    }

    public static List<ContingencyElement> getOutageElements(FlowCnec flowCnec) {
        Optional<Contingency> contingencyOpt = flowCnec.getState().getContingency();
        if (contingencyOpt.isPresent()) {
            return contingencyOpt.get().getElements();
        } else {
            return Collections.emptyList();
        }
    }

    public List<ElementaryCreationContext> getOutageCreationContext() {
        return cseCracCreationContext.getOutageCreationContexts().stream()
                .filter(ElementaryCreationContext::isImported)
                .toList();
    }
}
