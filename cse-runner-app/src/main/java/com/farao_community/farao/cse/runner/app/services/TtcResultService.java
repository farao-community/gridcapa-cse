/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.services;

import com.farao_community.farao.cse.data.ttc_res.CracResultsHelper;
import com.farao_community.farao.cse.data.ttc_res.TtcResult;
import com.farao_community.farao.cse.data.ttc_res.XNodeReader;
import com.farao_community.farao.cse.data.xsd.ttc_res.Timestamp;
import com.farao_community.farao.cse.runner.app.util.ItalianImport;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy_runner.api.resource.DichotomyResponse;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.app.CseData;
import com.farao_community.farao.cse.runner.app.configurations.XNodesConfiguration;
import com.powsybl.iidm.network.Network;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Collections;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class TtcResultService {

    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final XNodesConfiguration xNodesConfiguration;

    public TtcResultService(FileExporter fileExporter, FileImporter fileImporter, XNodesConfiguration xNodesConfiguration) {
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.xNodesConfiguration = xNodesConfiguration;
    }

    public String saveTtcResult(CseRequest cseRequest, CseData cseData, DichotomyResponse dichotomyResponse, Crac crac) throws IOException {
        TtcResult.TtcFiles ttcFiles = new TtcResult.TtcFiles(
            cseRequest.getCgmUrl(),
            cseData.getJsonCracUrl(),
            cseRequest.getMergedGlskUrl(),
            FilenameUtils.getName(cseRequest.getNtcReductionsUrl()),
            "ntcReductionCreationDatetime",
            dichotomyResponse.getHighestValidStep().getNetworkWithPra().getUrl()
        );

        Network networkAfterDichotomy = fileImporter.importNetwork(dichotomyResponse.getHighestValidStep().getNetworkWithPra().getUrl());
        double finalItalianImport = ItalianImport.compute(networkAfterDichotomy);
        TtcResult.ProcessData processData = new TtcResult.ProcessData(
            cseData.getCseReferenceExchanges().getExchanges(),
            cseData.getReducedSplittingFactors(),
            Collections.emptyMap(),
            dichotomyResponse.getLimitingCause().toString(),
            finalItalianImport,
            cseData.getMniiOffset(),
            cseRequest.getTargetProcessDateTime().toString()
        );

        RaoResult raoResult = fileImporter.importRaoResult(dichotomyResponse.getHighestValidStep().getRaoResult().getUrl(), crac);

        Timestamp timestamp = TtcResult.generate(ttcFiles, processData, new CracResultsHelper(crac, raoResult, XNodeReader.getXNodes(xNodesConfiguration.getxNodesFilePath())));
        return fileExporter.saveTtcResult(timestamp);
    }
}
