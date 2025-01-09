package com.farao_community.farao.cse.import_runner.app.util;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.data.DataUtil;
import com.farao_community.farao.cse.data.xsd.ntc2.CapacityDocument;
import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.commons.EICode;
import jakarta.xml.bind.JAXBException;

import java.io.InputStream;
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

    private Ntc2Util() {

    }

    public static Double getD2ExchangeByOffsetDateTime(InputStream ntc2InputStream, OffsetDateTime targetDateTime) throws JAXBException {
        Map<Integer, Double> qtyByPositionMap = new HashMap<>();
        CapacityDocument capacityDocument = DataUtil.unmarshalFromInputStream(ntc2InputStream, CapacityDocument.class);
        checkTimeInterval(capacityDocument, targetDateTime);

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
        ZonedDateTime targetDateInCETZone = targetDateTime.atZoneSameInstant(ZoneId.of("CET"));
        if (qtyByPositionMap.size() == 96) {
            if (targetDateInCETZone.getMinute() % 15 != 0) {
                throw new CseDataException("Minutes must be a multiple of 15 for 96 intervals");
            }
            position = 1 + (4 * targetDateInCETZone.getHour()) + (targetDateInCETZone.getMinute() / 15);
        } else if (qtyByPositionMap.size() == 24) {
            position = targetDateInCETZone.getHour() + 1;
        } else {
            throw new CseDataException(String.format("CapacityTimeSeries contains %s intervals which is different to 24 or 96", qtyByPositionMap.size()));
        }

        return qtyByPositionMap.get(position);
    }

    private static void checkTimeInterval(CapacityDocument capacityDocument, OffsetDateTime targetDateTime) {
        List<OffsetDateTime> interval = Stream.of(capacityDocument.getCapacityTimeInterval().getV().split("/"))
                .map(OffsetDateTime::parse)
                .toList();
        if (targetDateTime.isBefore(interval.get(0)) || targetDateTime.isAfter(interval.get(1)) || targetDateTime.equals(interval.get(1))) {
            throw new CseDataException("Target date time is out of bound for NTC2 archive");
        }
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
