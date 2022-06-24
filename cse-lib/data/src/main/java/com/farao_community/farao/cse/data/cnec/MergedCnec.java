/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.cnec;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */

public class MergedCnec {

    private CnecCommon cnecCommon;
    private double iAfterOutage;
    private double iMaxAfterOutage;
    private double iAfterCra;
    private double iMaxAfterCra;
    private double iAfterSps;
    private double iMaxAfterSps;

    public CnecCommon getCnecCommon() {
        return cnecCommon;
    }

    public void setCnecCommon(CnecCommon cnecCommon) {
        this.cnecCommon = cnecCommon;
    }

    public double getiAfterOutage() {
        return iAfterOutage;
    }

    public void setiAfterOutage(double iAfterOutage) {
        this.iAfterOutage = iAfterOutage;
    }

    public double getiMaxAfterOutage() {
        return iMaxAfterOutage;
    }

    public void setiMaxAfterOutage(double iMaxAfterOutage) {
        this.iMaxAfterOutage = iMaxAfterOutage;
    }

    public double getiAfterCra() {
        return iAfterCra;
    }

    public void setiAfterCra(double iAfterCra) {
        this.iAfterCra = iAfterCra;
    }

    public double getiMaxAfterCra() {
        return iMaxAfterCra;
    }

    public void setiMaxAfterCra(double iMaxAfterCra) {
        this.iMaxAfterCra = iMaxAfterCra;
    }

    public double getiAfterSps() {
        return iAfterSps;
    }

    public void setiAfterSps(double iAfterSps) {
        this.iAfterSps = iAfterSps;
    }

    public double getiMaxAfterSps() {
        return iMaxAfterSps;
    }

    public void setiMaxAfterSps(double iMaxAfterSps) {
        this.iMaxAfterSps = iMaxAfterSps;
    }
}
