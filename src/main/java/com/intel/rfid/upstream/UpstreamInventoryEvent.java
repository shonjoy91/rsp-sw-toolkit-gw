/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.upstream;

import com.intel.rfid.api.upstream.GatewayInventoryEventNotification;
import com.intel.rfid.api.downstream.SensorInventoryEventNotification;
import com.intel.rfid.api.data.Tag;
import com.intel.rfid.api.data.TagEvent;

public class UpstreamInventoryEvent extends GatewayInventoryEventNotification {

    public void add(String _epc, String _tid, String _location, String _facilityId, TagEvent _event, long _lastSeen) {
        Item item = new Item();
        item.epc_code = _epc;
        item.tid = _tid;
        item.location = _location;
        item.facility_id = _facilityId;
        item.event_type = _event.toString();
        item.timestamp = _lastSeen;
        params.data.add(item);
    }

    public void add(Tag _tag, TagEvent _event) {
        Item item = new Item();
        item.facility_id = _tag.getFacility();
        item.epc_code = _tag.getEPC();
        item.tid = _tag.getTID();
        item.event_type = _event.toString();
        item.timestamp = _tag.getLastRead();
        item.location = _tag.getLocation();
        params.data.add(item);
    }

    public void add(SensorInventoryEventNotification.Item _item, String _facilityId, String _deviceId) {
        Item item = new Item();
        item.facility_id = _facilityId;
        item.epc_code = _item.epc;
        item.tid = _item.tid;
        item.event_type = _item.event_type;
        item.timestamp = _item.event_date;
        item.location = _deviceId;
        params.data.add(item);
    }

}
