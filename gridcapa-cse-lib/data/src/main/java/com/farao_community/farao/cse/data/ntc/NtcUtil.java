/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.ntc;

import com.farao_community.farao.cse.data.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

final class NtcUtil {
    static final String ABSOLUTE = "absolute";

    private NtcUtil() {
        // Should not be instantiated
    }

    static <T> Collector<T, ?, Optional<T>> toSingleton(String message) {
        return Collectors.collectingAndThen(
            Collectors.toList(),
            list -> {
                if (list.isEmpty()) {
                    return Optional.empty();
                }
                if (list.size() == 1) {
                    return Optional.of(list.get(0));
                }
                throw new CseDataException(message);
            }
        );
    }

    static TNTC getTNtcFromLine(OffsetDateTime targetDateTime, TLine tLine) {
        return getTNtcFromPeriods(targetDateTime, tLine.getPeriod()).stream()
                .collect(toSingleton(String.format("Several matching flow information for line %s", tLine.getCode())))
                .orElseThrow(() -> new CseDataException(String.format("No NTC definition for line %s", tLine.getCode())));
    }

    static List<TNTC> getTNtcFromDays(OffsetDateTime targetDateTime, List<TDay> tDays) {
        Optional<TDay> day = tDays.stream()
                .filter(tDay -> DateTimeUtil.isDayMatching(targetDateTime, tDay.getDay()))
                .collect(toSingleton(String.format("Several day definitions for target date %s", targetDateTime.toString())));

        return day
                .map(tDay -> getTNtcFromTimeIntervals(targetDateTime, day.get().getTimeInterval()))
                .orElse(Collections.emptyList());
    }

    static List<TNTC> getTNtcFromPeriods(OffsetDateTime targetDateTime, List<TPeriod> tPeriods) {
        Optional<TPeriod> period = tPeriods.stream()
                .filter(tPeriod -> DateTimeUtil.isTargetDateInInterval(targetDateTime, tPeriod.getDtini(), tPeriod.getDtfin()))
                .collect(toSingleton(String.format("Several period definitions for target date %s", targetDateTime.toString())));

        if (period.isEmpty()) {
            return Collections.emptyList();
        }

        Optional<TDayOfWeek> dayOfWeek = period.get().getDayOfWeek().stream()
                .filter(tDayOfWeek -> isTargetDayOfWeekMatchWithDayNum(tDayOfWeek.getDaynum(), targetDateTime.getDayOfWeek().getValue()))
                .filter(tDayOfWeek -> !getTNtcFromTimeIntervals(targetDateTime, tDayOfWeek.getTimeInterval()).isEmpty())
                .collect(toSingleton(String.format("Several matching day of week in period for target date %s", targetDateTime.toString())));

        return dayOfWeek
                .map(tDay -> getTNtcFromTimeIntervals(targetDateTime, dayOfWeek.get().getTimeInterval()))
                .orElse(Collections.emptyList());
    }

    static List<TNTC> getTNtcFromTimeIntervals(OffsetDateTime targetDateTime, List<TTimeInterval> tTimeIntervals) {
        Optional<TTimeInterval> timeInterval = tTimeIntervals.stream()
                .filter(tTimeInterval -> DateTimeUtil.isTargetDateInInterval(targetDateTime, tTimeInterval.getTini(), tTimeInterval.getTfin()))
                .collect(toSingleton(String.format("Several matching time interval for target date %s", targetDateTime.toString())));

        return timeInterval.map(TTimeInterval::getNTC).orElse(Collections.emptyList());
    }

    static boolean isTargetDayOfWeekMatchWithDayNum(int daynum, int targetDayZOfWeek) {
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
