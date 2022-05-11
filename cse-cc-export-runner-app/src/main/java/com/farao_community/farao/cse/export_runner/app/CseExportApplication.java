/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app;

import com.farao_community.farao.cse.export_runner.app.configurations.PiSaConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@SuppressWarnings("HideUtilityClassConstructor")
@SpringBootApplication
@EnableConfigurationProperties({PiSaConfiguration.class})
public class CseExportApplication {
    public static void main(String[] args) {
        SpringApplication.run(CseExportApplication.class, args);
    }
}
