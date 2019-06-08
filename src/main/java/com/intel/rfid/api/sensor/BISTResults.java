/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.sensor;

import java.util.ArrayList;
import java.util.List;

public class BISTResults {

    public boolean rf_module_error;
    public int rf_status_code;
    public int ambient_temp;
    public int rf_module_temp;
    public long time_alive;
    public int cpu_usage;
    public int mem_used_percent;
    public int mem_total_bytes;
    public boolean camera_installed;
    public boolean temp_sensor_installed;
    public boolean accelerometer_installed;
    public GeoRegion region;
    public List<RfPortStatus> rf_port_statuses = new ArrayList<>();
    public boolean device_moved;

}
