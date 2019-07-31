/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MobilityProfile {

    public static String DEFAULT_ID = "default";

    private String id = DEFAULT_ID;

    /*
      "id": "asset_tracking_default",
      "a": 0.0,
      "m": -.008,
      "t": 6.0
    
      "id": "retail_garment_default",
      "a": 60000.0,
      "m": -.0005,
      "t": 6.0
     */
    // using general slope forumla y = m(x) + b
    // where m is slope in dBm per millisecond
    private double slope = -.008;

    // dBm change threshold
    private double threshold = 6.0;
    // milliseconds of holdoff
    private double holdoff = 0;
    // find b such that at 60 seconds, y = 3.0
    // b = y - (m*x)
    private double y_intercept = threshold - (slope * holdoff);

    public double getSlope() { return slope; }

    public double getThreshold() { return threshold; }

    public double getHoldoff() { return holdoff; }

    @JsonIgnore
    public double getY_intercept() { return y_intercept; }

    public String getId() {
        return id;
    }

    public void setSlope(double _d) {
        slope = _d;
        calcB();
    }

    public void setThreshold(double _d) {
        threshold = _d;
        calcB();
    }

    public void setHoldoff(double _d) {
        holdoff = _d;
        calcB();
    }

    public void setId(String _id) {
        id = _id;
    }

    private void calcB() {
        y_intercept = threshold - (slope * holdoff);
    }

    @Override
    public String toString() {
        return id + ": " +
                ", slope=" + slope +
                ", threshold=" + threshold +
                ", holdoff=" + holdoff +
                ", y_intercept=" + y_intercept;
    }
}
