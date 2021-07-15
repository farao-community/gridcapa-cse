/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public final class DateTimeUtil {

    private DateTimeUtil() {
        // Should not be instantiated
    }

    public static boolean isDayMatching(OffsetDateTime offsetDateTime, String day) {
        LocalDate utcDate = offsetDateTime.atZoneSameInstant(ZoneOffset.UTC).toLocalDate();
        LocalDate dayDate = LocalDate.parse(day, DateTimeFormatter.ofPattern("yyyy-MM-dd'Z'"));
        return utcDate.equals(dayDate);
    }

    public static boolean isTargetDateInInterval(OffsetDateTime targetDate, String startingTime, String endingTime) {
        try {
            OffsetDateTime startTime = OffsetDateTime.parse(startingTime);
            OffsetDateTime endTime = OffsetDateTime.parse(endingTime);
            return targetDate.isEqual(startTime) || (targetDate.isAfter(startTime) && targetDate.isBefore(endTime));
        } catch (DateTimeParseException e) {
            double utcHour = targetDate.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime().getHour();
            double startHour = LocalTime.parse(startingTime, DateTimeFormatter.ofPattern("'T'HH:mm'Z'")).getHour();
            double endHour = LocalTime.parse(endingTime, DateTimeFormatter.ofPattern("'T'HH:mm'Z'")).getHour();
            return utcHour >= startHour && utcHour <= endHour;
        }
    }

    public static boolean isTargetDayOfWeekMatchWithDayNum(int daynum, int targetDayZOfWeek) {
        DayOfWeek dayOfWeek = DayOfWeek.getInstance(daynum);
        switch (dayOfWeek) {
            case EVERYDAY:
                return true;
            case SATURDAY:
                return targetDayZOfWeek == DayOfWeek.SATURDAY.getDaynum();
            case SUNDAY:
                return targetDayZOfWeek == DayOfWeek.SUNDAY.getDaynum();
            case MONTOFRI:
                return targetDayZOfWeek != DayOfWeek.SATURDAY.getDaynum() && targetDayZOfWeek != DayOfWeek.SUNDAY.getDaynum();
            case MONTOSAT:
                return targetDayZOfWeek != DayOfWeek.SUNDAY.getDaynum();
            default:
                return false;
        }
    }
}
