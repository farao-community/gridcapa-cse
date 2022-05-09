package com.farao_community.farao.cse.runner.app.services;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ForcedPrasHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForcedPrasHandler.class);

    public void forcePras(List<String> inputForcesPrasIds, Network network, Crac crac) {
        checkInputForcesPrasConsistencyWithCrac(inputForcesPrasIds, crac, network);
        // call new adder method in farao to add forced_if_available preventive topo RA
    }

    void checkInputForcesPrasConsistencyWithCrac(List<String> inputForcesPrasIds, Crac crac, Network network) {
        List<String> availablePreventivePrasInCrac = crac.getNetworkActions().stream()
            .filter(na -> na.getUsageMethod(crac.getPreventiveState()).equals(UsageMethod.AVAILABLE))
            .map(NetworkAction::getId)
            .collect(Collectors.toList());

        List<String> wrongInputForcedPras = inputForcesPrasIds.stream()
            .filter(i -> !availablePreventivePrasInCrac.contains(i))
            .collect(Collectors.toList());

        if (!wrongInputForcedPras.isEmpty()) {
            throw new CseDataException(String.format("inconsistency between crac and inputForcedPras, crac file does not contain these topological remedial actions: %s", wrongInputForcedPras));
        }

        List<NetworkAction> applicableRemedialActions = inputForcesPrasIds.stream().map(crac::getNetworkAction).filter(networkAction -> {
            boolean applySuccess = networkAction.apply(network);
            if (!applySuccess) {
                LOGGER.warn("Network action {} will not be forced because not available", networkAction.getId());
            }
            return applySuccess;
        }).collect(Collectors.toList());
        if (applicableRemedialActions.isEmpty()) {
            throw new CseDataException("None of input Forced Pras can be applied, It is unnecessary to run the computation again");
        }
    }
}
