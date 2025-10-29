package com.farao_community.farao.cse.import_runner.app.util;

import com.farao_community.farao.cse.data.CseDataException;
import jakarta.xml.bind.JAXBException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

class Ntc2UtilTest {

    @Test
    void getD2ExchangeByOffsetDateTimeAfter() throws IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2025-10-27T01:00+01:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20251026_2D1_CH-IT1-25-hour-case.xml")).openStream();
        Assertions.assertThatExceptionOfType(CseDataException.class).isThrownBy(() -> Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate))
                .withMessage("Target date time is out of bounds for NTC2 archive");
    }

    @Test
    void getD2ExchangeByOffsetDateTimeBefore() throws IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2025-10-25T22:00+01:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20251026_2D1_CH-IT1-25-hour-case.xml")).openStream();
        Assertions.assertThatExceptionOfType(CseDataException.class).isThrownBy(() -> Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate))
                .withMessage("Target date time is out of bounds for NTC2 archive");
    }

    @Test
    void getD2ExchangeByOffsetDateTimeAfterWinterTimeChange() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2025-10-26T23:00+01:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20251026_2D1_CH-IT1-25-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(1789);
    }

    @Test
    void getD2ExchangeByOffsetDateTimeBeforeWinterTimeChange() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2025-10-26T02:00+02:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20251026_2D1_CH-IT1-25-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(2767);
    }

    @Test
    void getD2ExchangeByOffsetDateTimeOnWinterTimeChange() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2025-10-26T03:00+02:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20251026_2D1_CH-IT1-25-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(8888);
    }

    @Test
    void getD2ExchangeByOffsetDateTimeOnWinterTimeChange2() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2025-10-26T02:00+01:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20251026_2D1_CH-IT1-25-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(8888);
    }

    @Test
    void getAreaCodeFromFilenameDoesntExist() {
        Assertions.assertThat(Ntc2Util.getAreaCodeFromFilename("tweet")).isEmpty();
    }

    @Test
    void getAreaCodeFromFilename() {
        Assertions.assertThat(Ntc2Util.getAreaCodeFromFilename("NTC2_20251026_2D1_CH-IT1-25-hour-case.xml")).isEqualTo(Optional.of("10YCH-SWISSGRIDZ"));
    }

    @Test
    void getD2ExchangeByOffsetDateTimeAfterSummerTimeChange() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2025-03-30T23:00+02:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20250330_2D1_CH-IT1-23-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(1789);
    }

    @Test
    void getD2ExchangeByOffsetDateTimeBeforeSummerTimeChange() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2025-03-30T01:00+01:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20250330_2D1_CH-IT1-23-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(2222);
    }

    @Test
    void getD2ExchangeByOffsetDateTimeOnSummerTimeChange() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2025-03-30T02:00+01:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20250330_2D1_CH-IT1-23-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(8888);
    }

    @Test
    void getD2ExchangeByOffsetDateTimeOnSummerTimeChange2() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2025-03-30T03:00+02:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20250330_2D1_CH-IT1-23-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(8888);
    }

    @Test
    void getD2ExchangeByOffsetDateTimeOnSummerTimeChange100() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2025-10-26T23:30+01:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20251026_2D5_CH-IT1-test-100.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(4321);
    }

    @Test
    void getD2ExchangeByOffsetDateTimeOnSummerTimeChange94() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2025-03-30T23:30+02:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20250330_2D5_CH-IT1-test-92.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(4321);
    }

    @Test
    void getD2ExchangeByOffsetDateTimeWrongintervals() throws IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2021-09-02T03:00+02:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20210901_2D1_CH-IT1-bad-case.xml")).openStream();
        Assertions.assertThatExceptionOfType(CseDataException.class).isThrownBy(() -> Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate))
                .withMessage("CapacityTimeSeries contains 21 intervals which is different to 24 or 96 (or 23/92 or 25/100 on daylight saving time changes)");
    }

    @Test
    void getD2ExchangeByOffsetDateTimeWrongTargetDate() throws IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2025-10-26T23:11+02:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20251026_2D5_CH-IT1-test-100.xml")).openStream();
        Assertions.assertThatExceptionOfType(CseDataException.class).isThrownBy(() -> Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate))
                .withMessage("Minutes must be a multiple of 15 for 96 intervals (or 92 or 100 on daylight saving time changes)");
    }
}
