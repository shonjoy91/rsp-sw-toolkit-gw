/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonRequest;

public class AckAlertRequest extends JsonRequest {

    public static final String METHOD_NAME = "ack_alert";

    public Params params = new Params();

    public AckAlertRequest() { method = METHOD_NAME; }

    public AckAlertRequest(int _alertNumber, boolean _acknowledge, boolean _mute) {
        this();

        params.alert_number = _alertNumber;
        params.acknowledge = _acknowledge;
        params.mute = _mute;
    }

    public static class Params {
        public int alert_number;
        public boolean acknowledge;
        public boolean mute;
    }
}
