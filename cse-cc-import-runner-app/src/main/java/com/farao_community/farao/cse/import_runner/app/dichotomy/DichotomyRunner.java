/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.cse.computation.BorderExchanges;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.import_runner.app.services.FileExporter;
import com.farao_community.farao.cse.import_runner.app.services.FileImporter;
import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.dichotomy.api.DichotomyEngine;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.index.BiDirectionalStepsIndexStrategy;
import com.farao_community.farao.dichotomy.api.index.Index;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.shift.LinearScaler;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class DichotomyRunner {
    private static final double SHIFT_TOLERANCE = 1;
    private static final double MIN_IMPORT_VALUE = 0;
    private static final double MAX_IMPORT_VALUE = 19999;
    private static final String DICHOTOMY_PARAMETERS_MSG = "Starting dichotomy index: {}, Minimum dichotomy index: {}, Maximum dichotomy index: {}, Initial dichotomy step: {}, Dichotomy precision: {}";

    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final RaoRunnerClient raoRunnerClient;
    private final Logger logger;

    public DichotomyRunner(FileExporter fileExporter, FileImporter fileImporter, RaoRunnerClient raoRunnerClient, Logger logger) {
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.raoRunnerClient = raoRunnerClient;
        this.logger = logger;
    }

    public DichotomyResult<RaoResponse> runDichotomy(CseRequest cseRequest,
                                                     CseData cseData,
                                                     Network network,
                                                     double initialItalianImport) throws IOException {
        double initialIndexValue = Optional.ofNullable(cseRequest.getInitialDichotomyIndex()).orElse(initialItalianImport);
        double initialDichotomyStep = cseRequest.getInitialDichotomyStep();
        double dichotomyPrecision = cseRequest.getDichotomyPrecision();
        logger.info(DICHOTOMY_PARAMETERS_MSG, (int) initialIndexValue, MIN_IMPORT_VALUE, MAX_IMPORT_VALUE, (int) initialDichotomyStep, (int) dichotomyPrecision);
        Index<RaoResponse> index = new Index<>(MIN_IMPORT_VALUE, MAX_IMPORT_VALUE, dichotomyPrecision);
        DichotomyEngine<RaoResponse> engine = new DichotomyEngine<>(
            index,
            new BiDirectionalStepsIndexStrategy(initialIndexValue, initialDichotomyStep),
            getNetworkShifter(cseRequest, cseData, network),
            getNetworkValidator(cseRequest, cseData));
        return engine.run(network);
    }

    private NetworkShifter getNetworkShifter(CseRequest request,
                                             CseData cseData,
                                             Network network) throws IOException {
        return new LinearScaler(
            getZonalScalable(request.getMergedGlskUrl(), network, request.getProcessType()),
            getShiftDispatcher(request.getProcessType(), cseData, network),
            SHIFT_TOLERANCE);
    }

    ZonalData<Scalable> getZonalScalable(String mergedGlskUrl, Network network, ProcessType processType) throws IOException {
        ZonalData<Scalable> zonalScalable = fileImporter.importGlsk(mergedGlskUrl, network);
        Arrays.stream(CseCountry.values()).forEach(country -> checkCseCountryInGlsk(zonalScalable, country));
        stackScalableOnLoads(network, zonalScalable, processType);
        return zonalScalable;
    }

    private void stackScalableOnLoads(Network network, ZonalData<Scalable> zonalScalable, ProcessType processType) {
        zonalScalable.getDataPerZone().forEach((zone, scalable) -> {
            if (processType == ProcessType.IDCC && zone.equals(CseCountry.IT.getEiCode())) {
                return;
            }
            double sum = getZoneSumOfActiveLoads(network, zone);
            // No need to go further if a country has no active load
            if (sum == 0.0) {
                return;
            }
            Scalable stackedScalable = getStackedScalable(zone, scalable, network, sum);
            zonalScalable.getDataPerZone().put(zone, stackedScalable);
        });
    }

    private double getZoneSumOfActiveLoads(Network network, String zone) {
        return network.getLoadStream()
            .filter(load -> isLoadCorrespondingToTheZone(load, zone))
            .map(Load::getP0)
            .reduce(0., Double::sum);
    }

    private boolean isLoadCorrespondingToTheZone(Load load, String zone) {
        return load.getTerminal().getVoltageLevel().getSubstation()
            .flatMap(Substation::getCountry)
            .map(Country::toString)
            .map(country -> CseCountry.valueOf(country).getEiCode().equals(zone))
            .orElse(false);
    }

    private Scalable getStackedScalable(String zone, Scalable scalable, Network network, double sum) {
        List<Scalable> listScalables = new ArrayList<>();
        List<Float> listPercentages = new ArrayList<>();

        for (Load load : network.getLoads()) {
            if (isLoadCorrespondingToTheZone(load, zone)) {
                listPercentages.add((float) (load.getP0() / sum) * 100);
                listScalables.add(Scalable.onLoad(load.getId()));
            }
        }
        return Scalable.stack(scalable, Scalable.proportional(listPercentages, listScalables));
    }

    private static void checkCseCountryInGlsk(ZonalData<Scalable> zonalScalable, CseCountry country) {
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

    private NetworkValidator<RaoResponse> getNetworkValidator(CseRequest request, CseData cseData) {
        return new RaoRunnerValidator(
            request.getProcessType(),
            request.getId(),
            request.getTargetProcessDateTime(),
            cseData.getJsonCracUrl(),
            fileExporter.saveRaoParameters(request.getTargetProcessDateTime(), request.getProcessType()),
            raoRunnerClient,
            fileExporter,
            fileImporter);
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
