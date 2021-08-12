/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data;

public enum DayOfWeek {
    EVERYDAY(0),
    SATURDAY(6),
    SUNDAY(7),
    MONTOFRI(8),
    MONTOSAT(9),
    INVALID(-1);

    private final int daynum;

    DayOfWeek(int daynum) {
        this.daynum = daynum;
    }

    public int getDaynum() {
        return daynum;
    }

    public static DayOfWeek getInstance(int daynum) {
        switch (daynum) {
            case 0 : return EVERYDAY;
            case 6 : return SATURDAY;
            case 7 : return SUNDAY;
            case 8 : return MONTOFRI;
            case 9 : return MONTOSAT;
            default: return INVALID;
        }
    }

}
