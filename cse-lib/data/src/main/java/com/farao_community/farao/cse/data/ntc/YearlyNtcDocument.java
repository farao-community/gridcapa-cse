/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.ntc;

import com.farao_community.farao.cse.data.*;
import com.farao_community.farao.cse.data.xsd.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class YearlyNtcDocument {
    private final OffsetDateTime targetDateTime;
    private final NTCAnnualDocument ntcAnnualDocument;

    public YearlyNtcDocument(OffsetDateTime targetDateTime, NTCAnnualDocument ntcAnnualDocument) {
        this.targetDateTime = targetDateTime;
        this.ntcAnnualDocument = ntcAnnualDocument;
    }

    Map<String, LineInformation> getLineInformationPerLineId(Predicate<TLine> lineSelector) {
        TSpecialLines tSpecialLines = getTNtcValues().getSpecialLines();
        if (tSpecialLines != null) {
            return tSpecialLines.getLine().stream()
                    .filter(lineSelector)
                    .collect(Collectors.toMap(
                        TLine::getCode,
                        tLine -> {
                            TNTC tNtc = NtcUtil.getTNtcFromLine(targetDateTime, tLine);
                            if (tNtc.getType().equalsIgnoreCase(NtcUtil.ABSOLUTE)) {
                                return new LineInformation(tLine.getCNtc().value(), tNtc.getType(), tNtc.getV().doubleValue());
                            }
                            throw new CseDataException("Flow for yearly value must be absolute");
                        }
                    ));
        }
        return Collections.emptyMap();
    }

    Map<String, NtcInformation> getNtcInformationPerCountry() {
        List<TNTC> ntcValues = getNtcValues();
        Map<String, NtcInformation> ntcPerCountry = new HashMap<>();
        ntcValues.forEach(tntc -> {
            if (ntcPerCountry.containsKey(tntc.getCountry().value())) {
                throw new CseDataException("Two different NTC values for the same country");
            }
            if (tntc.getType() == null || tntc.getType().equalsIgnoreCase(NtcUtil.ABSOLUTE)) {
                ntcPerCountry.put(tntc.getCountry().value(), new NtcInformation(NtcUtil.ABSOLUTE, tntc.getV().doubleValue()));
            } else {
                throw new CseDataException("NTC for yearly value must be absolute");
            }

        });
        return ntcPerCountry;
    }

    private TNTCValues getTNtcValues() {
        if (ntcAnnualDocument.getNTCvalues().size() == 1) {
            return ntcAnnualDocument.getNTCvalues().get(0);
        } else {
            throw new CseDataException("Yearly document should contain exactly 1 \"NTCValues\" tag.");
        }
    }

    private List<TNTC> getNtcValues() {
        List<TNTC> ntcValues = getSpecialDayNtcValues();
        if (ntcValues.isEmpty()) {
            ntcValues = getBasicDayNtcValues();
        }
        if (ntcValues.isEmpty()) {
            throw new CseDataException(String.format("No NTC values for target date %s", targetDateTime));
        }
        return ntcValues;
    }

    private List<TNTC> getSpecialDayNtcValues() {
        TSpecialDays tSpecialDays = getTNtcValues().getSpecialDays();
        if (tSpecialDays != null) {
            return NtcUtil.getTNtcFromDays(targetDateTime, tSpecialDays.getDay());
        } else {
            return Collections.emptyList();
        }
    }

    private List<TNTC> getBasicDayNtcValues() {
        TBasicDays tBasicDays = getTNtcValues().getBasicDays();
        if (tBasicDays != null) {
            return NtcUtil.getTNtcFromPeriods(targetDateTime, tBasicDays.getPeriod());
        } else {
            return Collections.emptyList();
        }
    }
}
