/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

import java.util.ArrayList;
import java.util.List;

public class AckAlert extends JsonRequest {

    public static final String METHOD_NAME = "ack_alert";


    public AckAlert() { method = METHOD_NAME; }

    public AckAlert(int _alertNumber, boolean _ack, boolean _mute) {
        this();

        Param t = new Param();
        t.alert_number = _alertNumber;
        t.acknowledge = _ack;
        t.mute = _mute;

        params.add(t);
    }

    public List<Param> params = new ArrayList<>();

    public static class Param {
        public int alert_number;
        public boolean acknowledge;
        public boolean mute;
    }

    // not supported as of RSP version 1.0.2
    //public static final String METHOD_NAME = "ack_alert";

    //public Params params = new Params();

    //public AckAlert() { method = METHOD_NAME; }
    //
    //public AckAlert(int _alertNumber, boolean _acknowledge, boolean _mute) {
    //    this();
    //
    //    params.alert_number = _alertNumber;
    //    params.acknowledge = _acknowledge;
    //    params.mute = _mute;
    //}
    //
    //public static class Params {
    //    public int alert_number;
    //    public boolean acknowledge;
    //    public boolean mute;
    //}
}
