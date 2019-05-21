/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

public enum GPIOPinFunction {
    START_READING,
    STOP_READING,
    SENSOR_CONNECTED,
    SENSOR_DISCONNECTED,
    SENSOR_TRANSMITTING,
    SENSOR_READING_TAGS,
    NOT_ASSIGNED;
}
