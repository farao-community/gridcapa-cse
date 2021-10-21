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
import com.farao_community.farao.gridcapa_cse.api.exception.CseInternalException;
import com.farao_community.farao.gridcapa_cse.api.exception.CseInvalidDataException;
import com.powsybl.iidm.network.Country;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(Ntc2.class);
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

    public static Ntc2 create(OffsetDateTime targetDateTime, Iterable<File> ntc2Files) {
        return new Ntc2(getD2Exchanges(ntc2Files, targetDateTime));
    }

    private static Map<String, Double> getD2Exchanges(Iterable<File> ntc2Files, OffsetDateTime targetDateTime) {
        Map<String, Double> result = new HashMap<>();
        for (File ntc2file : ntc2Files) {
            Optional<String> optAreaCode = getAreaCodeFromFilename(ntc2file.getName());
            optAreaCode.ifPresent(areaCode -> {
                try {
                    double d2Exchange = getD2ExchangeByOffsetDateTime(ntc2file, targetDateTime);
                    result.put(areaCode, d2Exchange);
                } catch (Exception e) {
                    throw new CseInvalidDataException(e.getMessage(), e);
                }
            });
        }
        return result;
    }

    private static Double getD2ExchangeByOffsetDateTime(File ntc2file, OffsetDateTime targetDateTime) throws JAXBException {
        try (FileInputStream fileInputStream = new FileInputStream(ntc2file)) {
            Map<Integer, Double> qtyByPositionMap = new HashMap<>();
            CapacityDocument capacityDocument = DataUtil.unmarshalFromInputStream(fileInputStream, CapacityDocument.class);
            checkTimeInterval(capacityDocument, targetDateTime);
            int position = targetDateTime.atZoneSameInstant(ZoneId.of("CET")).getHour() + 1;
            capacityDocument.getCapacityTimeSeries().getPeriod().getInterval().forEach(interval -> {
                int index = interval.getPos().getV().intValue();
                qtyByPositionMap.put(index, interval.getQty().getV().doubleValue());
            });
            return qtyByPositionMap.get(position);
        } catch (IOException e) {
            LOGGER.error("Error while reading file '{}'", ntc2file.getName());
            throw new CseInternalException("Error while reading file " + ntc2file.getName(), e);
        }
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
