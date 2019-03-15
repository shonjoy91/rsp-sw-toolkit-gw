/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

public class SetMotionEvent extends JsonRequest {

    public static final String METHOD_NAME = "set_motion_event";

    public Params params = new Params();

    public SetMotionEvent() { method = METHOD_NAME; }

    public SetMotionEvent(boolean _sendEvents, boolean _captureImages) {
        this();
        params.send_events = _sendEvents;
        params.capture_images = _captureImages;
    }

    public static class Params {
        public boolean send_events;
        public boolean capture_images;
    }

}
