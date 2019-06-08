/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

import com.intel.rfid.tag.TagState;

public class TagInfo {

    public String epc;
    public String tid;
    public TagState state;
    public String location;
    public long last_read_on = 0;
    public String facility_id;

    public TagInfo(String _epc,
                   String _tid,
                   TagState _state,
                   String _location,
                   long _lastReadOn,
                   String _facilityId) {
        epc = _epc;
        tid = _tid;
        state = _state;
        location = _location;
        last_read_on = _lastReadOn;
        facility_id = _facilityId;
    }
}
