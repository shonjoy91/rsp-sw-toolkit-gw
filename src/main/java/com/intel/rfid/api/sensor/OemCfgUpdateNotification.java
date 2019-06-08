/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.api.data.OemCfgUpdateInfo;

public class OemCfgUpdateNotification extends JsonNotification {

    public static final String METHOD_NAME = "oem_cfg_update_status";

    public OemCfgUpdateInfo params;

    public OemCfgUpdateNotification() {
        method = METHOD_NAME;
    }

    public OemCfgUpdateNotification(OemCfgUpdateInfo _info) {
        this();
        params = _info;
    }

}
