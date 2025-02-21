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
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class Ntc {
    private final YearlyNtcDocument yearlyNtcDocument;
    private final DailyNtcDocument dailyNtcDocument;
    private final YearlyNtcDocumentAdapted yearlyNtcDocumentAdapted;
    private final DailyNtcDocumentAdapted dailyNtcDocumentAdapted;
    private final boolean isImportEcProcess;

    public Ntc(YearlyNtcDocument yearlyNtcDocument, DailyNtcDocument dailyNtcDocument, boolean isImportEcProcess) {
        this.yearlyNtcDocument = yearlyNtcDocument;
        this.dailyNtcDocument = dailyNtcDocument;
        this.yearlyNtcDocumentAdapted = null;
        this.dailyNtcDocumentAdapted = null;
        this.isImportEcProcess = isImportEcProcess;
    }

    public Ntc(YearlyNtcDocumentAdapted yearlyNtcDocumentAdapted, DailyNtcDocumentAdapted dailyNtcDocumentAdapted, boolean isImportEcProcess) {
        this.yearlyNtcDocument = null;
        this.dailyNtcDocument = null;
        this.yearlyNtcDocumentAdapted = yearlyNtcDocumentAdapted;
        this.dailyNtcDocumentAdapted = dailyNtcDocumentAdapted;
        this.isImportEcProcess = isImportEcProcess;
    }

    public Double computeMniiOffset() {
        Map<String, Double> flowOnNotModeledLinesPerCountry = isImportEcProcess ?
            getFlowPerCountryAdapted(Predicate.not(com.farao_community.farao.cse.data.xsd.ntc_adapted.TLine::isModelized)) :
            getFlowPerCountry(Predicate.not(TLine::isModelized));
        return flowOnNotModeledLinesPerCountry.values().stream().reduce(0., Double::sum);
    }

    public Map<String, Double> getFlowPerCountryOnMerchantLines() {
        return isImportEcProcess ?
            getFlowPerCountryAdapted(com.farao_community.farao.cse.data.xsd.ntc_adapted.TLine::isMerchantLine) :
            getFlowPerCountry(TLine::isMerchantLine);
    }

    public Map<String, Double> computeReducedSplittingFactors() {
        Map<String, Double> ntcsByCountry = getNtcPerCountry();
        Map<String, Double> flowOnMerchantLinesPerCountry = getFlowPerCountryOnMerchantLines();
        Double totalNtc = ntcsByCountry.values().stream().reduce(0., Double::sum);
        Double totalFlowOnMerchantLines = flowOnMerchantLinesPerCountry.values().stream().reduce(0., Double::sum);
        return getReducedSplittingFactors(ntcsByCountry, flowOnMerchantLinesPerCountry, totalNtc, totalFlowOnMerchantLines);
    }

    public Map<String, Double> getFlowOnFixedFlowLines() {
        if (isImportEcProcess) {
            Predicate<com.farao_community.farao.cse.data.xsd.ntc_adapted.TLine> fixedFlowLines = tLine -> tLine.isFixedFlow() && tLine.isModelized();
            Map<String, LineInformation> yearlyLineInformationPerLineId = yearlyNtcDocumentAdapted.getLineInformationPerLineId(fixedFlowLines);
            Map<String, LineInformation> dailyLineInformationPerLineId = dailyNtcDocumentAdapted != null ? dailyNtcDocumentAdapted.getLineInformationPerLineId(fixedFlowLines) : Map.of();
            return getFlowPerLineId(yearlyLineInformationPerLineId, dailyLineInformationPerLineId);
        } else {
            Predicate<TLine> fixedFlowLines = tLine -> tLine.isFixedFlow() && tLine.isModelized();
            Map<String, LineInformation> yearlyLineInformationPerLineId = yearlyNtcDocument.getLineInformationPerLineId(fixedFlowLines);
            Map<String, LineInformation> dailyLineInformationPerLineId = dailyNtcDocument != null ? dailyNtcDocument.getLineInformationPerLineId(fixedFlowLines) : Map.of();
            return getFlowPerLineId(yearlyLineInformationPerLineId, dailyLineInformationPerLineId);
        }
    }

    public Map<String, Double> getFlowPerCountryOnNotModelizedLines() {
        return isImportEcProcess ?
                getFlowPerCountryAdapted(t -> !t.isModelized()) :
                getFlowPerCountry(t -> !t.isModelized());
    }

    Map<String, Double> getFlowPerCountry(Predicate<TLine> lineSelector) {
        Map<String, LineInformation> yearlyLineInformationPerLineId = yearlyNtcDocument.getLineInformationPerLineId(lineSelector);
        Map<String, LineInformation> dailyLineInformationPerLineId = dailyNtcDocument != null ? dailyNtcDocument.getLineInformationPerLineId(lineSelector) : Map.of();
        Map<String, Double> flowPerLineId = getFlowPerLineId(yearlyLineInformationPerLineId, dailyLineInformationPerLineId);

        Map<String, Double> flowPerCountry = new HashMap<>();
        flowPerLineId.forEach((lineId, flow) -> {
            String country = Optional.ofNullable(yearlyLineInformationPerLineId.get(lineId))
                    .map(LineInformation::getCountry)
                    .orElseGet(() -> getCountryFromDailyLineInformation(dailyLineInformationPerLineId, lineId));
            double initialFlow = Optional.ofNullable(flowPerCountry.get(country)).orElse(0.);
            flowPerCountry.put(country, initialFlow + flow);
        });
        return flowPerCountry;
    }

    Map<String, Double> getFlowPerCountryAdapted(Predicate<com.farao_community.farao.cse.data.xsd.ntc_adapted.TLine> lineSelector) {
        Map<String, LineInformation> yearlyLineInformationPerLineId = yearlyNtcDocumentAdapted.getLineInformationPerLineId(lineSelector);
        Map<String, LineInformation> dailyLineInformationPerLineId = dailyNtcDocumentAdapted != null ? dailyNtcDocumentAdapted.getLineInformationPerLineId(lineSelector) : Map.of();
        Map<String, Double> flowPerLineId = getFlowPerLineId(yearlyLineInformationPerLineId, dailyLineInformationPerLineId);

        Map<String, Double> flowPerCountry = new HashMap<>();
        flowPerLineId.forEach((lineId, flow) -> {
            String country = Optional.ofNullable(yearlyLineInformationPerLineId.get(lineId))
                    .map(LineInformation::getCountry)
                    .orElseGet(() -> getCountryFromDailyLineInformation(dailyLineInformationPerLineId, lineId));
            double initialFlow = Optional.ofNullable(flowPerCountry.get(country)).orElse(0.);
            flowPerCountry.put(country, initialFlow + flow);
        });
        return flowPerCountry;
    }

    private String getCountryFromDailyLineInformation(Map<String, LineInformation> dailyLineInformationPerLineId, String lineId) {
        if (dailyLineInformationPerLineId.containsKey(lineId)) {
            return dailyLineInformationPerLineId.get(lineId).getCountry();
        } else {
            throw new CseInternalException(String.format("No information available for line %s", lineId));
        }
    }

    public Map<String, Double> getNtcPerCountry() {
        return isImportEcProcess ? getNtcPerCountryAdapted() : getNtcPerCountryNotAdapted();
    }

    private Map<String, Double> getNtcPerCountryNotAdapted() {
        Map<String, Double> ntcPerCountry = yearlyNtcDocument.getNtcInformationPerCountry().entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().getFlow()
                ));
        if (dailyNtcDocument != null) {
            dailyNtcDocument.getNtcInformationPerCountry().forEach((country, ntcInformation) -> {
                if (ntcInformation.getVariationType().equalsIgnoreCase(NtcUtil.ABSOLUTE)) {
                    ntcPerCountry.put(country, ntcInformation.getFlow());
                } else {
                    ntcPerCountry.put(country, ntcPerCountry.get(country) + ntcInformation.getFlow());
                }
            });
        }
        return ntcPerCountry;
    }

    private Map<String, Double> getNtcPerCountryAdapted() {
        Map<String, Double> ntcPerCountry = yearlyNtcDocumentAdapted.getNtcInformationPerCountry().entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().getFlow()
                ));
        if (dailyNtcDocumentAdapted != null) {
            dailyNtcDocumentAdapted.getNtcInformationPerCountry().forEach((country, ntcInformation) -> {
                if (ntcInformation.getVariationType().equalsIgnoreCase(NtcUtilAdapted.ABSOLUTE)) {
                    ntcPerCountry.put(country, ntcInformation.getFlow());
                } else {
                    ntcPerCountry.put(country, ntcPerCountry.get(country) + ntcInformation.getFlow());
                }
            });
        }
        return ntcPerCountry;
    }

    private static Map<String, Double> getFlowPerLineId(Map<String, LineInformation> yearlyLinePerId, Map<String, LineInformation> dailyLinePerId) {
        Map<String, Double> flowPerLine = yearlyLinePerId.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getFlow()
                ));
        dailyLinePerId.forEach((lineId, lineInformation) -> {
            if (lineInformation.getVariationType().equalsIgnoreCase(NtcUtil.ABSOLUTE)) {
                flowPerLine.put(lineId, lineInformation.getFlow());
            } else {
                double initialFlow = Optional.ofNullable(flowPerLine.get(lineId)).orElse(0.);
                flowPerLine.put(lineId, initialFlow + lineInformation.getFlow());
            }
        });
        return flowPerLine;
    }

    private static Map<String, Double>  getReducedSplittingFactors(Map<String, Double> ntcPerCountry,
                                                                  Map<String, Double> flowOnMerchantLinesPerCountry,
                                                                  Double totalNtc,
                                                                  Double totalFlowOnMerchantLines) {
        return ntcPerCountry.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                        Double flowOnMerchantLines = Optional
                                .ofNullable(flowOnMerchantLinesPerCountry.get(entry.getKey()))
                                .orElse(0.);
                        return (entry.getValue() - flowOnMerchantLines) / (totalNtc - totalFlowOnMerchantLines);
                    }));
    }
}
