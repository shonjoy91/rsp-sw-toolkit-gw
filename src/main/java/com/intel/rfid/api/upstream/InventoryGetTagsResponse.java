/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonResponseOK;
import com.intel.rfid.api.data.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InventoryGetTagsResponse extends JsonResponseOK {

    public InventoryGetTagsResponse(String _id, Collection<Tag> _tags) {
        super(_id, Boolean.TRUE);

        result = new Result(_tags);
    }

    public static class Result {
        public List<Tag> tags = new ArrayList<>();

        public Result(Collection<Tag> _tags) {
            tags.addAll(_tags);
        }
    }

}
