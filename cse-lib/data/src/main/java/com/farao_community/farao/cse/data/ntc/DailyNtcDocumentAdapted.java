/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.ntc;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.data.DataUtil;
import com.farao_community.farao.cse.data.xsd.ntc_adapted.*;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class DailyNtcDocumentAdapted {
    private final OffsetDateTime targetDateTime;
    private final NTCReductionsDocument ntcReductionsDocument;

    static DailyNtcDocumentAdapted create(OffsetDateTime targetDateTime, InputStream ntcReductionsInputStream) throws JAXBException {
        return new DailyNtcDocumentAdapted(targetDateTime, DataUtil.unmarshalFromInputStream(ntcReductionsInputStream, NTCReductionsDocument.class));
    }

    private DailyNtcDocumentAdapted(OffsetDateTime targetDateTime, NTCReductionsDocument ntcReductionsDocument) {
        this.targetDateTime = targetDateTime;
        this.ntcReductionsDocument = ntcReductionsDocument;
    }

    Map<String, LineInformation> getLineInformationPerLineId(Predicate<TLine> lineSelector) {
        TSpecialLines tSpecialLines = ntcReductionsDocument.getSpecialLinesImport();
        if (tSpecialLines == null || tSpecialLines.getLine() == null || tSpecialLines.getLine().isEmpty()) {
            return Collections.emptyMap();
        }
        return tSpecialLines.getLine().stream()
                .filter(lineSelector)
                .collect(Collectors.toMap(
                    TLine::getCode,
                    tLine -> {
                        TNTC tNtc = NtcUtilAdapted.getTNtcFromLine(targetDateTime, tLine);
                        return new LineInformation(tLine.getCNtc().value(), tNtc.getType(), tNtc.getV().doubleValue());
                    }
                ));
    }

    Map<String, NtcInformation> getNtcInformationPerCountry() {
        List<TNTC> ntcValues = NtcUtilAdapted.getTNtcFromPeriods(targetDateTime, getTNtcReductionsImport().getPeriod());
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

    private TNTCreductionsImport getTNtcReductionsImport() {
        if (ntcReductionsDocument.getNTCreductionsImport() != null) {
            return ntcReductionsDocument.getNTCreductionsImport();
        } else {
            throw new CseDataException("Daily NTC document should contain exactly 1 \"NTCreductionsImport\" tag.");
        }
    }
}
