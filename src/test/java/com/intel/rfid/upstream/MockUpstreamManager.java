package com.intel.rfid.upstream;

import com.intel.rfid.cluster.ClusterManager;
import com.intel.rfid.downstream.DownstreamManager;
import com.intel.rfid.inventory.InventoryManager;
import com.intel.rfid.schedule.ScheduleManager;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.gpio.GPIOManager;

public class MockUpstreamManager extends UpstreamManager {

    public MockUpstreamManager(ClusterManager _clusterMgr,
                               SensorManager _sensorMgr,
                               GPIOManager _gpioMgr,
                               ScheduleManager _scheduleMgr,
                               InventoryManager _inventoryMgr,
                               DownstreamManager _downstreamMgr) {

        super(_clusterMgr, _sensorMgr, _gpioMgr,
              _scheduleMgr, _inventoryMgr, _downstreamMgr);
        // swap out for a mock instance
        mqttUpstream = new MockMqttUpstream(this);
    }
}
