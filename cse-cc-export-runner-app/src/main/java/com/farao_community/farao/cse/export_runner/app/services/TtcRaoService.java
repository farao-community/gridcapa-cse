/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.data.cnec.CracResultsHelper;
import com.farao_community.farao.cse.data.ttc_rao.TtcRao;
import com.farao_community.farao.cse.data.xsd.ttc_rao.CseRaoResult;
import com.farao_community.farao.cse.export_runner.app.FileUtil;
import com.farao_community.farao.cse.runner.api.resource.CseExportRequest;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
public class TtcRaoService {

    private final FileExporter fileExporter;

    public TtcRaoService(FileExporter fileExporter) {
        this.fileExporter = fileExporter;
    }

    public String saveTtcRao(CseExportRequest request, CseCracCreationContext cracCreationContext, RaoResult raoResult, Network network, Map<String, Integer> preprocessedPsts) {
        CracResultsHelper cracResultsHelper = new CracResultsHelper(
            cracCreationContext,
            raoResult,
            network);
        CseRaoResult cseRaoResult = TtcRao.generate(request.getTargetProcessDateTime(), cracResultsHelper, preprocessedPsts);
        return fileExporter.saveTtcRao(cseRaoResult, request.getProcessType(), request.getTargetProcessDateTime(), FileUtil.getFilenameFromUrl(request.getCgmUrl()));
    }

    public String saveFailedTtcRao(CseExportRequest request) {
        CseRaoResult cseRaoResult = TtcRao.failed(request.getTargetProcessDateTime());
        return fileExporter.saveTtcRao(cseRaoResult, request.getProcessType(), request.getTargetProcessDateTime(), FileUtil.getFilenameFromUrl(request.getCgmUrl()));
    }
}
