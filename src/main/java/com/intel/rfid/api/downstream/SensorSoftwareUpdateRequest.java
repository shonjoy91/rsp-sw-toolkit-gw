/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.downstream;

import com.intel.rfid.api.common.JsonRequest;

public class SensorSoftwareUpdateRequest extends JsonRequest {

  public static final String METHOD_NAME = "software_update";

  public SensorSoftwareUpdateRequest() { method = METHOD_NAME; }

}
