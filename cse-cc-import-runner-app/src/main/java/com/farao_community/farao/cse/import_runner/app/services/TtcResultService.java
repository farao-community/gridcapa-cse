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
import com.farao_community.farao.cse.import_runner.app.util.FileUtil;
import com.farao_community.farao.cse.network_processing.busbar_change.BusBarChangeProcessor;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.import_runner.app.configurations.XNodesConfiguration;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreators;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.BusBarChangeSwitches;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.CseCracCreationParameters;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.results.LimitingCause;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Set;

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

    public String saveTtcResult(CseRequest cseRequest, CseData cseData, RaoResponse highestSecureStepRaoResponse, LimitingCause limitingCause, String baseCaseFileUrl, String finalCgmUrl) throws IOException {
        TtcResult.TtcFiles ttcFiles = createTtcFiles(cseRequest, baseCaseFileUrl, finalCgmUrl);
        String networkWithPraUrl = highestSecureStepRaoResponse.getNetworkWithPraFileUrl();
        Network networkAfterDichotomy = fileImporter.importNetwork(networkWithPraUrl);
        double finalItalianImport = BorderExchanges.computeItalianImport(networkAfterDichotomy);
        TtcResult.ProcessData processData = new TtcResult.ProcessData(
            BorderExchanges.computeCseBordersExchanges(networkAfterDichotomy),
            cseData.getReducedSplittingFactors(),
            BorderExchanges.computeCseCountriesBalances(networkAfterDichotomy),
            limitingCause,
            finalItalianImport,
            cseData.getMniiOffset(),
            cseRequest.getTargetProcessDateTime().toString()
        );

        // Important to load rao results from the same instance of CRAC that we send to TTC result creator
        // otherwise the researches in TTC result creation would fail...

        CseCrac nativeCseCrac = fileImporter.importCseCrac(cseRequest.getMergedCracUrl());
        CracCreationParameters cracCreationParameters = integrateBusBarPretreatment(networkAfterDichotomy, nativeCseCrac);
        CseCracCreationContext cseCracCreationContext = (CseCracCreationContext) CracCreators.createCrac(nativeCseCrac, networkAfterDichotomy, cseRequest.getTargetProcessDateTime(), cracCreationParameters);
        Crac cracResult = fileImporter.importCracFromJson(highestSecureStepRaoResponse.getCracFileUrl());
        RaoResult raoResult = fileImporter.importRaoResult(highestSecureStepRaoResponse.getRaoResultFileUrl(), cracResult);
        Timestamp timestamp = TtcResult.generate(ttcFiles, processData, new CracResultsHelper(cseCracCreationContext, raoResult, XNodeReader.getXNodes(xNodesConfiguration.getxNodesFilePath())));
        return fileExporter.saveTtcResult(timestamp, cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType());
    }

    private CracCreationParameters integrateBusBarPretreatment(Network networkAfterDichotomy, CseCrac nativeCseCrac) {
        Set<BusBarChangeSwitches> busBarChangeSwitchesSet = BusBarChangeProcessor.process(networkAfterDichotomy, nativeCseCrac);
        CseCracCreationParameters cseCracCreationParameters = new CseCracCreationParameters();
        cseCracCreationParameters.setBusBarChangeSwitchesSet(busBarChangeSwitchesSet);
        CracCreationParameters cracCreationParameters = CracCreationParameters.load();
        cracCreationParameters.addExtension(CseCracCreationParameters.class, cseCracCreationParameters);
        return cracCreationParameters;
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
