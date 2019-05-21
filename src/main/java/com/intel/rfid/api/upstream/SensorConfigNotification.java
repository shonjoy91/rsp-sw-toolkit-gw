/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonNotification;
import com.intel.rfid.api.data.Personality;

import java.util.ArrayList;
import java.util.List;

public class SensorConfigNotification extends JsonNotification {

    public static final String METHOD_NAME = "sensor_config";

    public Params params = new Params();

    public SensorConfigNotification() {
        method = METHOD_NAME;
    }
    
    public SensorConfigNotification(String _device_id,
                                    String _facility_id,
                                    Personality _personality,
                                    String _behavior_id,
                                    List<String> _aliases) {
        this();
        params.device_id = _device_id;
        params.facility_id = _facility_id;
        params.personality = _personality;
        params.behavior_id = _behavior_id;
        params.aliases.addAll(_aliases);
    }

    public class Params{
        public String device_id;
        public String facility_id;
        public Personality personality;
        public String behavior_id;
        public List<String> aliases = new ArrayList<>();
    }

}
