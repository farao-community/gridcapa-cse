/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.target_ch;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.data.DateTimeUtil;
import org.apache.commons.lang3.NotImplementedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

final class TargetChUtil {

    private TargetChUtil() {
        // Should not be instantiated
    }

    static <T> Collector<T, ?, Optional<T>> toSingleton(String message) {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            if (list.isEmpty()) {
                return Optional.empty();
            }
            if (list.size() == 1) {
                return Optional.of(list.get(0));
            }
            throw new CseDataException(message);
        });
    }

    static Optional<TPeriod> getMatchingPeriod(OffsetDateTime targetDateTime, List<TPeriod> tPeriods) {
        return tPeriods.stream()
                .filter(tPeriod -> DateTimeUtil.isTargetDateInInterval(targetDateTime, tPeriod.getTini(), tPeriod.getTfin()))
                .collect(toSingleton(String.format("Several period definitions for target date %s", targetDateTime.toString())));
    }

    static Optional<Double> getFixedFlowFromDaysOfWeek(OffsetDateTime targetDateTime, List<TDayOfWeek> tDayOfWeeks) {
        Optional<TDayOfWeek> dayOfWeek = tDayOfWeeks.stream()
                .filter(tDayOfWeek -> isTargetDayOfWeekMatchWithDayNum(tDayOfWeek.getDaynum(), targetDateTime.getDayOfWeek().getValue()))
                .filter(tDayOfWeek -> getFixedFlowFromTimeIntervals(targetDateTime, tDayOfWeek.getTimeInterval()).isPresent())
                .collect(toSingleton(String.format("Several matching day of week in period for target date %s", targetDateTime.toString())));

        return dayOfWeek
                .map(tDay -> getFixedFlowFromTimeIntervals(targetDateTime, dayOfWeek.get().getTimeInterval()).get());
    }

    static Optional<Double> getFixedFlowFromTimeIntervals(OffsetDateTime targetDateTime, List<TTimeInterval> tTimeIntervals) {
        Optional<TTimeInterval> timeInterval = tTimeIntervals.stream()
                .filter(tTimeInterval -> DateTimeUtil.isTargetDateInInterval(targetDateTime, tTimeInterval.getTini(), tTimeInterval.getTfin()))
                .collect(toSingleton(String.format("Several matching time interval for target date %s", targetDateTime.toString())));

        return timeInterval.map(TTimeInterval::getFixedFlow).map(TFixedFlow::getValue).map(Short::doubleValue);
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
