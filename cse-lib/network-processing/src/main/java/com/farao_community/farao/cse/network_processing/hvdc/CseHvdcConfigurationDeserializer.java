/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing.hvdc;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * @author Philippe Edwards {@literal <philippe.edwards@rte-france.com>}
 */
public class CseHvdcConfigurationDeserializer extends JsonDeserializer<CseHvdcConfiguration> {

    @Override
    public CseHvdcConfiguration deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        if (jsonParser.nextToken().isStructEnd()) {
            throw new CseHvdcConfigurationDeserializationException("Hvdc configuration file is empty");
        }
        switch (jsonParser.getCurrentName()) {
            case "cse-hvdc-parameters":
                jsonParser.nextToken();
                return deserializeHvdcList(jsonParser);
            default:
                throw new CseHvdcConfigurationDeserializationException("Only cse-hvdc-parameters is implemented for now.");
        }
    }

    private CseHvdcConfiguration deserializeHvdcList(JsonParser jsonParser) throws IOException {

        CseHvdcConfiguration configuration = new CseHvdcConfiguration();

        while (!jsonParser.nextToken().isStructEnd()) {
            deserializeHvdc(jsonParser, configuration, jsonParser.getCurrentName());
        }
        return configuration;
    }

    private void deserializeHvdc(JsonParser jsonParser, CseHvdcConfiguration hvdcConfiguration, String hvdcId) throws IOException {
        String vscName = null;
        String originSubstationName = null;
        String extremitySubstationName = null;
        Integer numberOfCablesPerPole = null;
        Double rdc = null;
        Double pMax = null;
        Double nominalVdc = null;
        Float lossCoefficient = null;
        jsonParser.nextToken();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "VSC-name":
                    vscName = jsonParser.nextTextValue();
                    break;
                case "origin-substation-name":
                    originSubstationName = jsonParser.nextTextValue();
                    break;
                case "extremity-substation-name":
                    extremitySubstationName = jsonParser.nextTextValue();
                    break;
                case "number-of-cables-per-pole":
                    jsonParser.nextToken();
                    numberOfCablesPerPole = jsonParser.getIntValue();
                    break;
                case "RDC":
                    jsonParser.nextToken();
                    rdc = jsonParser.getDoubleValue();
                    break;
                case "Pmax":
                    jsonParser.nextToken();
                    pMax = jsonParser.getDoubleValue();
                    break;
                case "nominal-VDC":
                    jsonParser.nextToken();
                    nominalVdc = jsonParser.getDoubleValue();
                    break;
                case "loss-coefficient":
                    jsonParser.nextToken();
                    lossCoefficient = jsonParser.getFloatValue();
                    break;
                default:
                    throw new CseHvdcConfigurationDeserializationException(String.format("Attribute '%s' invalid for hvdc %s", jsonParser.getCurrentName(), hvdcId));
            }
        }

        CseHvdc hvdc = new CseHvdc(hvdcId, vscName, originSubstationName, extremitySubstationName, numberOfCablesPerPole, rdc, pMax, nominalVdc, lossCoefficient);
        hvdcConfiguration.addHvdc(hvdc);
    }
}
