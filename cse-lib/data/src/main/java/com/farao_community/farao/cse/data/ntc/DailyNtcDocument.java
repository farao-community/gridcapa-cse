/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.ntc;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.data.xsd.NTCReductionsDocument;
import com.farao_community.farao.cse.data.xsd.TLine;
import com.farao_community.farao.cse.data.xsd.TNTC;
import com.farao_community.farao.cse.data.xsd.TNTCreductions;
import com.farao_community.farao.cse.data.xsd.TSpecialLines;
import org.apache.commons.lang3.tuple.Pair;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class DailyNtcDocument {
    private final OffsetDateTime targetDateTime;
    private final NTCReductionsDocument ntcReductionsDocument;

    public DailyNtcDocument(OffsetDateTime targetDateTime, NTCReductionsDocument ntcReductionsDocument) {
        this.targetDateTime = targetDateTime;
        this.ntcReductionsDocument = ntcReductionsDocument;
    }

    Map<String, LineInformation> getLineInformationPerLineId(Predicate<TLine> lineSelector) {
        List<TSpecialLines> tSpecialLines = ntcReductionsDocument.getSpecialLines();
        if (tSpecialLines.isEmpty()) {
            return Collections.emptyMap();
        }
        if (tSpecialLines.size() == 1) {
            return tSpecialLines.getFirst().getLine().stream()
                    .filter(lineSelector)
                    .map(tLine -> Pair.of(tLine, NtcUtil.getTNtcFromLine(targetDateTime, tLine)))
                    .filter(p -> p.getValue().isPresent())
                    .collect(Collectors.toMap(
                            p -> p.getKey().getCode(),
                            p -> new LineInformation(p.getKey().getCNtc().value(), p.getValue().get().getType(), p.getValue().get().getV().doubleValue())
                    ));
        }
        throw new CseDataException("Several special lines sections have been defined");
    }

    Map<String, NtcInformation> getNtcInformationPerCountry() {
        List<TNTC> ntcValues = NtcUtil.getTNtcFromPeriods(targetDateTime, getTNtcReductions().getPeriod());
        Map<String, NtcInformation> ntcPerCountry = new HashMap<>();
        ntcValues.forEach(tNtc -> {
            if (ntcPerCountry.containsKey(tNtc.getCountry().value())) {
                throw new CseDataException("Two different NTC values for the same country");
            }
            if (tNtc.getType() == null) {
                throw new CseDataException("NTC for daily value must be specified");
            }
            ntcPerCountry.put(tNtc.getCountry().value(), new NtcInformation(tNtc.getType(), tNtc.getV().doubleValue()));
        });
        return ntcPerCountry;
    }

    private TNTCreductions getTNtcReductions() {
        if (ntcReductionsDocument.getNTCreductions().size() == 1) {
            return ntcReductionsDocument.getNTCreductions().getFirst();
        } else {
            throw new CseDataException("Daily NTC document should contain exactly 1 \"NTCReductions\" tag.");
        }
    }
}
