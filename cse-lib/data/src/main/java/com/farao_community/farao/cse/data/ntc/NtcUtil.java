/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.ntc;

import com.farao_community.farao.cse.data.DateTimeUtil;
import com.farao_community.farao.cse.data.xsd.TDay;
import com.farao_community.farao.cse.data.xsd.TDayOfWeek;
import com.farao_community.farao.cse.data.xsd.TLine;
import com.farao_community.farao.cse.data.xsd.TNTC;
import com.farao_community.farao.cse.data.xsd.TPeriod;
import com.farao_community.farao.cse.data.xsd.TTimeInterval;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.farao_community.farao.cse.data.DataUtil.toOptional;
import static com.farao_community.farao.cse.data.DateTimeUtil.isTargetDateInInterval;
import static com.farao_community.farao.cse.data.ntc.DayOfWeek.SATURDAY;
import static com.farao_community.farao.cse.data.ntc.DayOfWeek.SUNDAY;
import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;

public final class NtcUtil {
    static final String ABSOLUTE = "absolute";

    private NtcUtil() {
        // Should not be instantiated
    }

    static Optional<TNTC> getTNtcFromLine(final OffsetDateTime targetDateTime, final TLine tLine) {
        return getTNtcFromPeriods(targetDateTime, tLine.getPeriod())
            .stream()
            .collect(toOptional());
    }

    static List<TNTC> getTNtcFromDays(final OffsetDateTime targetDateTime, final List<TDay> tDays) {
        return tDays.stream()
            .filter(tDay -> DateTimeUtil.isDayMatching(targetDateTime, tDay.getDay()))
            .collect(toOptional())
            .map(TDay::getTimeInterval)
            .map(getTNTCsFromTimeIntervals(targetDateTime))
            .orElse(emptyList());
    }

    static List<TNTC> getTNtcFromPeriods(OffsetDateTime targetDateTime, List<TPeriod> tPeriods) {
        return tPeriods.stream()
            .filter(periodContains(targetDateTime))
            .collect(toOptional())
            .map(tPeriod -> tPeriod.getDayOfWeek().stream()
                .filter(tDayOfWeek -> dayOfWeekMatchesDayNum(tDayOfWeek.getDaynum(), targetDateTime.getDayOfWeek().getValue()))
                .map(TDayOfWeek::getTimeInterval)
                .map(getTNTCsFromTimeIntervals(targetDateTime))
                .filter(not(List::isEmpty))
                .collect(toOptional())
                .orElse(emptyList()))
            .orElse(emptyList());

    }

    private static Function<List<TTimeInterval>, List<TNTC>> getTNTCsFromTimeIntervals(final OffsetDateTime targetDateTime) {
        return intervals -> intervals.stream()
            .filter(intervalContains(targetDateTime))
            .collect(toOptional())
            .map(TTimeInterval::getNTC)
            .orElse(emptyList());
    }

    private static Predicate<TTimeInterval> intervalContains(final OffsetDateTime targetDateTime) {
        return tTimeInterval -> isTargetDateInInterval(
            targetDateTime, tTimeInterval.getTini(), tTimeInterval.getTfin()
        );
    }

    private static Predicate<TPeriod> periodContains(final OffsetDateTime targetDateTime) {
        return tPeriod -> isTargetDateInInterval(
            targetDateTime, tPeriod.getDtini(), tPeriod.getDtfin()
        );
    }

    static boolean dayOfWeekMatchesDayNum(final int dayNum, final int targetDayOfWeek) {
        final DayOfWeek dayOfWeek = DayOfWeek.getInstance(dayNum);
        return switch (dayOfWeek) {
            case EVERYDAY -> true;
            case SATURDAY -> isSaturday(targetDayOfWeek);
            case SUNDAY -> isSunday(targetDayOfWeek);
            case MON_TO_FRI -> !isSaturday(targetDayOfWeek) && !isSunday(targetDayOfWeek);
            case MON_TO_SAT -> !isSunday(targetDayOfWeek);
            default -> false;
        };
    }

    static boolean isSaturday(final int targetDay) {
        return targetDay == SATURDAY.getDayNum();
    }

    static boolean isSunday(final int targetDay) {
        return targetDay == SUNDAY.getDayNum();
    }
}
