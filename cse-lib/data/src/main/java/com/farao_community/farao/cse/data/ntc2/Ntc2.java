/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.ntc2;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.cse.data.DataUtil;
import com.farao_community.farao.cse.data.xsd.ntc2.CapacityDocument;
import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import com.powsybl.iidm.network.Country;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Belgacem Najjari {@literal <belgacem.najjari at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class Ntc2 {
    private static final String AUSTRIAN_TAG = "AT";
    private static final String SWISS_TAG = "CH";
    private static final String FRENCH_TAG = "FR";
    private static final String SLOVENIAN_TAG = "SI";

    private final Map<String, Double> exchanges;

    private Ntc2(Map<String, Double> ntc2) {
        this.exchanges = ntc2;
    }

    public Map<String, Double> getExchanges() {
        return exchanges;
    }

    Double getExchange(Country country) {
        return exchanges.get(new EICode(country).getAreaCode());
    }

    public static Ntc2 create(OffsetDateTime targetDateTime, Map<String, InputStream> ntc2InputStreams) {
        return new Ntc2(getD2Exchanges(ntc2InputStreams, targetDateTime));
    }

    private static Map<String, Double> getD2Exchanges(Map<String, InputStream> ntc2InputStreams, OffsetDateTime targetDateTime) {
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, InputStream> ntc2Entry : ntc2InputStreams.entrySet()) {
            Optional<String> optAreaCode = getAreaCodeFromFilename(ntc2Entry.getKey());
            optAreaCode.ifPresent(areaCode -> {
                try {
                    double d2Exchange = getD2ExchangeByOffsetDateTime(ntc2Entry.getValue(), targetDateTime);
                    result.put(areaCode, d2Exchange);
                } catch (Exception e) {
                    throw new CseInvalidDataException(e.getMessage(), e);
                }
            });
        }
        return result;
    }

    private static Double getD2ExchangeByOffsetDateTime(InputStream ntc2InputStream, OffsetDateTime targetDateTime) throws JAXBException {
        Map<Integer, Double> qtyByPositionMap = new HashMap<>();
        CapacityDocument capacityDocument = DataUtil.unmarshalFromInputStream(ntc2InputStream, CapacityDocument.class);
        checkTimeInterval(capacityDocument, targetDateTime);
        int position = targetDateTime.atZoneSameInstant(ZoneId.of("CET")).getHour() + 1;
        capacityDocument.getCapacityTimeSeries().getPeriod().getInterval().forEach(interval -> {
            int index = interval.getPos().getV().intValue();
            qtyByPositionMap.put(index, interval.getQty().getV().doubleValue());
        });
        return qtyByPositionMap.get(position);
    }

    private static void checkTimeInterval(CapacityDocument capacityDocument, OffsetDateTime targetDateTime) {
        List<OffsetDateTime> interval = Stream.of(capacityDocument.getCapacityTimeInterval().getV().split("/"))
                .map(OffsetDateTime::parse)
                .collect(Collectors.toList());
        if (targetDateTime.isBefore(interval.get(0)) || targetDateTime.isAfter(interval.get(1)) || targetDateTime.equals(interval.get(1))) {
            throw new CseInvalidDataException("Target date time is out of bound for NTC2 archive");
        }
    }

    private static Optional<String> getAreaCodeFromFilename(String fileName) {
        if (fileName.contains(AUSTRIAN_TAG)) {
            return Optional.of(new EICode(Country.AT).getAreaCode());
        } else if (fileName.contains(SLOVENIAN_TAG)) {
            return Optional.of(new EICode(Country.SI).getAreaCode());
        } else if (fileName.contains(SWISS_TAG)) {
            return Optional.of(new EICode(Country.CH).getAreaCode());
        } else if (fileName.contains(FRENCH_TAG)) {
            return Optional.of(new EICode(Country.FR).getAreaCode());
        } else {
            return Optional.empty();
        }
    }
}
