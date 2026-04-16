/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.ntc;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.data.xsd.NTCAnnualDocument;
import com.farao_community.farao.cse.data.xsd.TLine;
import com.farao_community.farao.cse.data.xsd.TNTC;
import com.farao_community.farao.cse.data.xsd.TNTCValues;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.farao_community.farao.cse.data.ntc.NtcUtil.ABSOLUTE;
import static com.farao_community.farao.cse.data.ntc.NtcUtil.getTNtcFromDays;
import static com.farao_community.farao.cse.data.ntc.NtcUtil.getTNtcFromLine;
import static com.farao_community.farao.cse.data.ntc.NtcUtil.getTNtcFromPeriods;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

public final class YearlyNtcDocument {
    private final OffsetDateTime targetDateTime;
    private final NTCAnnualDocument ntcAnnualDocument;

    public YearlyNtcDocument(final OffsetDateTime targetDateTime, final NTCAnnualDocument ntcAnnualDocument) {
        this.targetDateTime = targetDateTime;
        this.ntcAnnualDocument = ntcAnnualDocument;
    }

    Map<String, LineInformation> getLineInformationById(final Predicate<TLine> lineSelector) {
        return Optional.ofNullable(getTNtcValues().getSpecialLines())
            .map(sl -> sl.getLine()
                .stream()
                .filter(lineSelector)
                .collect(toMap(TLine::getCode, getLineInformation(targetDateTime))))
            .orElse(emptyMap());
    }

    private static Function<TLine, LineInformation> getLineInformation(final OffsetDateTime targetDateTime) {
        return tLine -> {
            final TNTC tNtc = getTNtcFromLine(targetDateTime, tLine)
                .orElseThrow(() -> new CseDataException(String.format("No NTC definition for line %s", tLine.getCode())));
            if (tNtc.getType().equalsIgnoreCase(ABSOLUTE)) {
                return new LineInformation(tLine.getCNtc().value(), tNtc.getType(), tNtc.getV().doubleValue());
            }
            throw new CseDataException("Flow for yearly value must be absolute");
        };
    }

    Map<String, NtcInformation> getNtcInformationByCountry() {
        final List<TNTC> ntcValues = getNtcValues();
        final Map<String, NtcInformation> ntcByCountry = new HashMap<>();

        ntcValues.forEach(putInMap(ntcByCountry));

        return ntcByCountry;
    }

    static Consumer<TNTC> putInMap(final Map<String, NtcInformation> ntcByCountry) {
        return tntc -> {
            final String country = tntc.getCountry().value();
            final String type = tntc.getType();
            if (ntcByCountry.containsKey(country)) {
                throw new CseDataException("Two different NTC values for the same country");
            }
            if (type == null || type.equalsIgnoreCase(ABSOLUTE)) {
                ntcByCountry.put(country, new NtcInformation(ABSOLUTE, tntc.getV().doubleValue()));
            } else {
                throw new CseDataException("NTC for yearly value must be absolute");
            }
        };
    }

    private TNTCValues getTNtcValues() {
        if (ntcAnnualDocument.getNTCvalues().size() == 1) {
            return ntcAnnualDocument.getNTCvalues().getFirst();
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
        return Optional.ofNullable(getTNtcValues().getSpecialDays())
            .map(days -> getTNtcFromDays(targetDateTime, days.getDay()))
            .orElse(emptyList());
    }

    private List<TNTC> getBasicDayNtcValues() {
        return Optional.ofNullable(getTNtcValues().getBasicDays())
            .map(days -> getTNtcFromPeriods(targetDateTime, days.getPeriod()))
            .orElse(emptyList());
    }
}
