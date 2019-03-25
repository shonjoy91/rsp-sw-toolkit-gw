/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

public class DeviceAlert extends JsonNotification {

    public static final String METHOD_NAME = "device_alert";

    public enum Severity {info, warning, urgent, critical}

    public DeviceAlertDetails params = new DeviceAlertDetails();

    public DeviceAlert() {
        method = METHOD_NAME;
    }

}
