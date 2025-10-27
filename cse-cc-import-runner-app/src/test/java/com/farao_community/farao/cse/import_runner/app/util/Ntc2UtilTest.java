package com.farao_community.farao.cse.import_runner.app.util;

import jakarta.xml.bind.JAXBException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class Ntc2UtilTest {

    @Test
    void getD2ExchangeByOffsetDateTimeAfter() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2021-09-03T00:00+01:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20210901_2D1_CH-IT1-25-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).withFailMessage("Target date time is out of bound for NTC2 archive");
    }

    @Test
    void getD2ExchangeByOffsetDateTimeBefore() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2021-09-01T23:00+01:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20210901_2D1_CH-IT1-25-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).withFailMessage("Target date time is out of bound for NTC2 archive");
    }

    @Test
    void getD2ExchangeByOffsetDateTimeAfterWinterTimeChange() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2021-09-02T23:00+01:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20210901_2D1_CH-IT1-25-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(1789);
    }

    @Test
    void getD2ExchangeByOffsetDateTimeBeforeWinterTimeChange() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2021-09-02T02:00+02:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20210901_2D1_CH-IT1-25-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(2767);
    }

    @Test
    void getD2ExchangeByOffsetDateTimeOnWinterTimeChange() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2021-09-02T03:00+02:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20210901_2D1_CH-IT1-25-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(8888);
    }

    @Test
    void getD2ExchangeByOffsetDateTimeOnWinterTimeChange2() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2021-09-02T02:00+01:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20210901_2D1_CH-IT1-25-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(8888);
    }

    @Test
    void getAreaCodeFromFilenameDoesntExist() {
        Assertions.assertThat(Ntc2Util.getAreaCodeFromFilename("tweet")).isEqualTo(Optional.empty());
    }

    @Test
    void getAreaCodeFromFilename() {
        Assertions.assertThat(Ntc2Util.getAreaCodeFromFilename("NTC2_20210901_2D1_CH-IT1-25-hour-case.xml")).isEqualTo(Optional.of("10YCH-SWISSGRIDZ"));
    }

    @Test
    void getD2ExchangeByOffsetDateTimeAfterSummerTimeChange() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2021-09-02T23:00+02:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20210901_2D1_CH-IT1-23-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(1789);
    }

    @Test
    void getD2ExchangeByOffsetDateTimeBeforeSummerTimeChange() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2021-09-02T01:00+01:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20210901_2D1_CH-IT1-23-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(2222);
    }

    @Test
    void getD2ExchangeByOffsetDateTimeOnSummerTimeChange() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2021-09-02T02:00+01:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20210901_2D1_CH-IT1-23-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(8888);
    }

    @Test
    void getD2ExchangeByOffsetDateTimeOnSummerTimeChange2() throws JAXBException, IOException {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2021-09-02T03:00+02:00");
        final InputStream inputStream = Objects.requireNonNull(getClass().getResource("NTC2_20210901_2D1_CH-IT1-23-hour-case.xml")).openStream();
        Assertions.assertThat(Ntc2Util.getD2ExchangeByOffsetDateTime(inputStream, targetDate)).isEqualTo(8888);
    }
}
