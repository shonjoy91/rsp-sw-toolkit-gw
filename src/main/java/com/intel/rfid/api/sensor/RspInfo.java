/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

public class RspInfo {

    public static final String UNKNOWN = "unknown";
    public String hostname = UNKNOWN;
    public String hwaddress = UNKNOWN;
    public String app_version = UNKNOWN;
    public String module_version = UNKNOWN;
    public int num_physical_ports;
    public boolean motion_sensor;
    public boolean camera;
    public boolean wireless;
    public String configuration_state = UNKNOWN;
    public String operational_state = UNKNOWN;


    @Override
    public String toString() {
        return "{" + "hostname='" + hostname + '\'' +
                ", hwaddress='" + hwaddress + '\'' +
                ", app_version='" + app_version + '\'' +
                ", module_version='" + module_version + '\'' +
                ", num_physical_ports=" + num_physical_ports +
                ", motion_sensor=" + motion_sensor +
                ", wireless=" + wireless +
                ", camera=" + camera +
                ", configuration_state='" + configuration_state + '\'' +
                ", operational_state='" + operational_state + '\'' +
                '}';
    }
}
