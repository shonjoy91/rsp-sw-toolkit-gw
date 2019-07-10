/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonResponseOK;
import com.intel.rfid.api.data.ClusterTemplate;

public class ClusterGetTemplateResponse extends JsonResponseOK {

    public ClusterGetTemplateResponse(String _id, ClusterTemplate _template) {
        super(_id, Boolean.TRUE);
        result = _template;
    }

}
