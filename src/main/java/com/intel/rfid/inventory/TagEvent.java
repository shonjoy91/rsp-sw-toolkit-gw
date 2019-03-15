/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.inventory;

/**
 * NOTE: these are lower_case_with_underscore to match JSON messaging API
 */
public enum TagEvent {
    none,
    arrival,
    moved,
    departed,
    returned,
    cycle_count
}
