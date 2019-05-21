/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.sensor;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SensorUtilizationStats {

    protected boolean available = false;
    protected boolean on = false;

    protected long cumOnMillis;
    protected long cumAvailableMillis;

    protected long onTimestamp;
    protected long availableTimestamp;

    public synchronized void markOn() {
        // can't be ON without being AVAILABLE
        markAvailableUnsync();
        if (!on) {
            onTimestamp = System.currentTimeMillis();
            on = true;
        }
    }

    public synchronized void markAvailable() {
        markAvailableUnsync();
    }

    private void markAvailableUnsync() {
        if (!available) {
            availableTimestamp = System.currentTimeMillis();
            available = true;
        }
    }

    public synchronized void markUnavailable() {
        // can't be ON without being AVAILABLE
        markOffUnsync();
        if (available) {
            cumAvailableMillis += (System.currentTimeMillis() - availableTimestamp);
            available = false;
        }
    }

    public synchronized void markOff() {
        markOffUnsync();
    }

    private void markOffUnsync() {
        if (on) {
            cumOnMillis += (System.currentTimeMillis() - onTimestamp);
            on = false;
        }
    }


    // this will advance the baseline timestamps to current time
    // to reflect a new time period of utilization
    public synchronized void snapForward() {
        long now = System.currentTimeMillis();
        if (available) { availableTimestamp = now; }
        if (on) { onTimestamp = now; }
        cumAvailableMillis = 0;
        cumOnMillis = 0;
    }

    public synchronized double getUtilizationPercent() {

        long now = System.currentTimeMillis();

        long tmpCumAv = cumAvailableMillis;
        if (available) {
            tmpCumAv += now - availableTimestamp;
        }

        // no divide by 0; probably should go infinite,
        // but this is an error condition
        if (tmpCumAv == 0) {
            return 0.0;
        }

        long tmpCumOn = cumOnMillis;
        if (on) {
            tmpCumOn += now - onTimestamp;
        }

        BigDecimal n = new BigDecimal(tmpCumOn);
        BigDecimal d = new BigDecimal(tmpCumAv);
        return n.divide(d, 2, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public String toString() {
        return "utilizaation: " + getUtilizationPercent();
    }
}

