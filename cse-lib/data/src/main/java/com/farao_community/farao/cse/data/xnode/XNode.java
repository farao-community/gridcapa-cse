/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.xnode;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class XNode {

    private String name;
    private String area1;
    private String area2;
    private String subarea1;
    private String subarea2;

    public XNode(String name, String area1, String subarea1, String area2, String subarea2) {
        this.name = name;
        this.area1 = area1;
        this.area2 = area2;
        this.subarea1 = subarea1;
        this.subarea2 = subarea2;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArea1() {
        return area1;
    }

    public void setArea1(String area1) {
        this.area1 = area1;
    }

    public String getArea2() {
        return area2;
    }

    public void setArea2(String area2) {
        this.area2 = area2;
    }

    public String getSubarea1() {
        return subarea1;
    }

    public void setSubarea1(String subarea1) {
        this.subarea1 = subarea1;
    }

    public String getSubarea2() {
        return subarea2;
    }

    public void setSubarea2(String subarea2) {
        this.subarea2 = subarea2;
    }
}
