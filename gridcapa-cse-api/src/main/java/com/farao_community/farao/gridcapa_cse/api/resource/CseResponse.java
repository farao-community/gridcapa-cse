/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_cse.api.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Type("cse-response")
public class CseResponse {
    @Id
    private String id;
    private String ttcFileUrl;

    @JsonCreator
    public CseResponse(@JsonProperty("id") String id, @JsonProperty("ttcFileUrl") String ttcFileUrl) {
        this.id = id;
        this.ttcFileUrl = ttcFileUrl;
    }

    public String getId() {
        return id;
    }

    public String getTtcFileUrl() {
        return ttcFileUrl;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
