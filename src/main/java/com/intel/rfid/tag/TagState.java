/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.tag;

public enum TagState {
    UNKNOWN,
    PRESENT,
    EXITING,
    DEPARTED_EXIT,
    DEPARTED_POS;


    public String abbrev() {
        switch (this) {
            case PRESENT:
                return "P";
            case EXITING:
                return "E";
            case DEPARTED_EXIT:
                return "DX";
            case DEPARTED_POS:
                return "DP";
            case UNKNOWN:
            default:
                return "U";
        }
    }


}
