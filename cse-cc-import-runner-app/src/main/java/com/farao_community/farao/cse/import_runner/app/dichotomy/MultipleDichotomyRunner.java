/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.powsybl.iidm.network.Network;
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
    private static final int DEFAULT_MAX_DICHOTOMIES_NUMBER = 1;

    private final DichotomyRunner dichotomyRunner;
    private final DichotomyResultHelper dichotomyResultHelper;
    private final Logger businessLogger;

    public MultipleDichotomyRunner(DichotomyRunner dichotomyRunner, DichotomyResultHelper dichotomyResultHelper, Logger logger) {
        this.dichotomyRunner = dichotomyRunner;
        this.dichotomyResultHelper = dichotomyResultHelper;
        this.businessLogger = logger;
    }

    public MultipleDichotomyResult<DichotomyRaoResponse> runMultipleDichotomy(CseRequest request,
                                                                              CseData cseData,
                                                                              Network network,
                                                                              Crac crac,
                                                                              double initialItalianImport) throws IOException {
        int maximumDichotomiesNumber = Optional.ofNullable(request.getMaximumDichotomiesNumber()).orElse(DEFAULT_MAX_DICHOTOMIES_NUMBER);
        Map<String, List<Set<String>>> automatedForcedPrasIds = request.getAutomatedForcedPrasIds();
        MultipleDichotomyResult<DichotomyRaoResponse> multipleDichotomyResult = new MultipleDichotomyResult<>();
        List<Set<String>> forcedPrasIds = new ArrayList<>();
        forcedPrasIds.add(new HashSet<>(request.getManualForcedPrasIds()));

        businessLogger.info(
            "Multiple dichotomy runs: Initial dichotomy running, Forcing following PRAs: {}",
            printablePrasIds(forcedPrasIds));

        // Launch initial dichotomy and store result
        DichotomyResult<DichotomyRaoResponse> initialDichotomyResult =
            dichotomyRunner.runDichotomy(request, cseData, network, initialItalianImport, flattenPrasIds(forcedPrasIds));
        multipleDichotomyResult.addResult(initialDichotomyResult, flattenPrasIds(forcedPrasIds));

        if (automatedForcedPrasIds.isEmpty() || !multipleDichotomyResult.getBestDichotomyResult().hasValidStep()) {
            return multipleDichotomyResult;
        }

        int dichotomyCount = 2;
        int counterPerLimitingElement = 0;
        String limitingElement = dichotomyResultHelper.getLimitingElement(multipleDichotomyResult.getBestDichotomyResult());
        Set<String> additionalPrasToBeForced = getAdditionalPrasToBeForced(automatedForcedPrasIds, limitingElement, counterPerLimitingElement);

        while (dichotomyCount <= maximumDichotomiesNumber && !additionalPrasToBeForced.isEmpty()) {
            if (!checkIfPrasCombinationHasImpactOnNetwork(additionalPrasToBeForced, crac, network)) {
                businessLogger.info("RAs combination '{}' has no impact on network. It will not be applied", printablePrasIds(additionalPrasToBeForced));
                counterPerLimitingElement++;
            } else {
                double lastUnsecureItalianImport = dichotomyResultHelper.computeLowestUnsecureItalianImport(multipleDichotomyResult.getBestDichotomyResult());
                forcedPrasIds.add(additionalPrasToBeForced); // We add the new forced PRAs to the historical register of the forced PRAs

                businessLogger.info(String.format(
                    "Multiple dichotomy runs: Dichotomy number is : %d, Starting Italian import : %.0f, Current limiting element is : %s, Forcing following PRAs: %s",
                    dichotomyCount, lastUnsecureItalianImport, limitingElement, printablePrasIds(forcedPrasIds)));

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
                    flattenPrasIds(forcedPrasIds));

                if (nextDichotomyResult.hasValidStep()) {
                    String newLimitingElement = dichotomyResultHelper.getLimitingElement(nextDichotomyResult);
                    double previousLowestUnsecureItalianImport =
                        dichotomyResultHelper.computeLowestUnsecureItalianImport(multipleDichotomyResult.getBestDichotomyResult());
                    double newLowestUnsecureItalianImport = dichotomyResultHelper.computeLowestUnsecureItalianImport(nextDichotomyResult);

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
                    businessLogger.info("No valid step computed in previous dichotomy. Result will be ignored");
                    forcedPrasIds.remove(forcedPrasIds.size() - 1); // Remove from the historical forced PRAs to apply the last we tried.
                    counterPerLimitingElement++;
                }
            }
            additionalPrasToBeForced = getAdditionalPrasToBeForced(automatedForcedPrasIds, limitingElement, counterPerLimitingElement);
            dichotomyCount++;
        }
        return multipleDichotomyResult;
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

    private static Set<String> flattenPrasIds(List<Set<String>> prasIds) {
        return prasIds.stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    private static String printablePrasIds(List<Set<String>> prasIds) {
        return flattenPrasIds(prasIds).stream().map(Object::toString).collect(Collectors.joining(","));
    }

    private static String printablePrasIds(Set<String> prasIds) {
        return prasIds.stream().map(Object::toString).collect(Collectors.joining(","));
    }

    private static boolean checkIfPrasCombinationHasImpactOnNetwork(Set<String> raToBeForced, Crac crac, Network network) {
        // If one elementary action of the network action has an impact on the network then the network action has an impact on the network
        // We don't check availability here as in any case it won't be tested against the proper level of exchanges
        return raToBeForced.stream()
            .map(crac::getNetworkAction)
            .filter(Objects::nonNull)
            .anyMatch(na -> na.hasImpactOnNetwork(network));
    }
}
