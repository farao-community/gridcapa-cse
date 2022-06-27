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

public class CnecPreventive {

    private CnecCommon cnecCommon;
    private double i;
    private double iBeforeOptimisation;
    private double iMax;

    public CnecCommon getCnecCommon() {
        return cnecCommon;
    }

    public void setCnecCommon(CnecCommon cnecCommon) {
        this.cnecCommon = cnecCommon;
    }

    public double getI() {
        return i;
    }

    public void setI(double i) {
        this.i = i;
    }

    public double getiMax() {
        return iMax;
    }

    public void setiMax(double iMax) {
        this.iMax = iMax;
    }

    public double getiBeforeOptimisation() {
        return iBeforeOptimisation;
    }

    public void setiBeforeOptimisation(double iBeforeOptimisation) {
        this.iBeforeOptimisation = iBeforeOptimisation;
    }
}
