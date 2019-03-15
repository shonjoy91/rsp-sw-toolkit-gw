/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MobilityProfile {


    private String id = "retail_garment_default";

    // using general slope forumla y = m(x) + b
    // where m is slope in dBm per millisecond
    private double M = -(5 / (double) (10 * 1000));

    // dBm change threshold
    private double T = 6.0;
    // milliseconds of holdoff
    private double A = 60 * 1000;
    // find b such that at 60 seconds, y = 3.0
    // b = y - (m*x)
    private double B = T - (M * A);

    public double getM() { return M; }

    public double getT() { return T; }

    public double getA() { return A; }

    @JsonIgnore
    public double getB() { return B; }

    public String getId() {
        return id;
    }

    public void setM(double _d) {
        M = _d;
        calcB();
    }

    public void setT(double _d) {
        T = _d;
        calcB();
    }

    public void setA(double _d) {
        A = _d;
        calcB();
    }

    public void setId(String _id) {
        id = _id;
    }

    private void calcB() {
        B = T - (M * A);
    }

    @Override
    public String toString() {
        return id + ": " +
               ", M=" + M +
               ", T=" + T +
               ", A=" + A +
               ", B=" + B;
    }
}
