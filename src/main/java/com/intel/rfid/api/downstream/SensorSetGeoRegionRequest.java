/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.downstream;

import com.intel.rfid.api.common.JsonRequest;
import com.intel.rfid.api.data.GeoRegion;

public class SensorSetGeoRegionRequest extends JsonRequest {

  public static final String METHOD_NAME = "set_geo_region";

  public class Params {
    public GeoRegion region;
  }
  public Params params;

  // keep default for Jackson mapper
  public SensorSetGeoRegionRequest() { 
    method = METHOD_NAME;
    params = new Params();
  }
  
  public SensorSetGeoRegionRequest(GeoRegion _region) {
    this();
    params.region = _region;
  }

}
