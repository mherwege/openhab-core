/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.io.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

/**
 * This {@link InputStream} will stream {@link Stream}s as YAML one item at a time. This will reduce memory usage when
 * streaming large collections through the REST interface. The input stream creates one YAML representation at a time
 * from the top level elements of the stream. For best performance a flattened stream should be provided. Otherwise a
 * nested collections YAML representation will be fully transformed into memory.
 *
 * @author Henning Treu - Initial contribution
 * @author Jörg Sautter - Use as SequenceInputStream to simplify the logic
 */
@NonNullByDefault
public class Stream2YAMLInputStream extends InputStream implements YAMLInputStream {

    private final InputStream stream;

    /**
     * Creates a new {@link Stream2YAMLInputStream} backed by the given {@link Stream} source.
     *
     * @param source the {@link Stream} backing this input stream. Must not be null.
     */
    public Stream2YAMLInputStream(Stream<?> source) {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory().disable(Feature.WRITE_DOC_START_MARKER));
        yamlMapper.findAndRegisterModules();
        Iterator<String> iterator = source.map(e -> {
            try {
                return yamlMapper.writeValueAsString(e);
            } catch (JsonProcessingException e1) {
            }
            return null;
        }).iterator();

        Enumeration<InputStream> enumeration = new Enumeration<>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public InputStream nextElement() {
                String[] data = iterator.next().split("\n");
                InputStream is = toStream("- " + String.join("\n  ", data));

                return is;
            }

            private static InputStream toStream(String data) {
                return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            }
        };
        stream = new SequenceInputStream(enumeration);
    }

    @Override
    public int read() throws IOException {
        return stream.read();
    }

    @Override
    public int read(byte @Nullable [] b, int off, int len) throws IOException {
        return stream.read(b, off, len);
    }

    @Override
    public long transferTo(OutputStream target) throws IOException {
        return stream.transferTo(target);
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
