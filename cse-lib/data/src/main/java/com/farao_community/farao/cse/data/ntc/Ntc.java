/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.ntc;

import com.farao_community.farao.cse.data.xsd.TLine;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

import static java.util.Collections.emptyMap;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;

public final class Ntc {
    private final YearlyNtcDocument yearlyNtcDocument;
    private final DailyNtcDocument dailyNtcDocument;
    private final YearlyNtcDocumentAdapted yearlyNtcDocumentAdapted;
    private final DailyNtcDocumentAdapted dailyNtcDocumentAdapted;
    private final boolean isImportEcProcess;

    public Ntc(final YearlyNtcDocument yearlyNtcDocument, final DailyNtcDocument dailyNtcDocument, final boolean isImportEcProcess) {
        this.yearlyNtcDocument = yearlyNtcDocument;
        this.dailyNtcDocument = dailyNtcDocument;
        this.yearlyNtcDocumentAdapted = null;
        this.dailyNtcDocumentAdapted = null;
        this.isImportEcProcess = isImportEcProcess;
    }

    public Ntc(final YearlyNtcDocumentAdapted yearlyNtcDocumentAdapted,
               final DailyNtcDocumentAdapted dailyNtcDocumentAdapted,
               final boolean isImportEcProcess) {
        this.yearlyNtcDocument = null;
        this.dailyNtcDocument = null;
        this.yearlyNtcDocumentAdapted = yearlyNtcDocumentAdapted;
        this.dailyNtcDocumentAdapted = dailyNtcDocumentAdapted;
        this.isImportEcProcess = isImportEcProcess;
    }

    public Double computeMniiOffset() {
        final Map<String, Double> flowOnNotModeledLinesByCountry = isImportEcProcess ?
            getFlowByCountryAdapted(not(com.farao_community.farao.cse.data.xsd.ntc_adapted.TLine::isModelized)) :
            getFlowByCountry(not(TLine::isModelized));
        return sumOfMapValues(flowOnNotModeledLinesByCountry);
    }

    public Map<String, Double> getMerchantFlowByCountry() {
        return isImportEcProcess ?
            getFlowByCountryAdapted(com.farao_community.farao.cse.data.xsd.ntc_adapted.TLine::isMerchantLine) :
            getFlowByCountry(TLine::isMerchantLine);
    }

    public Map<String, Double> getReducedSplittingFactors() {
        final Map<String, Double> ntcsByCountry = getNtcByCountry();
        final Map<String, Double> merchantFlowByCountry = getMerchantFlowByCountry();
        final Double totalNtc = sumOfMapValues(ntcsByCountry);

        return ntcsByCountry.entrySet().stream()
            .collect(toMap(
                Map.Entry::getKey,
                getReducedSplittingFactor(merchantFlowByCountry, totalNtc))
            );
    }

    private Double sumOfMapValues(final Map<String, Double> map) {
        return map.values().stream().reduce(0., Double::sum);

    }

    private Function<Map.Entry<String, Double>, Double> getReducedSplittingFactor(final Map<String, Double> merchantFlowByCountry,
                                                                                  final Double totalNtc) {
        final Double totalMerchantFlow = sumOfMapValues(merchantFlowByCountry);
        return ntcByCountry -> {
            final Double countryMerchantFlow = Optional
                .ofNullable(merchantFlowByCountry.get(ntcByCountry.getKey()))
                .orElse(0.);
            return (ntcByCountry.getValue() - countryMerchantFlow) / (totalNtc - totalMerchantFlow);
        };
    }

    public Map<String, Double> getFlowOnFixedFlowLines() {
        final Map<String, LineInformation> yearlyInfoById;
        final Map<String, LineInformation> dailyInfoById;

        if (isImportEcProcess) {
            final Predicate<com.farao_community.farao.cse.data.xsd.ntc_adapted.TLine> fixedFlowLines = tLine ->
                tLine.isFixedFlow() && tLine.isModelized();

            yearlyInfoById = yearlyNtcDocumentAdapted.getLineInformationById(fixedFlowLines);
            dailyInfoById = Optional.ofNullable(dailyNtcDocumentAdapted)
                .map(doc -> doc.getLineInformationById(fixedFlowLines))
                .orElse(Map.of());
        } else {
            final Predicate<TLine> fixedFlowLines = tLine -> tLine.isFixedFlow() && tLine.isModelized();
            yearlyInfoById = yearlyNtcDocument.getLineInformationById(fixedFlowLines);
            dailyInfoById = Optional.ofNullable(dailyNtcDocument)
                .map(doc -> doc.getLineInformationByLineId(fixedFlowLines))
                .orElse(Map.of());
        }

        return getFlowByLine(yearlyInfoById, dailyInfoById);
    }

    public Map<String, Double> getFlowByCountryOnNotModelizedLines() {
        return isImportEcProcess ?
            getFlowByCountryAdapted(t -> !t.isModelized()) :
            getFlowByCountry(t -> !t.isModelized());
    }

