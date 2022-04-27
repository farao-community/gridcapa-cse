/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.app;

import com.farao_community.farao.cse.runner.app.configurations.MendrisioConfiguration;
import com.farao_community.farao.cse.runner.app.configurations.PiSaConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@SuppressWarnings("HideUtilityClassConstructor")
@SpringBootApplication
@EnableConfigurationProperties({PiSaConfiguration.class, MendrisioConfiguration.class})
public class CseApplication {
    public static void main(String[] args) {
        SpringApplication.run(CseApplication.class, args);
    }
}
