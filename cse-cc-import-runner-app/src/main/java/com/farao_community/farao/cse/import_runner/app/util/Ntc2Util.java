/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.util;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.data.DataUtil;
import com.farao_community.farao.cse.data.xsd.ntc2.CapacityDocument;
import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.commons.EICode;
import jakarta.xml.bind.JAXBException;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class Ntc2Util {

    private static final String AUSTRIAN_TAG = "AT";
    private static final String SWISS_TAG = "CH";
    private static final String FRENCH_TAG = "FR";
    private static final String SLOVENIAN_TAG = "SI";
    private static final String IT_AREA_CODE = "10YIT-GRTN-----B";
    public static final String CET_ZONE_ID = "CET";

    private Ntc2Util() {

    }

    public static Double getD2ExchangeByOffsetDateTime(final InputStream ntc2InputStream, final OffsetDateTime targetDateTime) throws JAXBException {
        final Map<Integer, Double> qtyByPositionMap = new HashMap<>();
        final CapacityDocument capacityDocument = DataUtil.unmarshalFromInputStream(ntc2InputStream, CapacityDocument.class);
        final ZonedDateTime targetDateInCETZone = targetDateTime.atZoneSameInstant(ZoneId.of(CET_ZONE_ID));
        final List<OffsetDateTime> documentInterval = getDocumentInterval(capacityDocument);
        checkTimeInterval(documentInterval, targetDateTime);

        capacityDocument.getCapacityTimeSeries()
                .stream()
                .filter(ts -> IT_AREA_CODE.equalsIgnoreCase(ts.getInArea().getV()) && !ts.getPeriod().isEmpty())
                .findFirst()
                .orElseThrow(() -> new CseDataException("No import Timeseries in NTC2 file"))
                .getPeriod()
                .get(0)
                .getInterval()
                .forEach(interval -> {
                    final int index = interval.getPos().getV();
                    qtyByPositionMap.put(index, interval.getQty().getV().doubleValue());
                });
        int position;

        final int positionHour = calculateHourPositionFromOffsets(targetDateInCETZone, capacityDocument);
        final int positionMapSize = qtyByPositionMap.size();
        if (hasCorrectNbOfQuarterHoursInDay(documentInterval, positionMapSize)) {
            if (targetDateInCETZone.getMinute() % 15 != 0) {
                throw new CseDataException("Minutes must be a multiple of 15 for 96 intervals (or 92 or 100 on daylight saving time changes)");
            }
            position = 1 + (4 * positionHour) + (targetDateInCETZone.getMinute() / 15);
        } else if (hasCorrectNbOfHoursInDay(documentInterval, positionMapSize)) {
            position = positionHour + 1;
        } else {
            throw new CseDataException(String.format("CapacityTimeSeries contains %s intervals which is different to 24 or 96 (or 23/92 or 25/100 on daylight saving time changes)", qtyByPositionMap.size()));
        }

        return qtyByPositionMap.get(position);
    }

    private static void checkTimeInterval(List<OffsetDateTime> interval, OffsetDateTime targetDateTime) {
        if (targetDateTime.isBefore(interval.get(0)) || !targetDateTime.isBefore(interval.get(1))) {
            throw new CseDataException("Target date time is out of bounds for NTC2 archive");
        }
    }

    private static @NotNull List<OffsetDateTime> getDocumentInterval(final CapacityDocument capacityDocument) {
        return Stream.of(capacityDocument.getCapacityTimeInterval().getV().split("/"))
                .map(OffsetDateTime::parse)
                .toList();
    }

    private static int calculateHourPositionFromOffsets(final ZonedDateTime targetDateTime, final CapacityDocument capacityDocument) {
        final OffsetDateTime beginDateTime = getDocumentInterval(capacityDocument).getFirst();
        return (int) Duration.between(beginDateTime, targetDateTime).getSeconds() / 3600;
    }

    private static boolean hasCorrectNbOfQuarterHoursInDay(List<OffsetDateTime> interval, int inputQuarterHoursInDay) {
        return hasCorrectNbOfHoursInDay(interval, inputQuarterHoursInDay / 4);
    }

    private static boolean hasCorrectNbOfHoursInDay(List<OffsetDateTime> interval, int inputHoursInDay) {
        final ZonedDateTime intervalBegin = interval.get(0).atZoneSameInstant(ZoneId.of(CET_ZONE_ID));
        final ZonedDateTime intervalEnd = interval.get(1).atZoneSameInstant(ZoneId.of(CET_ZONE_ID));
        final long hoursBetween = Duration.between(intervalBegin, intervalEnd).toHours();
        return hoursBetween == inputHoursInDay;
    }

    public static Optional<String> getAreaCodeFromFilename(String fileName) {
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
