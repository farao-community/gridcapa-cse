/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.target_ch;

import com.powsybl.openrao.data.crac.io.commons.ucte.UcteFlowElementHelper;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzer;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;

import jakarta.xml.bind.JAXBException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class LineFixedFlows {
    private final Map<String, List<OutageInformation>> outagesInformationPerLineId;

    public static LineFixedFlows create(OffsetDateTime targetDateTime, InputStream targetChInputStream, boolean isImportEcProcess) throws JAXBException {
        if (!isImportEcProcess) {
            return new LineFixedFlows(targetDateTime, TargetChDocument.create(targetChInputStream));
        } else {
            return new LineFixedFlows(targetDateTime, TargetChDocumentAdapted.create(targetChInputStream));
        }
    }

    private LineFixedFlows(OffsetDateTime targetDateTime, TargetChDocument targetChDocument) {
        this.outagesInformationPerLineId = targetChDocument.getOutagesInformationPerLineId(targetDateTime);
    }

    private LineFixedFlows(OffsetDateTime targetDateTime, TargetChDocumentAdapted targetChDocument) {
        this.outagesInformationPerLineId = targetChDocument.getOutagesInformationPerLineId(targetDateTime);
    }

    /**
     * Find the fixed flow defined for the given line in the target CH file. It depends on the effective outages in the
     * network. It is possible to have no new definition of flow for a line in this file.
     *
     * @param lineId: ID of the line that should match the ID in the network
     * @param network: Network on which to check whether outages are applied or not
     * @param ucteNetworkAnalyzer: For CSE network must be imported from UCTE file.
     *                         This helps to match corresponding line ID in the network
     * @return An optional of the corrected flow on the line given the network. Optional would be empty if there is
     * no new definition of the flow in the target CH file
     */
    public Optional<Double> getFixedFlow(String lineId, Network network, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        List<OutageInformation> outagesInformation = outagesInformationPerLineId.get(lineId);
        Optional<Double> fixedFlow = Optional.empty();
        if (outagesInformation == null) {
            return fixedFlow;
        }
        for (OutageInformation outageInformation : outagesInformation) {
            Optional<Branch<?>> optBranch = findBranch(outageInformation, network, ucteNetworkAnalyzer);
            if (optBranch.isPresent()) {
                Branch<?> branch = optBranch.get();
                double outageFixedFlow = outageInformation.getFixedFlow();
                if (isOutOfService(branch) && (fixedFlow.isEmpty() || fixedFlow.get() > outageFixedFlow)) {
                    fixedFlow = Optional.of(outageFixedFlow);
                }
            }
        }
        return fixedFlow;
    }

    private static Optional<Branch<?>> findBranch(OutageInformation outageInformation, Network network, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        UcteFlowElementHelper ucteCnecElementHelper = new UcteFlowElementHelper(
                outageInformation.getFromNode(),
                outageInformation.getToNode(),
                outageInformation.getOrderCode(),
                ucteNetworkAnalyzer
        );
        if (ucteCnecElementHelper.isValid()) {
            return Optional.of(network.getBranch(ucteCnecElementHelper.getIdInNetwork()));
        } else {
            return Optional.empty();
        }
    }

    private static boolean isOutOfService(Branch<?> branch) {
        return !branch.getTerminal1().isConnected() || !branch.getTerminal2().isConnected();
    }
}
