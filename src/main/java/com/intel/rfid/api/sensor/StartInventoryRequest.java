/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonRequest;

public class StartInventoryRequest extends JsonRequest {

    public static final String METHOD_NAME = "start_inventory";

    public StartInventoryRequest() {
        method = METHOD_NAME;
    }

    public Params params = new Params();

    public static class Params {
        public boolean perform_select = false;
        public boolean perform_post_match = false;
        public boolean filter_duplicates = false;
        public boolean auto_repeat = false;
        public int delay_time = 0;
    }

}
