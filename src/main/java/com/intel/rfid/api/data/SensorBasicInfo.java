/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

import java.util.ArrayList;
import java.util.List;

public class SensorBasicInfo {

    public String device_id;
    public Connection.State connection_state;
    public ReadState read_state;
    public String behavior_id;
    public String facility_id;
    public Personality personality;
    public List<String> aliases = new ArrayList<>();
    public List<DeviceAlertDetails> alerts = new ArrayList<>();

    public SensorBasicInfo(String _device_id,
                           Connection.State _connectionState,
                           ReadState _readState,
                           String _behaviorId,
                           String _facilityId,
                           Personality _personality,
                           List<String> _aliases,
                           List<DeviceAlertDetails> _alerts) {

        device_id = _device_id;
        connection_state = _connectionState;
        read_state = _readState;
        behavior_id = _behaviorId;
        facility_id = _facilityId;
        personality = _personality;
        aliases.addAll(_aliases);
        alerts.addAll(_alerts);
    }

}
