/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.util;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import org.apache.commons.io.FilenameUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class FileUtil {

    private FileUtil() {
        // Should not be instantiated
    }

    public static String getFilenameFromUrl(String url) {
        try {
            return FilenameUtils.getName(new URL(url).getPath());
        } catch (MalformedURLException e) {
            throw new CseInvalidDataException(String.format("URL is invalid: %s", url), e);
        }
    }

    /**
     * Returns three digits extracted from input Cgm filename
     * TT are the difference between business hour and publication hour
     * N is the day of week
     */
    public static String getTTNFromInputCgm(String cgmFilename) {
        Pattern fileNamePattern = Pattern.compile("^\\d{8}_\\d{4}(.*)_(\\d{3})_(.*)\\.(uct|UCT)");
        Matcher checkFileNameMatches = fileNamePattern.matcher(cgmFilename);
        if (checkFileNameMatches.matches()) {
            return checkFileNameMatches.group(2);
        }
        throw new CseDataException(String.format("CGM file %s of IDCC process is badly named", cgmFilename));
    }
}
