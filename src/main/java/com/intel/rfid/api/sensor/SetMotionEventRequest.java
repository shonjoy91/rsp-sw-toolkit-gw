/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonRequest;

public class SetMotionEventRequest extends JsonRequest {

    public static final String METHOD_NAME = "set_motion_event";

    public Params params = new Params();

    public SetMotionEventRequest() { method = METHOD_NAME; }

    public SetMotionEventRequest(boolean _sendEvents, boolean _captureImages) {
        this();
        params.send_events = _sendEvents;
        params.capture_images = _captureImages;
    }

    public static class Params {
        public boolean send_events;
        public boolean capture_images;
    }

}
