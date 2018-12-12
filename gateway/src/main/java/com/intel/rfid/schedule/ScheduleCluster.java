/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.schedule;

import com.intel.rfid.api.Behavior;
import com.intel.rfid.api.Personality;
import com.intel.rfid.behavior.BehaviorConfig;
import com.intel.rfid.exception.ConfigException;
import com.intel.rfid.sensor.SensorGroup;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.sensor.SensorPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class ScheduleCluster {

    protected Logger log = LoggerFactory.getLogger(getClass());

    String facilityId;
    Personality personality;
    Behavior behavior;
    List<SensorGroup> sensorGroups = new ArrayList<>();

    public ScheduleCluster(String _facilityId, Personality _personality, Behavior _behavior,
                           List<SensorGroup> _sensorGroups) {
        facilityId = _facilityId;
        personality = _personality;
        behavior = _behavior;
        sensorGroups.addAll(_sensorGroups);
    }

    public ScheduleCluster(ScheduleConfiguration.Cluster _schedCfgCluster, SensorManager _sensorMgr)
        throws ConfigException {

        facilityId = _schedCfgCluster.facility_id;

        if (_schedCfgCluster.personality != null && !_schedCfgCluster.personality.isEmpty()) {
            try {
                personality = Personality.valueOf(_schedCfgCluster.personality);
            } catch (IllegalArgumentException _e) {
                throw new ConfigException("unkown personality: " + _schedCfgCluster.personality);
            }
        }

        if (_schedCfgCluster.sensor_groups.isEmpty()) {
            throw new ConfigException("no sensors specified ");
        }

        for (List<String> sensorList : _schedCfgCluster.sensor_groups) {
            sensorGroups.add(new SensorGroup(sensorList, _sensorMgr));
        }

        try {
            behavior = BehaviorConfig.getBehavior(_schedCfgCluster.behavior_id);
        } catch (IOException e) {
            throw new ConfigException("Error for behavior id " + _schedCfgCluster.behavior_id
                                      + ": " + e.getMessage());
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

    public void alignSensorConfiguration() {

        for (SensorPlatform sensor : getAllSensors()) {
            if (facilityId != null && !facilityId.isEmpty()) {
                sensor.setFacilityId(facilityId);
            }

            if (personality != null) {
                sensor.setPersonality(personality);
            } else {
                sensor.clearPersonality();
            }

        }
    }
}
