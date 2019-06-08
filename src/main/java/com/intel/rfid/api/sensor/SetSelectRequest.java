/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonRequest;

public class SetSelectRequest extends JsonRequest {

    public static final String METHOD_NAME = "set_select";

    public Params params = new Params();

    public SetSelectRequest() { method = METHOD_NAME; }

    public static class Params {
        public int criteria_index = 0;
        public EnabledState active_state = EnabledState.UNKNOWN;
        public MemoryBank bank = MemoryBank.UNKNOWN;
        public int offset = 0;
        public int mask_length = 0;
        public String mask_data = "0";
        public SessionFlag target_flag = SessionFlag.UNKNOWN;
        public SelectAction action = SelectAction.UNKNOWN;
    }

}
