/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

import java.util.Map;

public class DeviceAlertDetails {
    public long sent_on;
    public String device_id;
    public String facility_id;
    public int alert_number;
    public String alert_description;
    public String severity;
    public Map<String, Object> optional;
}
