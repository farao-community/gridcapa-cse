/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.data.ttc_res.TtcResult;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.import_runner.app.configurations.ProcessConfiguration;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.powsybl.iidm.network.Network;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class MultipleDichotomyRunner {
    public static final String SUMMARY = "Summary :  " +
        "Progress in the forcing : {},  " +
        "MNII : {} MW,  " +
        "Limiting cause : {},  " +
        "Limiting element : {},  " +
        "PRAs : {},  " +
        "fPRA : {}.";
    private static final String DICHOTOMY_COUNT = "Dichotomy count: ";

    private final ProcessConfiguration processConfiguration;
    private final DichotomyRunner dichotomyRunner;
    private final DichotomyResultHelper dichotomyResultHelper;
    private final Logger businessLogger;

    public MultipleDichotomyRunner(ProcessConfiguration processConfiguration, DichotomyRunner dichotomyRunner,
                                   DichotomyResultHelper dichotomyResultHelper, Logger logger) {
        this.processConfiguration = processConfiguration;
        this.dichotomyRunner = dichotomyRunner;
        this.dichotomyResultHelper = dichotomyResultHelper;
        this.businessLogger = logger;
    }

    public MultipleDichotomyResult<DichotomyRaoResponse> runMultipleDichotomy(CseRequest request,
                                                                              CseData cseData,
                                                                              Network network,
                                                                              Crac crac,
                                                                              double initialIndexValue,
                                                                              Map<String, Double> referenceExchanges) throws IOException {

        int maximumDichotomiesNumber = Optional.ofNullable(request.getMaximumDichotomiesNumber()).orElse(processConfiguration.getDefaultMaxDichotomiesNumber());
        Map<String, List<Set<String>>> automatedForcedPrasIds = request.getAutomatedForcedPrasIds();
        MultipleDichotomyResult<DichotomyRaoResponse> multipleDichotomyResult = new MultipleDichotomyResult<>();
        List<Set<String>> forcedPrasIds = new ArrayList<>();
        forcedPrasIds.add(new HashSet<>(request.getManualForcedPrasIds()));

        // Launch initial dichotomy and store result
        DichotomyResult<DichotomyRaoResponse> initialDichotomyResult =
            dichotomyRunner.runDichotomy(request, cseData, network, initialIndexValue, referenceExchanges, flattenPrasIds(forcedPrasIds));
        multipleDichotomyResult.addResult(initialDichotomyResult, flattenPrasIds(forcedPrasIds));

        String limitingElement = "NONE";
        String ttcString = "NONE";
        String printablePrasIds = "NONE";
        String printableForcedPrasIds = "NONE";
        String limitingCause = TtcResult.limitingCauseToString(initialDichotomyResult.getLimitingCause());

        if (initialDichotomyResult.hasValidStep()) {
            limitingElement = dichotomyResultHelper.getLimitingElement(multipleDichotomyResult.getBestDichotomyResult());
            ttcString = String.valueOf(round(dichotomyResultHelper.computeHighestSecureItalianImport(initialDichotomyResult)));
            printablePrasIds = toString(getActivatedRangeActionInPreventive(crac, initialDichotomyResult));
            printableForcedPrasIds = toString(getForcedPrasIds(initialDichotomyResult));
        }

        int dichotomyCount = 1;

        logSummary(DICHOTOMY_COUNT + dichotomyCount,
            ttcString,
            limitingCause, limitingElement,
            printablePrasIds,
            printableForcedPrasIds);

        if (automatedForcedPrasIds.isEmpty() || !multipleDichotomyResult.getBestDichotomyResult().hasValidStep()) {
            return multipleDichotomyResult;
        }

        dichotomyCount++;
        int counterPerLimitingElement = 0;
        Set<String> additionalPrasToBeForced = getAdditionalPrasToBeForced(automatedForcedPrasIds, limitingElement, counterPerLimitingElement);

        while (dichotomyCount <= maximumDichotomiesNumber && !additionalPrasToBeForced.isEmpty()) {
            if (!checkIfPrasCombinationHasImpactOnNetwork(additionalPrasToBeForced, crac, network)) {
                if (businessLogger.isInfoEnabled()) {
                    logSummary(DICHOTOMY_COUNT + dichotomyCount,
                        "Additional forced PRAs have no impact on the network",
                        toString(CollectionUtils.union(flattenPrasIds(forcedPrasIds), additionalPrasToBeForced)));
                }
                counterPerLimitingElement++;
            } else {
                double lastUnsecureItalianImport = dichotomyResultHelper.computeLowestUnsecureItalianImport(multipleDichotomyResult.getBestDichotomyResult());
                forcedPrasIds.add(additionalPrasToBeForced); // We add the new forced PRAs to the historical register of the forced PRAs

                // We launch a new dichotomy still based on initial network but with a higher starting index -- previous unsecure index.
                // As we already computed a reference TTC we tweak the index so that it doesn't go below the starting
                // index -- if so we can just consider the previous result.
                // We pass to the dichotomy all the forced PRAs we want to apply including manual forced PRAs
                DichotomyResult<DichotomyRaoResponse> nextDichotomyResult = dichotomyRunner.runDichotomy(
                    request,
                    cseData,
                    network,
                    lastUnsecureItalianImport,
                    lastUnsecureItalianImport,
                    referenceExchanges,
                    flattenPrasIds(forcedPrasIds));

                if (nextDichotomyResult.hasValidStep()) {
                    String newLimitingElement = dichotomyResultHelper.getLimitingElement(nextDichotomyResult);
                    double previousLowestUnsecureItalianImport =
                        dichotomyResultHelper.computeLowestUnsecureItalianImport(multipleDichotomyResult.getBestDichotomyResult());
                    double newLowestUnsecureItalianImport = dichotomyResultHelper.computeLowestUnsecureItalianImport(nextDichotomyResult);

                    limitingCause = TtcResult.limitingCauseToString(nextDichotomyResult.getLimitingCause());
                    ttcString = String.valueOf(round(dichotomyResultHelper.computeHighestSecureItalianImport(nextDichotomyResult)));
                    printablePrasIds = toString(getActivatedRangeActionInPreventive(crac, nextDichotomyResult));
                    printableForcedPrasIds = toString(getForcedPrasIds(nextDichotomyResult));
                    logSummary(DICHOTOMY_COUNT + dichotomyCount,
                        ttcString,
                        limitingCause, newLimitingElement,
                        printablePrasIds, printableForcedPrasIds);

                    if (previousLowestUnsecureItalianImport < newLowestUnsecureItalianImport) {
                        // If result is improved we store this new result
                        multipleDichotomyResult.addResult(nextDichotomyResult, flattenPrasIds(forcedPrasIds));
                        businessLogger.info("New TTC '{}' is higher than previous TTC '{}'. Result will be kept", newLowestUnsecureItalianImport, previousLowestUnsecureItalianImport);
                        if (limitingElement.equals(newLimitingElement)) {
                            // If limiting element remains the same we remove the last tested combination and go to the next one
                            businessLogger.info("The limiting element '{}' didn't change after the last dichotomy. Next RAs combination will be tried", limitingElement);
                            forcedPrasIds.remove(forcedPrasIds.size() - 1); // Remove from the historical forced PRAs to apply the last we tried.
                            counterPerLimitingElement++;
                        } else {
                            // If not, we keep the current combination and follow the next ones according to the new limiting element
                            businessLogger.info("The limiting element '{}' changed after the last dichotomy. New limiting element is '{}'", limitingElement, newLimitingElement);
                            limitingElement = newLimitingElement;
                            counterPerLimitingElement = 0;
                        }
                    } else {
                        // If the result is not improved we don't store the result, keep the previous limiting element and goes to the next combination for this limiting element.
                        // For that we don't even consider if the limiting element has changed or not, as the reference remains the previous case
                        businessLogger.info("New TTC '{}' is lower or equal than previous TTC '{}'. Result will be ignored", newLowestUnsecureItalianImport, previousLowestUnsecureItalianImport);
                        forcedPrasIds.remove(forcedPrasIds.size() - 1); // Remove from the historical forced PRAs to apply the last we tried.
                        counterPerLimitingElement++;
                    }
                } else {
                    logSummary(DICHOTOMY_COUNT + dichotomyCount,
                        TtcResult.limitingCauseToString(nextDichotomyResult.getLimitingCause()),
                        toString(flattenPrasIds(forcedPrasIds)));
                    forcedPrasIds.remove(forcedPrasIds.size() - 1); // Remove from the historical forced PRAs to apply the last we tried.
                    counterPerLimitingElement++;
                }
            }
            additionalPrasToBeForced = getAdditionalPrasToBeForced(automatedForcedPrasIds, limitingElement, counterPerLimitingElement);
            dichotomyCount++;
        }

        String finalLimitingElement = "NONE";
        String finalTtcString = "NONE";
        String finalPrintablePrasIds = "NONE";
        String finalPrintableForcedPrasIds = "NONE";
        String finalLimitingCause = TtcResult.limitingCauseToString(multipleDichotomyResult.getBestDichotomyResult().getLimitingCause());

        if (multipleDichotomyResult.getBestDichotomyResult().hasValidStep()) {
            finalLimitingElement = dichotomyResultHelper.getLimitingElement(multipleDichotomyResult.getBestDichotomyResult());
            finalTtcString = String.valueOf(round(dichotomyResultHelper.computeHighestSecureItalianImport(multipleDichotomyResult.getBestDichotomyResult())));
            finalPrintablePrasIds = toString(getActivatedRangeActionInPreventive(crac, multipleDichotomyResult.getBestDichotomyResult()));
            finalPrintableForcedPrasIds = toString(getForcedPrasIds(multipleDichotomyResult.getBestDichotomyResult()));
        }

        logSummary("Calculation finished",
            finalTtcString,
            finalLimitingCause, finalLimitingElement,
            finalPrintablePrasIds, finalPrintableForcedPrasIds);

        return multipleDichotomyResult;
    }

    private void logSummary(String dichotomyCount, String mniiString, String limitingCause, String limitingElement, String printablePrasIds, String printableForcedPrasIds) {
        businessLogger.info(SUMMARY,
            dichotomyCount,
            mniiString,
            limitingCause,
            limitingElement,
            printablePrasIds,
            printableForcedPrasIds);
    }

    private void logSummary(String dichotomyCount, String limitingCause, String printableForcedPrasIds) {
        businessLogger.info(SUMMARY,
            dichotomyCount,
            "NONE",
            limitingCause,
            "NONE",
            "NONE",
            printableForcedPrasIds);
    }

    private static Set<String> getAdditionalPrasToBeForced(Map<String, List<Set<String>>> automatedForcedPrasIds, String limitingElement, int counterPerLimitingElement) {
        List<Set<String>> forcedPrasForLimitingElement = Optional
            .ofNullable(automatedForcedPrasIds.get(limitingElement))
            .orElse(automatedForcedPrasIds.get("default"));

        if (forcedPrasForLimitingElement == null || counterPerLimitingElement >= forcedPrasForLimitingElement.size()) {
            return Collections.emptySet();
        }

        return new HashSet<>(forcedPrasForLimitingElement.get(counterPerLimitingElement));
    }

    private static boolean checkIfPrasCombinationHasImpactOnNetwork(Set<String> raToBeForced, Crac crac, Network network) {
        // If one elementary action of the network action has an impact on the network then the network action has an impact on the network
        // We don't check availability here as in any case it won't be tested against the proper level of exchanges
        return raToBeForced.stream()
            .map(crac::getNetworkAction)
            .filter(Objects::nonNull)
            .anyMatch(na -> na.hasImpactOnNetwork(network));
    }

    private List<String> getActivatedRangeActionInPreventive(Crac crac, DichotomyResult<DichotomyRaoResponse> dichotomyResult) {
        if (dichotomyResult.hasValidStep() && dichotomyResult.getHighestValidStep().getRaoResult() != null) {
            List<String> prasNames = dichotomyResult.getHighestValidStep().getRaoResult().getActivatedNetworkActionsDuringState(crac.getPreventiveState()).stream().map(NetworkAction::getName).collect(Collectors.toList());
            prasNames.addAll(dichotomyResult.getHighestValidStep().getRaoResult().getActivatedRangeActionsDuringState(crac.getPreventiveState()).stream().map(RangeAction::getName).collect(Collectors.toList()));
            return prasNames;
        } else {
            return Collections.emptyList();
        }
    }

    private Set<String> getForcedPrasIds(DichotomyResult<DichotomyRaoResponse> dichotomyResult) {
        if (dichotomyResult.hasValidStep() && dichotomyResult.getHighestValidStep().getValidationData() != null) {
            return dichotomyResult.getHighestValidStep().getValidationData().getForcedPrasIds();
        } else {
            return Collections.emptySet();
        }
    }

    private static Set<String> flattenPrasIds(List<Set<String>> prasIds) {
        return prasIds.stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    private static double round(double value) {
        double scale = Math.pow(10, 2);
        return Math.round(value * scale) / scale;
    }

    private static String toString(Collection<String> c) {
        return c.stream().map(Object::toString).collect(Collectors.joining(","));
    }
}
