/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.target_ch;

import com.farao_community.farao.cse.data.DateTimeUtil;
import org.apache.commons.lang3.NotImplementedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.farao_community.farao.cse.data.DataUtil.toOptional;

final class TargetChUtil {

    private TargetChUtil() {
        // Should not be instantiated
    }

    static Optional<TPeriod> getMatchingPeriod(OffsetDateTime targetDateTime, List<TPeriod> tPeriods) {
        return tPeriods.stream()
                .filter(tPeriod -> DateTimeUtil.isTargetDateInInterval(targetDateTime, tPeriod.getTini(), tPeriod.getTfin()))
                .collect(toOptional());
    }

    static Optional<Double> getFixedFlowFromDaysOfWeek(OffsetDateTime targetDateTime, List<TDayOfWeek> tDayOfWeeks) {
        return tDayOfWeeks.stream()
                .filter(tDayOfWeek -> isTargetDayOfWeekMatchWithDayNum(tDayOfWeek.getDaynum(), targetDateTime.getDayOfWeek().getValue()))
                .filter(tDayOfWeek -> getFixedFlowFromTimeIntervals(targetDateTime, tDayOfWeek.getTimeInterval()).isPresent())
                .collect(toOptional())
                .map(TDayOfWeek::getTimeInterval)
                .flatMap(tTimeIntervals -> getFixedFlowFromTimeIntervals(targetDateTime, tTimeIntervals));
    }

    static Optional<Double> getFixedFlowFromTimeIntervals(OffsetDateTime targetDateTime, List<TTimeInterval> tTimeIntervals) {
        return tTimeIntervals.stream()
                .filter(tTimeInterval -> DateTimeUtil.isTargetDateInInterval(targetDateTime, tTimeInterval.getTini(), tTimeInterval.getTfin()))
                .collect(toOptional())
                .map(TTimeInterval::getFixedFlow)
                .map(TFixedFlow::getValue)
                .map(Short::doubleValue);
    }

    static boolean isTargetDayOfWeekMatchWithDayNum(int daynum, int targetDayZOfWeek) {
        DayOfWeek dayOfWeek = DayOfWeek.getInstance(daynum);
        switch (dayOfWeek) {
            case EVERYDAY:
                return true;
            case SUNDAY:
                return targetDayZOfWeek == DayOfWeek.SUNDAY.getDaynum();
            case MONTOSAT:
                return targetDayZOfWeek != DayOfWeek.SUNDAY.getDaynum();
            default:
                throw new NotImplementedException(String.format("The use of daynum %d must be implemented for target CH.", daynum));
        }
    }
}
