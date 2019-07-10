/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.upstream;

import com.intel.rfid.api.data.InventoryEventInfo;
import com.intel.rfid.api.data.InventoryEventItem;
import com.intel.rfid.tag.Tag;
import com.intel.rfid.tag.TagEvent;

public class UpstreamInventoryEventInfo extends InventoryEventInfo {

    public void add(String _epc, String _tid, String _location, String _facilityId, TagEvent _event, long _lastSeen) {
        InventoryEventItem item = new InventoryEventItem();
        item.epc_code = _epc;
        item.tid = _tid;
        item.location = _location;
        item.facility_id = _facilityId;
        item.event_type = _event.toString();
        item.timestamp = _lastSeen;
        data.add(item);
    }

    public void add(Tag _tag, TagEvent _event) {
        InventoryEventItem item = new InventoryEventItem();
        item.facility_id = _tag.getFacility();
        item.epc_code = _tag.getEPC();
        item.tid = _tag.getTID();
        item.event_type = _event.toString();
        item.timestamp = _tag.getLastRead();
        item.location = _tag.getLocation();
        data.add(item);
    }

}
