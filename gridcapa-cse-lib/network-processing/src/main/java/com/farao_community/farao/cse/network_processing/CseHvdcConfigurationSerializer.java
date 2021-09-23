/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * @author Philippe Edwards {@literal <philippe.edwards@rte-france.com>}
 */
class CseHvdcConfigurationSerializer extends JsonSerializer<CseHvdcConfiguration> {

    @Override
    public void serialize(CseHvdcConfiguration hvdcConfiguration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) {
        throw new UnsupportedOperationException("CSE HVDC configuration serializer is not implemented yet");
    }
}
