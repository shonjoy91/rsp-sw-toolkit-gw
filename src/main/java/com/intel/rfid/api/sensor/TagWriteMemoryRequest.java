/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonRequest;

public class TagWriteMemoryRequest extends JsonRequest {

    public static final String METHOD_NAME = "tag_write_memory";

    public Params params = new Params();

    public TagWriteMemoryRequest() { method = METHOD_NAME; }

    public static class Params {
        public String epc = "";
        public int password = 0;
        public MemoryBank bank = MemoryBank.UNKNOWN;
        public int word_offset = 0;
        public int retry_count = 0;
        public String data = "";
    }
}
