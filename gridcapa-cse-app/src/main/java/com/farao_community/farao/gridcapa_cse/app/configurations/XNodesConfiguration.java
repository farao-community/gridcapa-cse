/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_cse.app.configurations;

import com.farao_community.farao.cse.data.DataUtil;
import com.farao_community.farao.cse.data.ttc_res.XNode;
import com.farao_community.farao.cse.data.xsd.Xnodes;
import com.farao_community.farao.gridcapa_cse.api.exception.CseInvalidDataException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.xml.bind.JAXBException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Configuration
public class XNodesConfiguration {

    private List<XNode> xNodes;

    @Value("${cse-cc.xnodes.filename}")
    private String xNodesFilename;

    public List<XNode> getXNodes() {
        if (xNodes == null) {
            Xnodes xnodes;
            try {
                xnodes = DataUtil.unmarshalFromInputStream(getClass().getResourceAsStream("/xnodes/" + xNodesFilename), Xnodes.class);
            } catch (JAXBException e) {
                throw new CseInvalidDataException("Xnodes configuration file is not readable.", e);
            }
            xNodes = xnodes.getXnode().stream().map(xnode ->
                new XNode(xnode.getName(), xnode.getArea1(), xnode.getSubarea1(), xnode.getArea2(), xnode.getSubarea2())).collect(Collectors.toList());
        }
        return xNodes;
    }
}
