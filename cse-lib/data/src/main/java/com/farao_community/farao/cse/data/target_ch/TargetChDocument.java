/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.target_ch;

import com.farao_community.farao.cse.data.DataUtil;
import com.farao_community.farao.cse.data.xsd.target_ch.*;

import jakarta.xml.bind.JAXBException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.*;

import static com.farao_community.farao.cse.data.DataUtil.toOptional;

final class TargetChDocument {
    private final TargetsDocument targetsDocument;

    static TargetChDocument create(InputStream targetChInputStream) throws JAXBException {
        return new TargetChDocument(DataUtil.unmarshalFromInputStream(targetChInputStream, TargetsDocument.class));
    }

    private TargetChDocument(TargetsDocument targetsDocument) {
        this.targetsDocument = targetsDocument;
    }

    Map<String, List<OutageInformation>> getOutagesInformationPerLineId(OffsetDateTime targetDateTime) {
        Map<String, List<OutageInformation>> outagesInformationPerLineId = new HashMap<>();
        Set<TLine> lines = getLines(targetsDocument);
        lines.forEach(line -> addOutagesInformation(outagesInformationPerLineId, targetDateTime, line));
        return outagesInformationPerLineId;
    }

    private static Set<TLine> getLines(TargetsDocument targetsDocument) {
        List<TTargetData> tTargetData = targetsDocument.getTargetData();
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
        TargetChUtil.getMatchingPeriod(targetDateTime, tLine.getPeriod())
                .map(TPeriod::getOutages)
                .ifPresent(tOutages -> tOutages.getOutage().forEach(tOutage ->
                        addOutageInformation(outagesInformation, targetDateTime, tOutage)));
        outagesInformationPerLineId.put(tLine.getCode(), outagesInformation);
    }

    private void addOutageInformation(List<OutageInformation> outagesInformation, OffsetDateTime targetDateTime, TOutage tOutage) {
        TargetChUtil.getFixedFlowFromDaysOfWeek(targetDateTime, tOutage.getDayOfWeek()).stream()
                .collect(toOptional())
                .ifPresent(fixedFlow -> outagesInformation.add(new OutageInformation(tOutage, fixedFlow)));
    }
}
