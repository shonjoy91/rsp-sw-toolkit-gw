/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.schedule;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps track of the last time of an event in a way that
 * may be used concurrently while guaranteeing a monotonic
 * increase. Because it's based on AtomicLong, if the system
 * supports CAS operations, updates occur without blocking.
 */
public class AtomicTimeMillis extends AtomicLong {
    private static final long serialVersionUID = -6890231016788534971L;

    public AtomicTimeMillis() {
        super(0);
    }


    /**
     * @return true if this update is the latest time, or false if
     * another thread with a later timestamp updated the time
     * as this one was attempting to do so.
     */
    public boolean mark() {
        long now = System.currentTimeMillis();
        return updateAndGet(before -> now > before ? now : before) == now;
    }

    /**
     * @return true if the last marked time is within the given number of milliseconds.
     */
    public boolean isWithin(long _millis) {
        return (System.currentTimeMillis() < get() + _millis);
    }
}
