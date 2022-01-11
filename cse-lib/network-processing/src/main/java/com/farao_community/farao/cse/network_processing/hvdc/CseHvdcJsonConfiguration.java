/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing.hvdc;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * @author Philippe Edwards {@literal <philippe.edwards@rte-france.com>}
 */
public final class CseHvdcJsonConfiguration {

    private CseHvdcJsonConfiguration() {

    }

    public static CseHvdcConfiguration importConfiguration(FileInputStream inputStream) {
        Objects.requireNonNull(inputStream, "Hvdcs configuration import on null input stream is invalid");
        try {
            ObjectMapper objectMapper = preparedObjectMapper();
            return objectMapper.readValue(inputStream, CseHvdcConfiguration.class);
        } catch (IOException e) {
            throw new CseHvdcConfigurationDeserializationException(e);
        }
    }

    private static ObjectMapper preparedObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new CseHvdcConfigurationJsonModule());
        return objectMapper;
    }
}

