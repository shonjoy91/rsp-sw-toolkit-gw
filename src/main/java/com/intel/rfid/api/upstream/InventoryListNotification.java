/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonNotification;
import com.intel.rfid.api.data.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InventoryListNotification extends JsonNotification {

    public static final String METHOD_NAME = "inventory_list";

    public List<Tag> params = new ArrayList<>();

    public InventoryListNotification() {
        method = METHOD_NAME;
    }

    public InventoryListNotification(Collection<Tag> _tags) {
        this();
        params.addAll(_tags);
    }

}
