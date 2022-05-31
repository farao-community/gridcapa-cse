/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.minio_adapter.starter.GridcapaFileGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MinioMock {

    private final Map<String, MinioEntry> minioEntriesByPath = new HashMap<>();

    public MinioEntry getEntry(String path) {
        return minioEntriesByPath.get(path);
    }

    public void addEntry(String path, MinioEntry minioEntry) {
        minioEntriesByPath.put(path, minioEntry);
    }

    public static final class MinioEntry {
        private final String filename;
        private final GridcapaFileGroup gridcapaFileGroup;
        private final String processTag;
        private final String fileType;

        public MinioEntry(String filename, GridcapaFileGroup gridcapaFileGroup, String processTag, String fileType) {
            this.filename = filename;
            this.gridcapaFileGroup = gridcapaFileGroup;
            this.processTag = processTag;
            this.fileType = fileType;
        }

        public String getFilename() {
            return filename;
        }

        public GridcapaFileGroup getGridcapaFileGroup() {
            return gridcapaFileGroup;
        }

        public String getProcessTag() {
            return processTag;
        }

        public String getFileType() {
            return fileType;
        }
    }
}
