/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.cse.computation.BorderExchanges;
import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.import_runner.app.services.FileImporter;
import com.farao_community.farao.cse.import_runner.app.services.ForcedPrasHandler;
import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.data.crac_api.Crac;
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
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class MultipleDichotomyRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleDichotomyRunner.class);
    private static final double SHIFT_TOLERANCE = 1;
    private static final int MAX_DICHOTOMY_NUMBER = 6; //TODO: to be defined as config parameter

    private final DichotomyRunner dichotomyRunner;
    private final FileImporter fileImporter;
    private final ForcedPrasHandler forcedPrasHandler;

    public MultipleDichotomyRunner(DichotomyRunner dichotomyRunner, FileImporter fileImporter, ForcedPrasHandler forcedPrasHandler) {
        this.dichotomyRunner = dichotomyRunner;
        this.fileImporter = fileImporter;
        this.forcedPrasHandler = forcedPrasHandler;
    }

    public MultipleDichotomyResult runMultipleDichotomy(CseRequest request,
                                                        CseData cseData,
                                                        Network network,
                                                        Crac crac,
                                                        List<String> manualForcedPrasIds,
                                                        Map<String, List<Set<String>>> automatedForcedPrasIds,
                                                        double initialItalianImport) throws IOException {
        MultipleDichotomyResult multipleDichotomyResult = new MultipleDichotomyResult();

        // Shift first before evaluating the applicability of forced PRAs (maybe it will require to run a loadflow after that)
        NetworkShifter networkShifter = getNetworkShifter(request, cseData, network);

        try {
            networkShifter.shiftNetwork(initialItalianImport, network);
        } catch (GlskLimitationException e) {
            // Handle errors properly
            e.printStackTrace();
        } catch (ShiftingException e) {
            // Handle errors properly
            e.printStackTrace();
        }

        // Force manual PRAs if specified
        forcedPrasHandler.forcePras(manualForcedPrasIds, network, crac);

        // Launch initial dichotomy and store result
        DichotomyResult<RaoResponse> initialDichotomyResult = dichotomyRunner.runDichotomy(request, cseData, network, networkShifter, initialItalianImport);
        multipleDichotomyResult.addResult(initialDichotomyResult, new HashSet<>(manualForcedPrasIds));

        int dichotomyCount = 1;
        String limitingElement;
        Map<String, Integer> counterPerLimitingElement = new HashMap<>();

        while (dichotomyCount <= MAX_DICHOTOMY_NUMBER) {
            limitingElement = multipleDichotomyResult.getLimitingElement();

            List<Set<String>> forcedPras = Optional
                .ofNullable(automatedForcedPrasIds.get(limitingElement))
                .orElse(automatedForcedPrasIds.get("default"));
            if (forcedPras == null) {
                // Limiting element not in the list and no default list of forced PRAs
                return multipleDichotomyResult;
            }

            Set<String> raToBeForced = forcedPras.get(counterPerLimitingElement.get(limitingElement));
            try {
                forcedPrasHandler.forcePras(new ArrayList<>(raToBeForced), network, crac);
            } catch (CseDataException e) { // Set a more specific exception
                LOGGER.info("No need to run for this config", e);
                // Go to next forced pra set for this limiting element
                counterPerLimitingElement.put(limitingElement, counterPerLimitingElement.get(limitingElement) + 1);
                continue;
            }

            Network bestNetwork = fileImporter.importNetwork(multipleDichotomyResult.getBestNetworkUrl());
            DichotomyResult<RaoResponse> nextDichotomyResult = dichotomyRunner.runDichotomy(request, cseData, bestNetwork, networkShifter, initialItalianImport);

            if (multipleDichotomyResult.getHighestTTC() < MultipleDichotomyResult.getTTC(nextDichotomyResult.getHighestValidStep())) {
                multipleDichotomyResult.addResult(nextDichotomyResult, raToBeForced);
            }
            if (limitingElement.equals(MultipleDichotomyResult.getLimitingElement(nextDichotomyResult))) {
                counterPerLimitingElement.put(limitingElement, counterPerLimitingElement.get(limitingElement) + 1);
            }
            dichotomyCount++;
        }
        return multipleDichotomyResult;
    }

    /*
        Stores the succession of DichotomyResults when it improves the TTC
     */
    public static final class MultipleDichotomyResult {
        List<Pair<Set<String>, DichotomyResult<RaoResponse>>> dichotomyHistory = new ArrayList<>();

        // Stores a new increasing TTC dichotomy result alongside the set of forced PRAs to keep track of them
        public void addResult(DichotomyResult<RaoResponse> initialDichotomyResult, Set<String> forcedPras) {
            dichotomyHistory.add(Pair.of(forcedPras, initialDichotomyResult));
        }

        public String getLimitingElement() {
            return getLimitingElement(getBestDichotomyResult());
        }

        public double getHighestTTC() {
            return getTTC(getBestDichotomyResult().getHighestValidStep());
        }

        public String getBestNetworkUrl() {
            return dichotomyHistory.get(dichotomyHistory.size() - 1).getRight().getHighestValidStep().getValidationData().getNetworkWithPraFileUrl();
        }

        public DichotomyResult<RaoResponse> getBestDichotomyResult() {
            return dichotomyHistory.get(dichotomyHistory.size() - 1).getRight();
        }

        private static double getTTC(DichotomyStepResult<RaoResponse> highestValidStep) {
            return 0.;
        }

        private static String getLimitingElement(DichotomyResult<RaoResponse> nextDichotomyResult) {
            return null;
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
}