    Map<String, Double> getFlowByCountry(final Predicate<TLine> lineSelector) {
        final Map<String, LineInformation> yearlyInfoById = yearlyNtcDocument.getLineInformationById(lineSelector);
        final Map<String, LineInformation> dailyInfoById = Optional.ofNullable(dailyNtcDocument)
            .map(doc -> doc.getLineInformationByLineId(lineSelector))
            .orElse(Map.of());
        final Map<String, Double> lineFlowById = getFlowByLine(yearlyInfoById, dailyInfoById);

        return computeFlowByCountryMap(yearlyInfoById, dailyInfoById, lineFlowById);
    }

    Map<String, Double> getFlowByCountryAdapted(final Predicate<com.farao_community.farao.cse.data.xsd.ntc_adapted.TLine> lineSelector) {
        final Map<String, LineInformation> yearlyInfoById = yearlyNtcDocumentAdapted.getLineInformationById(lineSelector);
        final Map<String, LineInformation> dailyInfoById = Optional.ofNullable(dailyNtcDocumentAdapted)
            .map(doc -> doc.getLineInformationById(lineSelector))
            .orElse(Map.of());
        final Map<String, Double> lineFlowById = getFlowByLine(yearlyInfoById, dailyInfoById);

        return computeFlowByCountryMap(yearlyInfoById, dailyInfoById, lineFlowById);
    }

    private Map<String, Double> computeFlowByCountryMap(final Map<String, LineInformation> yearlyInfoById,
                                                        final Map<String, LineInformation> dailyInfoById,
                                                        final Map<String, Double> lineFlowById) {
        Map<String, Double> flowByCountry = new HashMap<>();

        lineFlowById
            .forEach((lineId, flowOnLine) -> {
                final String country = Optional.ofNullable(yearlyInfoById.get(lineId))
                    .map(LineInformation::getCountry)
                    .orElseGet(() -> getCountryFromDailyLineInformation(dailyInfoById, lineId));

                flowByCountry.put(country, flowByCountry.getOrDefault(country, 0.) + flowOnLine);
            });
        return flowByCountry;
    }

    private String getCountryFromDailyLineInformation(final Map<String, LineInformation> dailyInfoById, final String lineId) {
        if (dailyInfoById.containsKey(lineId)) {
            return dailyInfoById.get(lineId).getCountry();
        } else {
            throw new CseInternalException(String.format("No information available for line %s", lineId));
        }
    }

    public Map<String, Double> getNtcByCountry() {
        final Optional<Map<String, ? extends FlowInformation>> yearlyInfo;
        final Optional<Map<String, ? extends FlowInformation>> dailyInfo;

        if (isImportEcProcess) {
            yearlyInfo = Optional.ofNullable(yearlyNtcDocumentAdapted).map(YearlyNtcDocumentAdapted::getNtcInformationByCountry);
            dailyInfo = Optional.ofNullable(dailyNtcDocumentAdapted).map(DailyNtcDocumentAdapted::getNtcInformationByCountry);
        } else {
            yearlyInfo = Optional.ofNullable(yearlyNtcDocument).map(YearlyNtcDocument::getNtcInformationByCountry);
            dailyInfo = Optional.ofNullable(dailyNtcDocument).map(DailyNtcDocument::getNtcInformationByCountry);
        }

        return getNtcByCountry(yearlyInfo.orElse(emptyMap()), dailyInfo.orElse(emptyMap()));
    }

    private static Map<String, Double> getNtcByCountry(final Map<String, ? extends FlowInformation> yearlyInfo,
                                                       final Map<String, ? extends FlowInformation> dailyInfo) {
        final Map<String, Double> ntcByCountry = yearlyInfo
            .entrySet().stream()
            .collect(toFlowMap());

        return Optional.ofNullable(dailyInfo)
            .map(doc -> computeFlowSumByKey(ntcByCountry, dailyInfo))
            .orElse(emptyMap());
    }

    private static Map<String, Double> computeFlowSumByKey(final Map<String, Double> flowByKey,
                                                           final Map<String, ? extends FlowInformation> flowInfoByKey) {
        flowInfoByKey.forEach((key, flowInfo) -> {
            if (hasAbsoluteVariation(flowInfo)) {
                flowByKey.put(key, flowInfo.getFlow());
            } else {
                flowByKey.put(key, flowByKey.getOrDefault(key, 0.) + flowInfo.getFlow());
            }
        });

        return flowByKey;
    }

    private static Map<String, Double> getFlowByLine(final Map<String, LineInformation> yearlyInfoById,
                                                     final Map<String, LineInformation> dailyInfoById) {
        final Map<String, Double> flowByLine = yearlyInfoById
            .entrySet()
            .stream()
            .collect(toFlowMap());

        computeFlowSumByKey(flowByLine, dailyInfoById);
        return flowByLine;
    }

    private static boolean hasAbsoluteVariation(final FlowInformation flowInformation) {
        return flowInformation.getVariationType().equalsIgnoreCase(NtcUtil.ABSOLUTE);
    }

    private static Collector<Map.Entry<String, ? extends FlowInformation>, ?, Map<String, Double>> toFlowMap() {
        return toMap(Map.Entry::getKey, entry -> entry.getValue().getFlow());
    }
}
