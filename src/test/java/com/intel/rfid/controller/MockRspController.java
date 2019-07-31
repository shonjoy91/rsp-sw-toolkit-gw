package com.intel.rfid.controller;

import com.intel.rfid.cluster.MockClusterManager;
import com.intel.rfid.downstream.MockDownstreamManager;
import com.intel.rfid.gpio.MockGPIOManager;
import com.intel.rfid.inventory.MockInventoryManager;
import com.intel.rfid.schedule.MockScheduleManager;
import com.intel.rfid.sensor.MockSensorManager;
import com.intel.rfid.upstream.MockUpstreamManager;

public class MockRspController extends RspController {

    // prevent default super constructor
    // from instantiating objects before
    // mock objects are substituted
    public MockRspController() {
        // almost every test needs these mocked out
        // for now, they mostly just extend the
        // actual implementation
        clusterMgr = new MockClusterManager();
        inventoryMgr = new MockInventoryManager();
        sensorMgr = new MockSensorManager(clusterMgr);
        gpioMgr = new MockGPIOManager(sensorMgr);
        downstreamMgr = new MockDownstreamManager(sensorMgr, gpioMgr);
        scheduleMgr = new MockScheduleManager(clusterMgr);

        upstreamMgr = new MockUpstreamManager(clusterMgr, sensorMgr, gpioMgr,
                                              scheduleMgr, inventoryMgr, downstreamMgr);

        // this one is different from standard because of
        // the circular reference, can't set it up in the constructor and only use it to
        // support faking out the testing when establishing a new RSP.
        sensorMgr.setDownstreamMgr(downstreamMgr);
        init();
    }

    public MockClusterManager getMockClusterManager() {
        return (MockClusterManager) clusterMgr;
    }

    public MockSensorManager getMockSensorManager() {
        return (MockSensorManager) sensorMgr;
    }

    public MockGPIOManager getMockGPIOManager() {
        return (MockGPIOManager) gpioMgr;
    }

    public MockUpstreamManager getMockUpstreamManager() {
        return (MockUpstreamManager) upstreamMgr;
    }

    public MockDownstreamManager getMockDownstreamManager() {
        return (MockDownstreamManager) downstreamMgr;
    }

    public MockScheduleManager getMockScheduleManager() {
        return (MockScheduleManager) scheduleMgr;
    }

    public MockInventoryManager getMockInventoryManager() {
        return (MockInventoryManager) inventoryMgr;
    }
}
