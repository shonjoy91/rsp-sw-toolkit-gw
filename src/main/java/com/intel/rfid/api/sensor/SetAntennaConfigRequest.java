/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonRequest;

import java.util.ArrayList;
import java.util.List;

public class SetAntennaConfigRequest extends JsonRequest {

    public static final String METHOD_NAME = "set_antenna_config";

    public List<VirtualPort> params = new ArrayList<>();

    public SetAntennaConfigRequest() { method = METHOD_NAME; }

    public SetAntennaConfigRequest(List<VirtualPort> _virtualPorts) {
        this();
        params.addAll(_virtualPorts);
    }

}
