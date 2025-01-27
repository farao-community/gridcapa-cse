/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.ntc;

import com.farao_community.farao.cse.data.*;
import com.farao_community.farao.cse.data.xsd.*;

import java.time.OffsetDateTime;
import java.util.*;

import static com.farao_community.farao.cse.data.DataUtil.toOptional;

final class NtcUtil {
    static final String ABSOLUTE = "absolute";

    private NtcUtil() {
        // Should not be instantiated
    }

    static Optional<TNTC> getTNtcFromLineFromNtcRedFile(OffsetDateTime targetDateTime, TLine tLine) {
        return getTNtcFromPeriods(targetDateTime, tLine.getPeriod()).stream()
                .collect(toOptional());
    }

    static TNTC getTNtcFromLineFromYearlyNtc(OffsetDateTime targetDateTime, TLine tLine) {
        return getTNtcFromPeriods(targetDateTime, tLine.getPeriod()).stream()
                .collect(toOptional())
                .orElseThrow(() -> new CseDataException(String.format("No NTC definition for line %s", tLine.getCode())));
    }

    static List<TNTC> getTNtcFromDays(OffsetDateTime targetDateTime, List<TDay> tDays) {
        return tDays.stream()
                .filter(tDay -> DateTimeUtil.isDayMatching(targetDateTime, tDay.getDay()))
                .collect(toOptional())
                .map(TDay::getTimeInterval)
                .map(tTimeIntervals -> getTNtcFromTimeIntervals(targetDateTime, tTimeIntervals))
                .orElse(Collections.emptyList());
    }

    static List<TNTC> getTNtcFromPeriods(OffsetDateTime targetDateTime, List<TPeriod> tPeriods) {
        Optional<TPeriod> period = tPeriods.stream()
                .filter(tPeriod -> DateTimeUtil.isTargetDateInInterval(targetDateTime, tPeriod.getDtini(), tPeriod.getDtfin()))
                .collect(toOptional());

        if (period.isEmpty()) {
            return Collections.emptyList();
        }

        return period.get().getDayOfWeek().stream()
                .filter(tDayOfWeek -> isTargetDayOfWeekMatchWithDayNum(tDayOfWeek.getDaynum(), targetDateTime.getDayOfWeek().getValue()))
                .filter(tDayOfWeek -> !getTNtcFromTimeIntervals(targetDateTime, tDayOfWeek.getTimeInterval()).isEmpty())
                .collect(toOptional())
                .map(TDayOfWeek::getTimeInterval)
                .map(tTimeIntervals -> getTNtcFromTimeIntervals(targetDateTime, tTimeIntervals))
                .orElse(Collections.emptyList());
    }

    static List<TNTC> getTNtcFromTimeIntervals(OffsetDateTime targetDateTime, List<TTimeInterval> tTimeIntervals) {
        return tTimeIntervals.stream()
                .filter(tTimeInterval -> DateTimeUtil.isTargetDateInInterval(targetDateTime, tTimeInterval.getTini(), tTimeInterval.getTfin()))
                .collect(toOptional())
                .map(TTimeInterval::getNTC)
                .orElse(Collections.emptyList());
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
