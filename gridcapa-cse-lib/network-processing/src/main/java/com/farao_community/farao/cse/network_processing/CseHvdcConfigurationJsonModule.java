/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Philippe Edwards {@literal <philippe.edwards@rte-france.com>}
 */
class CseHvdcConfigurationJsonModule extends SimpleModule {
    public CseHvdcConfigurationJsonModule() {
        super();
        addSerializer(CseHvdcConfiguration.class, new CseHvdcConfigurationSerializer());
        addDeserializer(CseHvdcConfiguration.class, new CseHvdcConfigurationDeserializer());
    }
}
