/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.downstream;

import com.intel.rfid.api.common.JsonNotification;
import com.intel.rfid.api.data.GeoRegion;

public class OemCfgUpdateNotification extends JsonNotification {

    public static final String METHOD_NAME = "oem_cfg_update_status";

    public enum Status { IN_PROGRESS, RESET_RADIO, COMPLETE, ERROR, FAIL }
    public Params params = new Params();

    public OemCfgUpdateNotification() {
        method = METHOD_NAME;
    }

    public static class Params {
        public long sent_on;
        public String device_id;
        public GeoRegion region;
        public String file;
        public Status status;
        public int current_line_num;
        public int total_lines;
        public String message;
    }

}
