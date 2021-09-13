/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.ntc;

import com.farao_community.farao.cse.data.TLine;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class Ntc {
    private final YearlyNtcDocument yearlyNtcDocument;
    private final DailyNtcDocument dailyNtcDocument;

    public static Ntc create(OffsetDateTime targetDateTime,
                             InputStream ntcAnnualInputStream,
                             InputStream ntcReductionsInputStream) throws JAXBException {
        return new Ntc(
                YearlyNtcDocument.create(targetDateTime, ntcAnnualInputStream),
                DailyNtcDocument.create(targetDateTime, ntcReductionsInputStream)
        );
    }

    private Ntc(YearlyNtcDocument yearlyNtcDocument, DailyNtcDocument dailyNtcDocument) {
        this.yearlyNtcDocument = yearlyNtcDocument;
        this.dailyNtcDocument = dailyNtcDocument;
    }

    public Double computeMniiOffset() {
        Map<String, Double> flowOnNotModeledLinesPerCountry = getFlowPerCountry(Predicate.not(TLine::isModelized));
        return flowOnNotModeledLinesPerCountry.values().stream().reduce(0., Double::sum);
    }

    public Map<String, Double> computeReducedSplittingFactors() {
        Map<String, Double> flowOnMerchantLinesPerCountry = getFlowPerCountry(TLine::isMerchantLine);
        Map<String, Double> ntcPerCountry = getNtcPerCountry();
        Double totalNtc = ntcPerCountry.values().stream().reduce(0., Double::sum);
        Double totalFlowOnMerchantLines = flowOnMerchantLinesPerCountry.values().stream().reduce(0., Double::sum);
        return getReducedSplittingFactors(ntcPerCountry, flowOnMerchantLinesPerCountry, totalNtc, totalFlowOnMerchantLines);
    }

    public Map<String, Double> getFlowOnFixedFlowLines() {
        Predicate<TLine> fixedFlowLines = tLine -> tLine.isFixedFlow() && tLine.isModelized();
        Map<String, LineInformation> yearlyLineInformationPerLineId = yearlyNtcDocument.getLineInformationPerLineId(fixedFlowLines);
        Map<String, LineInformation> dailyLineInformationPerLineId = dailyNtcDocument.getLineInformationPerLineId(fixedFlowLines);
        return getFlowPerLineId(yearlyLineInformationPerLineId, dailyLineInformationPerLineId);
    }

    Map<String, Double> getFlowPerCountry(Predicate<TLine> lineSelector) {
        Map<String, LineInformation> yearlyLineInformationPerLineId = yearlyNtcDocument.getLineInformationPerLineId(lineSelector);
        Map<String, LineInformation> dailyLineInformationPerLineId = dailyNtcDocument.getLineInformationPerLineId(lineSelector);
        Map<String, Double> flowPerLineId = getFlowPerLineId(yearlyLineInformationPerLineId, dailyLineInformationPerLineId);

        Map<String, Double> flowPerCountry = new HashMap<>();
        flowPerLineId.forEach((lineId, flow) -> {
            String country = Optional.ofNullable(yearlyLineInformationPerLineId.get(lineId))
                    .map(LineInformation::getCountry)
                    .orElseGet(() -> dailyLineInformationPerLineId.get(lineId).getCountry());
            double initialFlow = Optional.ofNullable(flowPerCountry.get(country)).orElse(0.);
            flowPerCountry.put(country, initialFlow + flow);
        });
        return flowPerCountry;
    }

    Map<String, Double> getNtcPerCountry() {
        Map<String, Double> ntcPerCountry = yearlyNtcDocument.getNtcInformationPerCountry().entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().getFlow()
                ));
        dailyNtcDocument.getNtcInformationPerCountry().forEach((country, ntcInformation) -> {
            if (ntcInformation.getVariationType().equalsIgnoreCase(NtcUtil.ABSOLUTE)) {
                ntcPerCountry.put(country, ntcInformation.getFlow());
            } else {
                ntcPerCountry.put(country, ntcPerCountry.get(country) + ntcInformation.getFlow());
            }
        });
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

    private static Map<String, Double> getReducedSplittingFactors(Map<String, Double> ntcPerCountry,
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
