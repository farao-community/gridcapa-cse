/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.ntc;

enum DayOfWeek {
    EVERYDAY(0),
    SATURDAY(6),
    SUNDAY(7),
    MON_TO_FRI(8),
    MON_TO_SAT(9),
    INVALID(-1);

    private final int dayNum;

    DayOfWeek(int dayNum) {
        this.dayNum = dayNum;
    }

    int getDayNum() {
        return dayNum;
    }

    static DayOfWeek getInstance(final int dayNum) {
        return switch (dayNum) {
            case 0 -> EVERYDAY;
            case 6 -> SATURDAY;
            case 7 -> SUNDAY;
            case 8 -> MON_TO_FRI;
            case 9 -> MON_TO_SAT;
            default -> INVALID;
        };
    }

}
