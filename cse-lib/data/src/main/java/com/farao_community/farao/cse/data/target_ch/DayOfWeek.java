/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.target_ch;

/**
 * Different interpretation for days of week in target CH file...
 */
enum DayOfWeek {
    SUNDAY(7),
    EVERYDAY(8),
    MONTOSAT(9),
    INVALID(-1);

    private final int daynum;

    DayOfWeek(int daynum) {
        this.daynum = daynum;
    }

    int getDaynum() {
        return daynum;
    }

    static DayOfWeek getInstance(int daynum) {
        switch (daynum) {
            case 7 : return SUNDAY;
            case 8 : return EVERYDAY;
            case 9 : return MONTOSAT;
            default: return INVALID;
        }
    }
}
