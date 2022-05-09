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
        checkInputForcesPrasConsistencyWithCrac(inputForcesPrasIds, crac);
        // call new adder method in farao to add forced_if_available preventive topo RA
        // block below will be deleted
        inputForcesPrasIds.stream().map(crac::getNetworkAction)
            //.filter(na -> isRemedialActionAvailable(na, network))
            .forEach(networkAction -> {
                boolean successApply = networkAction.apply(network);
                if (!successApply) {
                    LOGGER.warn("Remedial action {} couldn't be forced", networkAction.getId());
                }
            });
    }

    void checkInputForcesPrasConsistencyWithCrac(List<String> inputForcesPrasIds, Crac crac) {
        List<String> availablePreventivePrasInCrac = crac.getNetworkActions().stream()
            .filter(na -> na.getUsageMethod(crac.getPreventiveState()).equals(UsageMethod.AVAILABLE))
            .map(NetworkAction::getId)
            .collect(Collectors.toList());
        if (!availablePreventivePrasInCrac.containsAll(inputForcesPrasIds)) {
            throw new CseDataException("inconsistency between crac and inputForcedPras");
        }
    }
}
