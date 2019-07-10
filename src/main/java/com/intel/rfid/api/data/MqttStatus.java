/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

import java.util.ArrayList;
import java.util.List;

public class MqttStatus {

    public Connection.State connection_state = Connection.State.DISCONNECTED;
    public String broker_uri = "unknown";
    public List<String> subscribes = new ArrayList<>();
    public List<String> publishes = new ArrayList<>();

}
