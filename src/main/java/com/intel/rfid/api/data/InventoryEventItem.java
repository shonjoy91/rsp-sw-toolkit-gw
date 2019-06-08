/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

public class InventoryEventItem {
    public String facility_id;
    public String epc_code;
    public String tid;
    public String epc_encode_format = "tbd";
    public String event_type;
    public long timestamp;
    public String location;
}
