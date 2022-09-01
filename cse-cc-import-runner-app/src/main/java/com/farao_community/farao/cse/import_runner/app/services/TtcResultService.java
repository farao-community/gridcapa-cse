/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.computation.BorderExchanges;
import com.farao_community.farao.cse.data.cnec.CracResultsHelper;
import com.farao_community.farao.cse.data.ttc_res.TtcResult;
import com.farao_community.farao.cse.data.xnode.XNodeReader;
import com.farao_community.farao.cse.data.xsd.ttc_res.Timestamp;
import com.farao_community.farao.cse.import_runner.app.dichotomy.DichotomyRaoResponse;
import com.farao_community.farao.cse.import_runner.app.util.FileUtil;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.import_runner.app.configurations.XNodesConfiguration;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.results.LimitingCause;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.io.*;

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

    public String saveFailedTtcResult(CseRequest cseRequest, String baseCaseFileUrl, TtcResult.FailedProcessData.FailedProcessReason failedProcessReason) {
        TtcResult.TtcFiles ttcFiles = createTtcFiles(cseRequest, baseCaseFileUrl, baseCaseFileUrl);
        Timestamp timestamp = TtcResult.generate(ttcFiles, new TtcResult.FailedProcessData(
            cseRequest.getTargetProcessDateTime().toString(),
            failedProcessReason
        ));
        return fileExporter.saveTtcResult(timestamp, cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType());
    }

    public String saveTtcResult(CseRequest cseRequest, CseData cseData, CseCracCreationContext cseCracCreationContext, DichotomyRaoResponse highestSecureStepRaoResponse, LimitingCause limitingCause, String baseCaseFileUrl, String finalCgmUrl) throws IOException {
        TtcResult.TtcFiles ttcFiles = createTtcFiles(cseRequest, baseCaseFileUrl, finalCgmUrl);
        String networkWithPraUrl = highestSecureStepRaoResponse.getRaoResponse().getNetworkWithPraFileUrl();
        Network networkWithPra = fileImporter.importNetwork(networkWithPraUrl);
        double finalItalianImport = BorderExchanges.computeItalianImport(networkWithPra);
        TtcResult.ProcessData processData = new TtcResult.ProcessData(
            highestSecureStepRaoResponse.getForcedPrasIds(),
            BorderExchanges.computeCseBordersExchanges(networkWithPra),
            cseData.getReducedSplittingFactors(),
            BorderExchanges.computeCseCountriesBalances(networkWithPra),
            limitingCause,
            finalItalianImport,
            cseData.getMniiOffset(),
            cseRequest.getTargetProcessDateTime().toString()
        );

        RaoResult raoResult = fileImporter.importRaoResult(highestSecureStepRaoResponse.getRaoResponse().getRaoResultFileUrl(), cseCracCreationContext.getCrac());
        CracResultsHelper cracResultsHelper = new CracResultsHelper(
            cseCracCreationContext, raoResult, XNodeReader.getXNodes(xNodesConfiguration.getxNodesFilePath()));
        Timestamp timestamp = TtcResult.generate(ttcFiles, processData, cracResultsHelper);
        return fileExporter.saveTtcResult(timestamp, cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType());
    }

    private static TtcResult.TtcFiles createTtcFiles(CseRequest cseRequest, String baseCaseFileUrl, String finalCgmUrl) {
        return new TtcResult.TtcFiles(
            FileUtil.getFilenameFromUrl(baseCaseFileUrl),
            FileUtil.getFilenameFromUrl(cseRequest.getCgmUrl()),
            FileUtil.getFilenameFromUrl(cseRequest.getMergedCracUrl()),
            FileUtil.getFilenameFromUrl(cseRequest.getMergedGlskUrl()),
            FileUtil.getFilenameFromUrl(cseRequest.getNtcReductionsUrl()),
            "ntcReductionCreationDatetime",
            FileUtil.getFilenameFromUrl(finalCgmUrl)
        );
    }
}
