/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonNotification;

public class MotionEventNotification extends JsonNotification {

    public static final String METHOD_NAME = "motion_event";

    public MotionEventNotification() {
        method = METHOD_NAME;
    }

    public Params params = new Params();

    public static class Params {
        public long sent_on;
        public String device_id;
        public String facility_id;
        public String image_resolution;
        public String image_url;
        public Object location;

        @Override
        public String toString() {
            return "{ sent_on=" + sent_on +
                    ", device_id='" + device_id + '\'' +
                    ", facility_id='" + facility_id + '\'' +
                    ", image_resolution='" + image_resolution + '\'' +
                    ", image_url='" + image_url + '\'' +
                    ", location=" + location +
                    " }";
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + params.toString();
    }

}
