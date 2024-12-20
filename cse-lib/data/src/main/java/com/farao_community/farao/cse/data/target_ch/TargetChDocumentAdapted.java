/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.target_ch;

import com.farao_community.farao.cse.data.DataUtil;

import com.farao_community.farao.cse.data.DateTimeUtil;
import com.farao_community.farao.cse.data.xsd.target_ch_adapted.*;

import jakarta.xml.bind.JAXBException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.*;

import static com.farao_community.farao.cse.data.DataUtil.toOptional;
/*
* An adapted import process will temporarily coexist with the old import
* and the two xsd target CH files are different
* */
final class TargetChDocumentAdapted {
    private final TargetsDocument targetsDocument;

    static TargetChDocumentAdapted create(InputStream targetChInputStream) throws JAXBException {
        return new TargetChDocumentAdapted(DataUtil.unmarshalFromInputStream(targetChInputStream, TargetsDocument.class));
    }

    private TargetChDocumentAdapted(TargetsDocument targetsDocument) {
        this.targetsDocument = targetsDocument;
    }

    Map<String, List<OutageInformation>> getOutagesInformationPerLineId(OffsetDateTime targetDateTime) {
        Map<String, List<OutageInformation>> outagesInformationPerLineId = new HashMap<>();
        Set<TLine> lines = getLines(targetsDocument);
        lines.forEach(line -> addOutagesInformation(outagesInformationPerLineId, targetDateTime, line));
        return outagesInformationPerLineId;
    }

    private static Set<TLine> getLines(TargetsDocument targetsDocument) {
        List<TTargetDataCHIT> tTargetData = targetsDocument.getTargetDataCHIT();
        Set<TLine> lines = new HashSet<>();
        tTargetData.forEach(data -> {
            TSpecialLines tSpecialLines = data.getSpecialLines();
            if (tSpecialLines != null && tSpecialLines.getLine() != null) {
                lines.add(tSpecialLines.getLine());
            }
        });
        return lines;
    }

    private void addOutagesInformation(Map<String, List<OutageInformation>> outagesInformationPerLineId, OffsetDateTime targetDateTime, TLine tLine) {
        List<OutageInformation> outagesInformation = new ArrayList<>();
        getMatchingPeriod(targetDateTime, tLine.getPeriod())
                .map(TPeriod::getOutages)
                .ifPresent(tOutages -> tOutages.getOutage().forEach(tOutage ->
                        addOutageInformation(outagesInformation, targetDateTime, tOutage)));
        outagesInformationPerLineId.put(tLine.getCode(), outagesInformation);
    }

    private void addOutageInformation(List<OutageInformation> outagesInformation, OffsetDateTime targetDateTime, TOutage tOutage) {
        getFixedFlowFromDaysOfWeek(targetDateTime, tOutage.getDayOfWeek()).stream()
                .collect(toOptional())
                .ifPresent(fixedFlow -> outagesInformation.add(new OutageInformation(tOutage, fixedFlow)));
    }

    private Optional<TPeriod> getMatchingPeriod(OffsetDateTime targetDateTime, List<TPeriod> tPeriods) {
        return tPeriods.stream()
                .filter(tPeriod -> DateTimeUtil.isTargetDateInInterval(targetDateTime, tPeriod.getTini(), tPeriod.getTfin()))
                .collect(toOptional());
    }

    private Optional<Double> getFixedFlowFromDaysOfWeek(OffsetDateTime targetDateTime, List<TDayOfWeek> tDayOfWeeks) {
        return tDayOfWeeks.stream()
                .filter(tDayOfWeek -> TargetChUtil.isTargetDayOfWeekMatchWithDayNum(tDayOfWeek.getDaynum(), targetDateTime.getDayOfWeek().getValue()))
                .filter(tDayOfWeek -> getFixedFlowFromTimeIntervals(targetDateTime, tDayOfWeek.getTimeInterval()).isPresent())
                .collect(toOptional())
                .map(TDayOfWeek::getTimeInterval)
                .flatMap(tTimeIntervals -> getFixedFlowFromTimeIntervals(targetDateTime, tTimeIntervals));
    }

    private Optional<Double> getFixedFlowFromTimeIntervals(OffsetDateTime targetDateTime, List<TTimeInterval> tTimeIntervals) {
        return tTimeIntervals.stream()
                .filter(tTimeInterval -> DateTimeUtil.isTargetDateInInterval(targetDateTime, tTimeInterval.getTini(), tTimeInterval.getTfin()))
                .collect(toOptional())
                .map(TTimeInterval::getFixedFlow)
                .map(TFixedFlow::getValue)
                .map(Short::doubleValue);
    }

}
