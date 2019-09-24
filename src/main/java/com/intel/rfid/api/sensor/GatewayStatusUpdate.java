/*
 * INTEL CONFIDENTIAL
 * Copyright (2015, 2016, 2017) Intel Corporation.
 *
 * The source code contained or described herein and all documents related to the source code ("Material")
 * are owned by Intel Corporation or its suppliers or licensors. Title to the Material remains with
 * Intel Corporation or its suppliers and licensors. The Material may contain trade secrets and proprietary
 * and confidential information of Intel Corporation and its suppliers and licensors, and is protected by
 * worldwide copyright and trade secret laws and treaty provisions. No part of the Material may be used,
 * copied, reproduced, modified, published, uploaded, posted, transmitted, distributed, or disclosed in
 * any way without Intel/'s prior express written permission.
 * No license under any patent, copyright, trade secret or other intellectual property right is granted
 * to or conferred upon you by disclosure or delivery of the Materials, either expressly, by implication,
 * inducement, estoppel or otherwise. Any license under such intellectual property rights must be express
 * and approved by Intel in writing.
 * Unless otherwise agreed by Intel in writing, you may not remove or alter this notice or any other
 * notice embedded in Materials by Intel or Intel's suppliers or licensors in any way.
 */
package com.intel.rfid.api.sensor;


import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.api.upstream.RspControllerStatusUpdateNotification;

@Deprecated
public class GatewayStatusUpdate extends JsonNotification {

    public static final String METHOD_NAME = "gw_status_update";

    public static final String READY = "ready";
    public static final String SHUTTING_DOWN = "shutting_down";
    public static final String LOST = "lost";

    public Params params = new Params();

    public GatewayStatusUpdate() {
        method = METHOD_NAME;
    }

    public GatewayStatusUpdate(String _deviceId, String _status) {
        method = METHOD_NAME;
        params.device_id = _deviceId;
        params.status = _status;
    }

    public static class Params {
        public long sent_on = System.currentTimeMillis();
        public String device_id;
        public String status;
    }

    public GatewayStatusUpdate(RspControllerStatusUpdateNotification _update) {
        params.device_id = _update.params.device_id;
        params.status = _update.params.status;
        params.sent_on = _update.params.sent_on;
    }
}
