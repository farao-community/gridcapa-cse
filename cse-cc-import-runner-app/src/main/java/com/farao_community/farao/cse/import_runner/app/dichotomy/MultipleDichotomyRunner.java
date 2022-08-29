/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.cse.computation.BorderExchanges;
import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.import_runner.app.services.FileImporter;
import com.farao_community.farao.cse.import_runner.app.services.ForcedPrasHandler;
import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.dichotomy.shift.LinearScaler;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class MultipleDichotomyRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleDichotomyRunner.class);
    private static final double SHIFT_TOLERANCE = 1;

    private final DichotomyRunner dichotomyRunner;
    private final FileImporter fileImporter;
    private final ForcedPrasHandler forcedPrasHandler;
    private final Logger businessLogger;

    public MultipleDichotomyRunner(DichotomyRunner dichotomyRunner, FileImporter fileImporter, ForcedPrasHandler forcedPrasHandler, Logger logger) {
        this.dichotomyRunner = dichotomyRunner;
        this.fileImporter = fileImporter;
        this.forcedPrasHandler = forcedPrasHandler;
        this.businessLogger = logger;
    }

    public MultipleDichotomyResult runMultipleDichotomy(CseRequest request,
                                                        CseData cseData,
                                                        Network network,
                                                        Crac crac,
                                                        double initialItalianImport) throws IOException {

        List<String> manualForcedPrasIds = request.getManualForcedPrasIds();
        Integer maximumDichotomiesNumber = request.getMaximumDichotomiesNumber();
        Map<String, List<Set<String>>> automatedForcedPrasIds = request.getAutomatedForcedPrasIds();
        MultipleDichotomyResult multipleDichotomyResult = new MultipleDichotomyResult();

        NetworkShifter networkShifter = getNetworkShifter(request, cseData, network);

        try {
            networkShifter.shiftNetwork(initialItalianImport, network);
            LoadFlow.run(network, LoadFlowParameters.load());
        } catch (GlskLimitationException e) {
            String errorMessage = String.format("Glsk limitation prevents the network to be shifted: %s", e.getMessage());
            LOGGER.error(errorMessage);
            throw new CseDataException(errorMessage);
        } catch (ShiftingException e) {
            String errorMessage = String.format("Exception occurred during the shift on network: %s", e.getMessage());
            LOGGER.error(errorMessage);
            throw new CseDataException(errorMessage);
        }

        // Force manual PRAs if specified
        if (!manualForcedPrasIds.isEmpty()) {
            forcedPrasHandler.forcePras(manualForcedPrasIds, network, crac);
        }

        // Launch initial dichotomy and store result
        DichotomyResult<RaoResponse> initialDichotomyResult = dichotomyRunner.runDichotomy(request, cseData, network, networkShifter, initialItalianImport);
        multipleDichotomyResult.addResult(initialDichotomyResult, new HashSet<>(manualForcedPrasIds));

        if (automatedForcedPrasIds.isEmpty() ||
            (multipleDichotomyResult.getBestDichotomyResult().getHighestValidStep() != null && !multipleDichotomyResult.getBestDichotomyResult().getHighestValidStep().isValid())) {
            return multipleDichotomyResult;
        }

        int dichotomyCount = 1;
        String limitingElement;
        int counterPerLimitingElement = 0;
        businessLogger.info("Dichotomies maximum number is '{}'", maximumDichotomiesNumber);
        limitingElement = getLimitingElement(multipleDichotomyResult.getBestDichotomyResult().getHighestValidStep());

        while (dichotomyCount <= maximumDichotomiesNumber) {

            List<Set<String>> forcedPrasForLimitingElement = Optional
                .ofNullable(automatedForcedPrasIds.get(limitingElement))
                .orElse(automatedForcedPrasIds.get("default"));

            if (forcedPrasForLimitingElement == null || counterPerLimitingElement >= forcedPrasForLimitingElement.size()) {
                // Limiting element not in the list and no default list of forced PRAs  || all RAs combinations are tried
                businessLogger.warn("No (more) RAs matching the limiting element '{}' found in the automatedForcedPras inputs. No more dichotomies can be performed", limitingElement);
                return multipleDichotomyResult;
            }

            Set<String> raToBeForced = forcedPrasForLimitingElement.get(counterPerLimitingElement);
            String raListToString = raToBeForced.stream().map(Object::toString).collect(Collectors.joining(","));

            if (!checkIfPrasCombinationHasImpactOnNetwork(raToBeForced, crac, network)) {
                businessLogger.info("RAs combination '{}' has no impact on network. It will not be applied", raListToString);
                counterPerLimitingElement++;
                continue;
            }

            try {
                businessLogger.info("Trying to force pras combination '{}'", raListToString);
                forcedPrasHandler.forcePras(new ArrayList<>(raToBeForced), network, crac);
            } catch (CseDataException e) {
                businessLogger.info("RAs combination '{}' not allowed for the limiting element: '{}'. Next RAs combination will be tried. Exception details: {}", raListToString, limitingElement, e.getMessage());
                // Go to next forced pra set for this limiting element without incrementing dichotomy counter
                counterPerLimitingElement++;
                continue;
            }

            Network bestNetwork = fileImporter.importNetwork(multipleDichotomyResult.getBestNetworkUrl());
            businessLogger.info("Automated forced pras processing: Dichotomy number is : '{}'. Current limiting element is : '{}'.", dichotomyCount, limitingElement);
            DichotomyResult<RaoResponse> nextDichotomyResult = dichotomyRunner.runDichotomy(request, cseData, bestNetwork, networkShifter, initialItalianImport);

            String newLimitingElement = getLimitingElement(nextDichotomyResult.getHighestValidStep());
            double previousHighestTtc = getTTC(multipleDichotomyResult.getBestDichotomyResult().getHighestValidStep());
            double newTtc = getTTC(nextDichotomyResult.getHighestValidStep());

            if (limitingElement.equals(newLimitingElement)) {
                businessLogger.info("The limiting element '{}' didn't change after the last dichotomy. Next RAs combination will be tried", limitingElement);
                counterPerLimitingElement++;
            } else {
                businessLogger.info("The limiting element '{}' changed after the last dichotomy. New limiting element is '{}'", limitingElement, newLimitingElement);

                if (newTtc > previousHighestTtc) {
                    limitingElement = newLimitingElement;
                    counterPerLimitingElement = 0;
                }
            }

            if (previousHighestTtc < newTtc) {
                multipleDichotomyResult.addResult(nextDichotomyResult, raToBeForced);
                businessLogger.info("New TTC '{}' is higher than previous TTC '{}'. Result will be kept", newTtc, previousHighestTtc);
            } else {
                businessLogger.info("New TTC '{}' is lower than previous TTC '{}'. Result will be ignored", newTtc, previousHighestTtc);
            }

            dichotomyCount++;
        }
        return multipleDichotomyResult;
    }

    private boolean checkIfPrasCombinationHasImpactOnNetwork(Set<String> raToBeForced, Crac crac, Network network) {
        //if one elementary action of the network action has an impact on the network then the network action has an impact on the network
        List<String> availablePreventivePrasInCrac = crac.getNetworkActions().stream()
            .filter(na -> na.getUsageMethod(crac.getPreventiveState()).equals(UsageMethod.AVAILABLE))
            .map(NetworkAction::getId)
            .collect(Collectors.toList());

        List<String> rasToBeForcedAvailableInCrac = raToBeForced.stream()
            .filter(availablePreventivePrasInCrac::contains)
            .collect(Collectors.toList());

        if (!rasToBeForcedAvailableInCrac.isEmpty()) {

            List<String> rasWithImpactOnNetwork = new ArrayList<>();
            rasToBeForcedAvailableInCrac.stream().map(crac::getNetworkAction).forEach(networkAction -> {
                if (networkAction.hasImpactOnNetwork(network)) {
                    rasWithImpactOnNetwork.add(networkAction.getId());
                }
            });
            return !rasWithImpactOnNetwork.isEmpty();

        } else {
            return false;
        }
    }

    private NetworkShifter getNetworkShifter(CseRequest request,
                                             CseData cseData,
                                             Network network) throws IOException {
        return new LinearScaler(
            getZonalScalable(request.getMergedGlskUrl(), network),
            getShiftDispatcher(request.getProcessType(), cseData, network),
            SHIFT_TOLERANCE);
    }

    private ZonalData<Scalable> getZonalScalable(String mergedGlskUrl, Network network) throws IOException {
        ZonalData<Scalable> zonalScalable = fileImporter.importGlsk(mergedGlskUrl, network);
        Arrays.stream(CseCountry.values()).forEach(country -> checkCseCountryInGlsk(zonalScalable, country));
        return zonalScalable;
    }

    private void checkCseCountryInGlsk(ZonalData<Scalable> zonalScalable, CseCountry country) {
        if (!zonalScalable.getDataPerZone().containsKey(country.getEiCode())) {
            throw new CseInvalidDataException(String.format("Area '%s' was not found in the glsk file.", country.getEiCode()));
        }
    }

    private ShiftDispatcher getShiftDispatcher(ProcessType processType, CseData cseData, Network network) {
        if (processType == ProcessType.D2CC) {
            return new CseD2ccShiftDispatcher(
                convertSplittingFactors(cseData.getReducedSplittingFactors()),
                convertBorderExchanges(BorderExchanges.computeCseBordersExchanges(network, true)),
                convertFlowsOnMerchantLines(cseData.getNtc().getFlowPerCountryOnMerchantLines()));
        } else {
            return new CseIdccShiftDispatcher(
                convertSplittingFactors(cseData.getReducedSplittingFactors()),
                cseData.getCseReferenceExchanges().getExchanges(),
                cseData.getNtc2().getExchanges());
        }
    }

    static Map<String, Double> convertSplittingFactors(Map<String, Double> tSplittingFactors) {
        Map<String, Double> splittingFactors = new TreeMap<>();
        tSplittingFactors.forEach((key, value) -> splittingFactors.put(toEic(key), value));
        splittingFactors.put(toEic("IT"), -splittingFactors.values().stream().reduce(0., Double::sum));
        return splittingFactors;
    }

    static Map<String, Double> convertBorderExchanges(Map<String, Double> borderExchanges) {
        Map<String, Double> convertedBorderExchanges = new HashMap<>();
        borderExchanges.forEach((key, value) -> {
            // We take -value because we want flow towards Italy
            switch (key) {
                case BorderExchanges.IT_AT:
                    convertedBorderExchanges.put(CseCountry.AT.getEiCode(), -value);
                    break;
                case BorderExchanges.IT_CH:
                    convertedBorderExchanges.put(CseCountry.CH.getEiCode(), -value);
                    break;
                case BorderExchanges.IT_FR:
                    convertedBorderExchanges.put(CseCountry.FR.getEiCode(), -value);
                    break;
                case BorderExchanges.IT_SI:
                    convertedBorderExchanges.put(CseCountry.SI.getEiCode(), -value);
                    break;
                default:
                    break;
            }
        });
        return convertedBorderExchanges;
    }

    static Map<String, Double> convertFlowsOnMerchantLines(Map<String, Double> flowOnMerchantLinesPerCountry) {
        Map<String, Double> convertedFlowOnMerchantLinesPerCountry = new HashMap<>();
        Set.of(CseCountry.FR, CseCountry.CH, CseCountry.AT, CseCountry.SI).forEach(country -> {
            double exchange = flowOnMerchantLinesPerCountry.getOrDefault(country.getName(), 0.);
            convertedFlowOnMerchantLinesPerCountry.put(country.getEiCode(), exchange);
        });
        return convertedFlowOnMerchantLinesPerCountry;
    }

    private static String toEic(String country) {
        return new EICode(Country.valueOf(country)).getAreaCode();
    }

    private double getTTC(DichotomyStepResult<RaoResponse> dichotomyStepResult) throws IOException {
        Network network = fileImporter.importNetwork(dichotomyStepResult.getValidationData().getNetworkWithPraFileUrl());
        return BorderExchanges.computeItalianImport(network);
    }

    private String getLimitingElement(DichotomyStepResult<RaoResponse> dichotomyStepResult) throws IOException {
        double worstMargin = Double.MAX_VALUE;
        Optional<FlowCnec> worstCnec = Optional.empty();
        Crac crac = fileImporter.importCracFromJson(dichotomyStepResult.getValidationData().getCracFileUrl());
        RaoResult raoResult = fileImporter.importRaoResult(dichotomyStepResult.getValidationData().getRaoResultFileUrl(), crac);
        for (FlowCnec flowCnec : crac.getFlowCnecs()) {
            double margin = computeFlowMargin(raoResult, flowCnec);
            if (margin < worstMargin) {
                worstMargin = margin;
                worstCnec = Optional.of(flowCnec);
            }
        }

        return worstCnec.orElseThrow(() -> new CseDataException("Exception occurred while retrieving the most limiting element in preventive state.")).getName();
    }

    private double computeFlowMargin(RaoResult raoResult, FlowCnec flowCnec) {
        if (flowCnec.getState().getInstant() == Instant.CURATIVE) {
            return raoResult.getMargin(OptimizationState.AFTER_CRA, flowCnec, Unit.AMPERE);
        } else {
            return raoResult.getMargin(OptimizationState.AFTER_PRA, flowCnec, Unit.AMPERE);
        }
    }
}
