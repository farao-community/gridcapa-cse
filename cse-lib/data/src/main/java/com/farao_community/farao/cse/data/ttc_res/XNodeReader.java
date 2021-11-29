package com.farao_community.farao.cse.data.ttc_res;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.data.DataUtil;
import com.farao_community.farao.cse.data.xsd.Xnodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class XNodeReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(XNodeReader.class);

    private XNodeReader() {
        // Should not be instantiated
    }

    public static List<XNode> getXNodes(String filePath) {
        Xnodes xnodes;
        try (InputStream is = new FileInputStream(filePath)) {
            xnodes = DataUtil.unmarshalFromInputStream(is, Xnodes.class);
        } catch (IOException e) {
            String errorMessage = String.format("Unable to find xnodes configuration file at %s", filePath);
            LOGGER.error(errorMessage);
            throw new CseDataException(errorMessage, e);
        } catch (JAXBException e) {
            throw new CseDataException("Unable to unmarshal xnodes file.", e);
        }
        return xnodes.getXnode().stream().map(xnode ->
            new XNode(xnode.getName(), xnode.getArea1(), xnode.getSubarea1(), xnode.getArea2(), xnode.getSubarea2())).collect(Collectors.toList());
    }
}
