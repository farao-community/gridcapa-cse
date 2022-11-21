/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.net.URL;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public final class FileUtil {

    private FileUtil() {
        // Should not be instantiated
    }

    public static String getFilenameFromUrl(String stringUrl) {
        try {
            URL url = new URL(stringUrl);
            return FilenameUtils.getName(url.getPath());
        } catch (IOException e) {
            throw new CseDataException(String.format("Exception occurred while retrieving file name from : %s Cause: %s ", stringUrl, e.getMessage()));
        }
    }

    public static String getFileVersion(String cgmFilename,  ProcessType processType) {
        if (processType == ProcessType.IDCC) {
            // The naming rule of the initial cgm for Idcc process is YYYYMMDD_hhmm_NNp_Transit_CSEq.uct
            // with q is the version number
            return cgmFilename.substring(29, 30);
        } else if (processType == ProcessType.D2CC) {
            // The naming rule of the initial cgm for D2cc process is YYYYMMDD_hhmm_2Dp_CO_Transit_CSEq.uct
            // with q is the version number
            return cgmFilename.substring(32, 33);
        } else {
            throw new CseInternalException(String.format("Process type %s is not handled", processType));
        }
    }
}
