/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.helpers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Jackson {

    // ObjectMapper is thread safe if not reconfigured
    protected static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

    }

    public static ObjectMapper getMapper() {
        return mapper;
    }

}
