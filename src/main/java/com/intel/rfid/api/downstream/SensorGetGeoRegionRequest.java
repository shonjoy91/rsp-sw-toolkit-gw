/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.downstream;

import com.intel.rfid.api.common.JsonRequest;

public class SensorGetGeoRegionRequest extends JsonRequest {

  public static final String METHOD_NAME = "get_geo_region";

  // keep default for Jackson mapper
  public SensorGetGeoRegionRequest() { method = METHOD_NAME; }

}
