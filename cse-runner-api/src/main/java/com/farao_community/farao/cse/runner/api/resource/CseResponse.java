/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.api.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Type("cse-response")
@Data
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CseResponse {
    @Id
    String id;
    String ttcFileUrl;
    String finalCgmFileUrl;

    @JsonCreator
    public CseResponse(@JsonProperty("id") String id, @JsonProperty("ttcFileUrl") String ttcFileUrl, @JsonProperty("finalCgmFileUrl") String finalCgmFileUrl) {
        this.id = id;
        this.ttcFileUrl = ttcFileUrl;
        this.finalCgmFileUrl = finalCgmFileUrl;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
