/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

import com.intel.rfid.api.sensor.GeoRegion;

public class OemCfgUpdateInfo {

    public enum Status {IN_PROGRESS, RESET_RADIO, COMPLETE, ERROR, FAIL}

    public long sent_on;
    public String device_id;
    public GeoRegion region;
    public String file;
    public Status status;
    public int current_line_num;
    public int total_lines;
    public String message;

}
