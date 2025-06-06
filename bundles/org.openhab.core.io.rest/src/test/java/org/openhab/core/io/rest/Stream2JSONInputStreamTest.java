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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.library.types.DateTimeType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

/**
 * Tests {@link Stream2JSONInputStream}.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public class Stream2JSONInputStreamTest {

    private static final Gson GSON = new GsonBuilder().setDateFormat(DateTimeType.DATE_PATTERN_JSON_COMPAT).create();

    @Test
    public void shouldReturnForEmptyStream() throws Exception {
        List<Object> emptyList = List.of();
        Stream2JSONInputStream collection2InputStream = new Stream2JSONInputStream(emptyList.stream());

        assertThat(inputStreamToString(collection2InputStream), is(GSON.toJson(emptyList)));
    }

    @Test
    public void shouldStreamSingleObjectToJSON() throws Exception {
        DummyObject dummyObject = new DummyObject("demoKey", "demoValue");
        List<DummyObject> dummyList = List.of(dummyObject);
        Stream2JSONInputStream collection2InputStream = new Stream2JSONInputStream(Stream.of(dummyObject));

        assertThat(JsonParser.parseString(inputStreamToString(collection2InputStream)),
                is(JsonParser.parseString(GSON.toJson(dummyList))));
    }

    @Test
    public void shouldStreamCollectionStreamToJSON() throws Exception {
        DummyObject dummyObject1 = new DummyObject("demoKey1", "demoValue1");
        DummyObject dummyObject2 = new DummyObject("demoKey2", "demoValue2");
        List<DummyObject> dummyCollection = List.of(dummyObject1, dummyObject2);
        Stream2JSONInputStream collection2InputStream = new Stream2JSONInputStream(dummyCollection.stream());

        assertThat(JsonParser.parseString(inputStreamToString(collection2InputStream)),
                is(JsonParser.parseString(GSON.toJson(dummyCollection))));
    }

    private String inputStreamToString(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unused")
    private static class DummyObject {
        private final String key;
        private final String value;

        DummyObject(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
