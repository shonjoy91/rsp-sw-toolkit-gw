/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.schedule;

import com.intel.rfid.api.Behavior;
import com.intel.rfid.cluster.Cluster;
import com.intel.rfid.sensor.SensorGroup;
import com.intel.rfid.sensor.SensorPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class ScheduleCluster {

    protected Logger log = LoggerFactory.getLogger(getClass());

    String id;
    Behavior behavior;
    List<SensorGroup> sensorGroups = new ArrayList<>();

    public ScheduleCluster(String _clusterId,
                           Behavior _behavior,
                           List<SensorGroup> _sensorGroups) {
        id = _clusterId;
        behavior = _behavior;
        if(_sensorGroups != null) {
            sensorGroups.addAll(_sensorGroups);
        }
    }

    public boolean contains(SensorPlatform _sensor) {
        return getAllSensors().contains(_sensor);
    }

    public Collection<SensorPlatform> getAllSensors() {
        Collection<SensorPlatform> all = new HashSet<>();
        for (SensorGroup sg : sensorGroups) {
            all.addAll(sg.sensors);
        }
        return all;
    }
    
    public Cluster asCluster() {
        Cluster c = new Cluster();
        c.id = id;
        c.behavior_id = behavior.id;
        for (SensorGroup sg : sensorGroups) {
            List<String> sensorIds = new ArrayList<>();
            for (SensorPlatform sensor : sg.sensors) {
                sensorIds.add(sensor.getDeviceId());
            }
            c.sensor_groups.add(sensorIds);
        }
        return c;
    }

}
