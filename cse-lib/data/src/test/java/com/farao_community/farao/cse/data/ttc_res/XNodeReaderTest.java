package com.farao_community.farao.cse.data.ttc_res;

import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class XNodeReaderTest {
    @Test
    void testXNodesLoading() {
        String path = Objects.requireNonNull(getClass().getResource("cvg_xnodes_20200714.xml")).getPath();
        assertEquals(413, XNodeReader.getXNodes(path).size());
    }
}
