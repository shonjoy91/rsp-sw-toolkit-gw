/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

public class AckAlert extends JsonRequest {

    public static final String METHOD_NAME = "ack_alert";

    public Params params = new Params();

    public AckAlert() { method = METHOD_NAME; }

    public AckAlert(int _alertNumber, boolean _ack, boolean _mute) {
        this();

        params.alert_number = _alertNumber;
        params.acknowledge = _ack;
        params.mute = _mute;
    }

    public static class Params {
        public int alert_number;
        public boolean acknowledge;
        public boolean mute;
    }
}
