/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing;

import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.data.crac.io.cse.parameters.BusBarChangeSwitches;
import com.powsybl.openrao.data.crac.io.cse.parameters.CseCracCreationParameters;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class CracCreationParametersService {
    private CracCreationParametersService() {
        // utility class
    }

    public static CracCreationParameters getCracCreationParameters(final InputStream cracCreationParamsInputStream,
                                                                   final Set<BusBarChangeSwitches> busBarChangeSwitches,
                                                                   final List<String> alignedRaNames) {
        final CracCreationParameters cracCreationParameters = JsonCracCreationParameters.read(cracCreationParamsInputStream);
        CseCracCreationParameters cseCracCreationParameters = cracCreationParameters.getExtension(CseCracCreationParameters.class);
        if (cseCracCreationParameters != null) {
            updateParameters(cseCracCreationParameters, busBarChangeSwitches, alignedRaNames);
        } else {
            cseCracCreationParameters = new CseCracCreationParameters();
            updateParameters(cseCracCreationParameters, busBarChangeSwitches, alignedRaNames);
            cracCreationParameters.addExtension(CseCracCreationParameters.class, cseCracCreationParameters);
        }
        return cracCreationParameters;
    }

    private static void updateParameters(final CseCracCreationParameters cseCracCreationParameters,
                                         final Set<BusBarChangeSwitches> busBarChangeSwitches,
                                         final List<String> alignedRaNames) {
        cseCracCreationParameters.setBusBarChangeSwitchesSet(busBarChangeSwitches);
        if (alignedRaNames != null && !alignedRaNames.isEmpty()) {
            cseCracCreationParameters.setRangeActionGroupsAsString(alignedRaNames);
        }
    }
}
