/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.configurations;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Configuration
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProcessConfiguration {

    @Value("${cse-cc-runner.zone-id}")
    String zoneId;
    @Value("${cse-cc-runner.trm}")
    Double trm;
    @Value("${cse-cc-runner.outputs.initial-cgm}")
    String initialCgm;
    @Value("${cse-cc-runner.outputs.final-cgm}")
    String finalCgm;
    @Value("${cse-cc-runner.outputs.ttc-res}")
    String ttcRes;


}
