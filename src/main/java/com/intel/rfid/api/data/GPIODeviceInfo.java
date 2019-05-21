/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

import java.util.ArrayList;
import java.util.List;

public class GPIODeviceInfo {

    public String device_id;
    public String hwaddress;
    public String app_version;
    public List<GPIOInfo> gpio_info = new ArrayList<>();

}
