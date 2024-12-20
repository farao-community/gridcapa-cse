/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class DataUtil {

    private DataUtil() {
        // Should not be instantiated
    }

    public static <T> T unmarshalFromInputStream(InputStream inputStream, Class<T> clazz) throws JAXBException {
        return JAXBContext.newInstance(clazz)
                .createUnmarshaller()
                .unmarshal(new StreamSource(inputStream), clazz)
                .getValue();
    }

    /**
     * This collector only allows 0 or 1 element in the stream. It returns the result as an optional.
     *
     * @param <T> Type of the element for the collector.
     * @return An empty optional if there is no element in the stream and an optional of the value if there is one.
     * It would throw an error if there are more than one element in the stream.
     */
    public static <T> Collector<T, ?, Optional<T>> toOptional() {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            if (list.isEmpty()) {
                return Optional.empty();
            }
            if (list.size() == 1) {
                return Optional.of(list.get(0));
            }
            throw new CseDataException("List in the XML must contain only 0 or 1 element.");
        });
    }
}

