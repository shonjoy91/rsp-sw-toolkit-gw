/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.inventory;

import java.util.concurrent.TimeUnit;

// be sure to preserve order
public enum TimeBucket {
    within_last_01_min(TimeUnit.MINUTES.toMillis(1)),
    from_01_to_05_min(TimeUnit.MINUTES.toMillis(5)),
    from_05_to_30_min(TimeUnit.MINUTES.toMillis(30)),
    from_30_to_60_min(TimeUnit.MINUTES.toMillis(60)),
    from_60_min_to_24_hr(TimeUnit.HOURS.toMillis(24)),
    more_than_24_hr(Long.MAX_VALUE);
    public final long millis;

    TimeBucket(long _millis) {
        millis = _millis;
    }
}
