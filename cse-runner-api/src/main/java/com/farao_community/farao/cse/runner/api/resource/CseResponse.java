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
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Type("cse-response")
public class CseResponse {
    @Id
    private final String id;
    private final String ttcFileUrl;
    private final String finalCgmFileUrl;
    private final boolean interrupted;
    private final boolean raoFailed;

    @JsonCreator
    public CseResponse(@JsonProperty("id") String id, @JsonProperty("ttcFileUrl") String ttcFileUrl, @JsonProperty("finalCgmFileUrl") String finalCgmFileUrl, @JsonProperty("interrupted") boolean interrupted, @JsonProperty("raoFailed") boolean raoFailed) {
        this.id = id;
        this.ttcFileUrl = ttcFileUrl;
        this.finalCgmFileUrl = finalCgmFileUrl;
        this.interrupted = interrupted;
        this.raoFailed = raoFailed;
    }

    public String getId() {
        return id;
    }

    public String getTtcFileUrl() {
        return ttcFileUrl;
    }

    public String getFinalCgmFileUrl() {
        return finalCgmFileUrl;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public boolean isRaoFailed() {
        return raoFailed;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
