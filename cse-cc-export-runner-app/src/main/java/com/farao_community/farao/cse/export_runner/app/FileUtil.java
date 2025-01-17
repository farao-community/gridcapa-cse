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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
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
            URL url = new URI(stringUrl).toURL();
            return FilenameUtils.getName(url.getPath());
        } catch (IOException | URISyntaxException | IllegalArgumentException e) {
            throw new CseDataException(String.format("Exception occurred while retrieving file name from : %s", stringUrl), e);
        }
    }

    public static String getFileVersion(String cgmFilename,  ProcessType processType) {
        if (processType != ProcessType.D2CC && processType != ProcessType.IDCC) {
            throw new CseInternalException(String.format("Process type %s is not handled", processType));
        }
        Pattern fileNamePattern = Pattern.compile("^\\d{8}_\\d{4}_(\\d{3}_Transit_CSE|2D\\d_CO_Transit_CSE)(\\d+)\\.(uct|UCT)");
        Matcher checkFileNameMatches = fileNamePattern.matcher(cgmFilename);
        if (checkFileNameMatches.matches()) {
            return checkFileNameMatches.group(2);
        }
        throw new CseDataException(String.format("CGM file %s of process type %s is badly named", cgmFilename, processType));
    }

    public static void checkCgmFileName(String cgmFileUrl, ProcessType processType) {
        String cgmFilename = getFilenameFromUrl(cgmFileUrl);
        switch (processType) {
            case IDCC -> {
                if (!Pattern.matches("\\d{8}_\\d{4}_\\d{3}_Transit_CSE\\d+.(uct|UCT)", cgmFilename)) {
                    throw new CseDataException(String.format("CGM file name %s is incorrect for process %s.", cgmFilename, processType.name()));
                }
            }
            case D2CC -> {
                if (!Pattern.matches("\\d{8}_\\d{4}_2D\\d_CO_Transit_CSE\\d+.(uct|UCT)", cgmFilename)) {
                    throw new CseDataException(String.format("CGM file name %s is incorrect for process %s.", cgmFilename, processType.name()));
                }
            }
            case null, default -> throw new CseInternalException(String.format("Process type %s is not handled", processType));
        }
    }
}
