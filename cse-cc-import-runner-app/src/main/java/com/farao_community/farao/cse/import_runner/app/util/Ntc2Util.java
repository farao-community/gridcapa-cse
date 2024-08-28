package com.farao_community.farao.cse.import_runner.app.util;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.data.DataUtil;
import com.farao_community.farao.cse.data.xsd.ntc2.CapacityDocument;
import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.commons.EICode;

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
        int position = targetDateTime.atZoneSameInstant(ZoneId.of("CET")).getHour() + 1;
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
        return Optional.ofNullable(qtyByPositionMap.get(position))
                .orElseThrow(() -> new CseDataException(
                        String.format("Impossible to retrieve NTC2 position %d. It does not exist", position)));
    }

    private static void checkTimeInterval(CapacityDocument capacityDocument, OffsetDateTime targetDateTime) {
        List<OffsetDateTime> interval = Stream.of(capacityDocument.getCapacityTimeInterval().getV().split("/"))
                .map(OffsetDateTime::parse)
                .collect(Collectors.toList());
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
