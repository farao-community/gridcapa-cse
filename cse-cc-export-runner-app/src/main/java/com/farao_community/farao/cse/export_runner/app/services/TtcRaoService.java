/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.data.cnec.CracResultsHelper;
import com.farao_community.farao.cse.data.ttc_rao.TtcRao;
import com.farao_community.farao.cse.data.xnode.XNodeReader;
import com.farao_community.farao.cse.data.xsd.ttc_rao.CseRaoResult;
import com.farao_community.farao.cse.export_runner.app.configurations.XNodesConfiguration;
import com.farao_community.farao.cse.runner.api.resource.CseExportRequest;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import org.springframework.stereotype.Service;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
public class TtcRaoService {

    private final FileExporter fileExporter;
    private final XNodesConfiguration xNodesConfiguration;

    public TtcRaoService(FileExporter fileExporter, XNodesConfiguration xNodesConfiguration) {
        this.fileExporter = fileExporter;
        this.xNodesConfiguration = xNodesConfiguration;
    }

    public String saveTtcRao(CseExportRequest request, RaoResult raoResult, Crac crac) {
        CseRaoResult cseRaoResult = TtcRao.generate(request.getTargetProcessDateTime(), raoResult, new CracResultsHelper(crac, raoResult, XNodeReader.getXNodes(xNodesConfiguration.getxNodesFilePath())));
        return fileExporter.saveTtcRao(cseRaoResult, request.getProcessType(), request.getTargetProcessDateTime());
    }

    public String saveFailedTtcRao(CseExportRequest request) {
        CseRaoResult cseRaoResult = TtcRao.failed(request.getTargetProcessDateTime());
        return fileExporter.saveTtcRao(cseRaoResult, request.getProcessType(), request.getTargetProcessDateTime());
    }
}
