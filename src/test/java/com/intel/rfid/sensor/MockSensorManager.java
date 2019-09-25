package com.intel.rfid.sensor;

import com.intel.rfid.cluster.ClusterManager;

public class MockSensorManager extends SensorManager {

    public MockSensorManager(ClusterManager _clusterMgr) {
        super(_clusterMgr);
    }

    public MockSensorPlatform establishRSP(String _deviceId) {
        SensorPlatform sensor;
        synchronized (deviceIdToRSP) {
            sensor = deviceIdToRSP.computeIfAbsent(_deviceId.toUpperCase(),
                                                   k -> new MockSensorPlatform(_deviceId.toUpperCase(), this));
        }
        return (MockSensorPlatform) sensor;
    }

    public void sendConnectResponse(String _responseId, String _deviceId, String _facilityId) {
        log.info("stubbed sendConnectResponse");
    }
}
