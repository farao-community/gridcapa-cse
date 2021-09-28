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
            // Patch to model that an end time of 00 is midnight, so we set it to 24 so that condition works
            if (endHour == 0) {
                endHour = 24;
            }
            return utcHour >= startHour && utcHour < endHour;
        }
    }
}
