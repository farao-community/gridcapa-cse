/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.api;

import com.farao_community.farao.cse.runner.api.exception.AbstractCseException;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;
import com.github.jasminb.jsonapi.SerializationFeature;
import com.github.jasminb.jsonapi.exceptions.DocumentSerializationException;
import com.github.jasminb.jsonapi.models.errors.Error;

/**
 * JSON API conversion component
 * Allows automatic conversion from resources or exceptions towards JSON API formatted bytes.
 *
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public class JsonApiConverter {
    private final ObjectMapper objectMapper;

    public JsonApiConverter() {
        objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
            .build();
    }

    public <T> T fromJsonMessage(byte[] jsonMessage, Class<T> tClass) {
        ResourceConverter converter = createConverter(tClass);
        try {
            return converter.readDocument(jsonMessage, tClass).get();
        } catch (Exception e) {
            throw new CseInvalidDataException(String.format("Message cannot be converted to class %s", tClass.getName()), e);
        }

    }

    public <T> byte[] toJsonMessage(T jsonApiObject, Class<T> clazz) {
        ResourceConverter converter = createConverter(clazz);
        JSONAPIDocument<?> jsonApiDocument = new JSONAPIDocument<>(jsonApiObject);
        try {
            return converter.writeDocument(jsonApiDocument);
        } catch (DocumentSerializationException e) {
            throw new CseInternalException("Exception occurred during message conversion", e);
        }
    }

    public byte[] toJsonMessage(AbstractCseException exception) {
        ResourceConverter converter = createConverter();
        JSONAPIDocument<?> jsonApiDocument = new JSONAPIDocument<>(convertExceptionToJsonError(exception));
        try {
            return converter.writeDocument(jsonApiDocument);
        } catch (DocumentSerializationException e) {
            throw new CseInternalException("Exception occurred during message conversion", e);
        }
    }

    private ResourceConverter createConverter(Class<?>... classes) {
        ResourceConverter converter = new ResourceConverter(objectMapper, classes);
        converter.disableSerializationOption(SerializationFeature.INCLUDE_META);
        return converter;
    }

    private Error convertExceptionToJsonError(AbstractCseException exception) {
        Error error = new Error();
        error.setStatus(Integer.toString(exception.getStatus()));
        error.setCode(exception.getCode());
        error.setTitle(exception.getTitle());
        error.setDetail(exception.getDetails());
        return error;
    }
}
