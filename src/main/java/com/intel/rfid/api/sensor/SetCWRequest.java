/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonRequest;

public class SetCWRequest extends JsonRequest {

    public static final String METHOD_NAME = "set_cw";

    public Params params = new Params();

    public SetCWRequest() { method = METHOD_NAME; }

    public static class Params {
        public EnabledState state = EnabledState.UNKNOWN;
        public int physical_port = 0;
        public int channel = 0;
        public ChannelType type = ChannelType.UNKNOWN;
        public float power_level = 30.5f;
    }

}
