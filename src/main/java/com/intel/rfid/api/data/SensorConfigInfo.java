/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

import java.util.ArrayList;
import java.util.List;

public class SensorConfigInfo {

    public String device_id;
    public String facility_id;
    public Personality personality;
    public List<String> aliases = new ArrayList<>();

    public SensorConfigInfo(String _device_id,
                            String _facilityId,
                            Personality _personality,
                            List<String> _aliases) {

        device_id = _device_id;
        facility_id = _facilityId;
        personality = _personality;
        aliases.addAll(_aliases);
    }

}
