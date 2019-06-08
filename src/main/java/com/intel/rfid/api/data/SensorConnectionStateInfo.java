/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

public class SensorConnectionStateInfo {

    public String device_id;
    public Connection.State connection_state;

    public SensorConnectionStateInfo(String _device_id,
                                     Connection.State _connectionState) {

        device_id = _device_id;
        connection_state = _connectionState;
    }

}
