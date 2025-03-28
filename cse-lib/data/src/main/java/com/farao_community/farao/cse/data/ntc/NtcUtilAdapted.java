/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.ntc;

import com.farao_community.farao.cse.data.DateTimeUtil;
import com.farao_community.farao.cse.data.xsd.ntc_adapted.TDay;
import com.farao_community.farao.cse.data.xsd.ntc_adapted.TDayOfWeek;
import com.farao_community.farao.cse.data.xsd.ntc_adapted.TLine;
import com.farao_community.farao.cse.data.xsd.ntc_adapted.TNTC;
import com.farao_community.farao.cse.data.xsd.ntc_adapted.TPeriod;
import com.farao_community.farao.cse.data.xsd.ntc_adapted.TTimeInterval;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.farao_community.farao.cse.data.DataUtil.toOptional;

final class NtcUtilAdapted {
    static final String ABSOLUTE = "absolute";

    private NtcUtilAdapted() {
        // Should not be instantiated
    }

    static Optional<TNTC> getTNtcFromLine(OffsetDateTime targetDateTime, TLine tLine) {
        return getTNtcFromPeriods(targetDateTime, tLine.getPeriod())
                .stream()
                .collect(toOptional());
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
        return NtcUtil.isTargetDayOfWeekMatchWithDayNum(daynum, targetDayZOfWeek);
    }
}
