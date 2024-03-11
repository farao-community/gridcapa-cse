/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing;

import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.cse.parameters.BusBarChangeSwitches;
import com.powsybl.openrao.data.craccreation.creator.cse.parameters.CseCracCreationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Set;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class CracCreationParametersService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CracCreationParametersService.class);

    private CracCreationParametersService() {
        // utility class
    }

    public static CracCreationParameters getCracCreationParameters(Path jsonFile, Set<BusBarChangeSwitches> busBarChangeSwitches) {
        CracCreationParameters cracCreationParameters = loadCracCreationParametersJson(jsonFile);
        CseCracCreationParameters cseCracCreationParameters = cracCreationParameters.getExtension(CseCracCreationParameters.class);
        if (cseCracCreationParameters != null) {
            cseCracCreationParameters.setBusBarChangeSwitchesSet(busBarChangeSwitches);
        } else {
            cseCracCreationParameters = new CseCracCreationParameters();
            cseCracCreationParameters.setBusBarChangeSwitchesSet(busBarChangeSwitches);
            cracCreationParameters.addExtension(CseCracCreationParameters.class, cseCracCreationParameters);
        }
        return cracCreationParameters;
    }

    private static CracCreationParameters loadCracCreationParametersJson(Path jsonFile) {
        LOGGER.info("Importing Crac Creation Parameters file: {}", jsonFile.getFileName());
        return JsonCracCreationParameters.read(jsonFile);
    }

}
