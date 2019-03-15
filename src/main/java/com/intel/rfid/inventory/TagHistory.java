/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.inventory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TagHistory {

    public static class Waypoint implements Comparable<Waypoint> {
        public String deviceId = "";
        public long timestamp;

        public Waypoint(String _deviceId, long _timestamp) {
            deviceId = _deviceId;
            timestamp = _timestamp;
        }

        @Override
        public int compareTo(Waypoint o) {
            return Long.compare(timestamp, o.timestamp);
        }
    }

    private LinkedList<Waypoint> waypoints;
    private final int maxSize;

    public TagHistory(int _size) {
        maxSize = _size;
        waypoints = new LinkedList<>();
    }

    public synchronized void add(String _deviceId, long _timestamp) {
        waypoints.addLast(new Waypoint(_deviceId, _timestamp));
        while (waypoints.size() > maxSize) {
            waypoints.removeFirst();
        }
    }

    public synchronized List<Waypoint> getWaypoints() {
        return new ArrayList<>(waypoints);
    }

}
