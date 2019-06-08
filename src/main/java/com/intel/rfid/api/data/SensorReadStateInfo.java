/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

public class SensorReadStateInfo {

    public String device_id;
    public ReadState previous_state;
    public ReadState current_state;
    public String behavior_id;

    public SensorReadStateInfo(String _deviceId,
                               ReadState _previousState,
                               ReadState _currentState,
                               String _behaviorId) {

        device_id = _deviceId;
        previous_state = _previousState;
        current_state = _currentState;
        behavior_id = _behaviorId;
    }

}
