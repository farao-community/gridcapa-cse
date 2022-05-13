/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.data.ttc_rao.TtcRao;
import com.farao_community.farao.cse.data.xsd.ttc_rao.CseRaoResult;
import com.farao_community.farao.cse.runner.api.resource.CseExportRequest;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import org.springframework.stereotype.Service;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
public class TtcRaoService {

    private final FileExporter fileExporter;

    public TtcRaoService(FileExporter fileExporter) {
        this.fileExporter = fileExporter;
    }

    public String saveTtcRao(CseExportRequest request, RaoResult raoResult) {
        CseRaoResult cseRaoResult = TtcRao.generate(request.getTargetProcessDateTime(), raoResult);
        return fileExporter.saveTtcRao(cseRaoResult, request.getProcessType(), request.getTargetProcessDateTime());
    }
}
