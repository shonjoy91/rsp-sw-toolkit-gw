/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonResponseOK;
import com.intel.rfid.api.data.MqttStatus;

public class DownstreamGetMqttStatusResponse extends JsonResponseOK {

    public DownstreamGetMqttStatusResponse(String _id, MqttStatus _status) {
        super(_id, Boolean.TRUE);
        result = _status;
    }
}
