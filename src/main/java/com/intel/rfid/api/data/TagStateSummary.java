/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

public class TagStateSummary {
    
    public int PRESENT;
    public int EXITING;
    public int DEPARTED_EXIT;
    public int DEPARTED_POS;

    public void copyFrom(TagStateSummary _other) {
        PRESENT = _other.PRESENT;
        EXITING = _other.EXITING;
        DEPARTED_EXIT = _other.DEPARTED_EXIT;
        DEPARTED_POS = _other.DEPARTED_POS;
    }
}
