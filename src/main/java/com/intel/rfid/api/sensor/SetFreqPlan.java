/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonRequest;

public class SetFreqPlan extends JsonRequest {

    public static final String METHOD_NAME = "set_freq_plan";

    public Params params = new Params();

    public SetFreqPlan() { method = METHOD_NAME; }

    public SetFreqPlan(FreqPlan _freq_plan) {
        this();
        params.freq_plan = _freq_plan;
    }

    public static class Params {
        public FreqPlan freq_plan = FreqPlan.UNKNOWN;
    }

}
