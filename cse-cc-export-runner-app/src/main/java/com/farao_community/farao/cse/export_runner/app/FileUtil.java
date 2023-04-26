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
import java.util.regex.Pattern;

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

    public static String getFileVersion(String cgmFilename, ProcessType processType) {

        if (processType == ProcessType.IDCC) {
            // The naming rule of the initial cgm for Idcc process is YYYYMMDD_hhmm_NNp_Transit_CSEq.uct
            // with q is the version number
            return cgmFilename.substring(29, cgmFilename.indexOf('.'));
        } else if (processType == ProcessType.D2CC) {
            // The naming rule of the initial cgm for D2cc process is YYYYMMDD_hhmm_2Dp_CO_Transit_CSEq.uct
            // with q is the version number
            return cgmFilename.substring(32, cgmFilename.indexOf('.'));
        } else {
            throw new CseInternalException(String.format("Process type %s is not handled", processType));
        }
    }

    public static void checkCgmFileName(String cgmFileUrl, ProcessType processType) {
        String cgmFilename = getFilenameFromUrl(cgmFileUrl);
        if (processType == ProcessType.IDCC) {
            if (!Pattern.matches("[0-9]{8}_[0-9]{4}_[0-9]{3}_Transit_CSE[0-9]+.(uct|UCT)", cgmFilename)) {
                throw new CseDataException(String.format("CGM file name %s is incorrect for process %s.", cgmFilename, processType.name()));
            }
        } else if (processType == ProcessType.D2CC) {
            if (!Pattern.matches("[0-9]{8}_[0-9]{4}_2D[0-9]_CO_Transit_CSE[0-9]+.(uct|UCT)", cgmFilename)) {
                throw new CseDataException(String.format("CGM file name %s is incorrect for process %s.", cgmFilename, processType.name()));
            }
        } else {
            throw new CseInternalException(String.format("Process type %s is not handled", processType));
        }
    }
}
