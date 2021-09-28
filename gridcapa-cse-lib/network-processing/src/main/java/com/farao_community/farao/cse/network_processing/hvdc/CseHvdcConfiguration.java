/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing.hvdc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Philippe Edwards {@literal <philippe.edwards@rte-france.com>}
 */
public class CseHvdcConfiguration {
    private final List<CseHvdc> hvdcs = new ArrayList<>();

    public void addHvdc(CseHvdc hvdc) {
        hvdcs.add(Objects.requireNonNull(hvdc, "Hvdc configuration does not allow adding null hvdc."));
    }

    public List<CseHvdc> getHvdcs() {
        return hvdcs;
    }
}
