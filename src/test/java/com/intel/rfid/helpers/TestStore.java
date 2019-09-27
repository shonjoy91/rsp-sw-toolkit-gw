package com.intel.rfid.helpers;

import com.intel.rfid.api.data.Cluster;
import com.intel.rfid.api.data.ClusterConfig;
import com.intel.rfid.api.data.Personality;
import com.intel.rfid.api.sensor.TagRead;
import com.intel.rfid.controller.MockRspController;
import com.intel.rfid.sensor.MockSensorManager;
import com.intel.rfid.sensor.MockSensorPlatform;

import java.util.ArrayList;
import java.util.List;

public class TestStore {

    public enum Facility {BackStock, SalesFloor, COLD, DRY, A, B, C}

    private int minRSSI = -95 * 10;
    private int maxRSSI = -55 * 10;

    public int rssiMax() { return maxRSSI; }

    public int rssiStrong() { return (maxRSSI - (maxRSSI - minRSSI) / 3); }

    public int rssiWeak() { return (minRSSI + (maxRSSI - minRSSI) / 3); }

    public int rssiMin() { return minRSSI; }

    public MockRspController rspController;
    public MockSensorPlatform sensorFront01;
    public MockSensorPlatform sensorFront02;
    public MockSensorPlatform sensorFront03;
    public MockSensorPlatform sensorFrontPOS;
    public MockSensorPlatform sensorFrontExit;

    public MockSensorPlatform sensorBack01;
    public MockSensorPlatform sensorBack02;
    public MockSensorPlatform sensorBack03;

    public MockSensorPlatform sensorCold01;
    public MockSensorPlatform sensorDry01;

    public MockSensorPlatform sensorA01;
    public MockSensorPlatform sensorB01;
    public MockSensorPlatform sensorCexit01;
    public MockSensorPlatform sensorCexit02;

    public long now = System.currentTimeMillis();
    public long time_m10;
    public long time_m09;
    public long time_m08;
    public long time_m07;
    public long time_m06;
    public long time_m05;
    public long time_m04;
    public long time_m03;
    public long time_m02;
    public long time_m01;
    public long time_m00;

    public TestStore() {
        rspController = new MockRspController();

        sensorFront01 = establish("RSP-150000", Facility.SalesFloor);
        sensorFront02 = establish("RSP-150001", Facility.SalesFloor);
        sensorFront03 = establish("RSP-150002", Facility.SalesFloor);

        sensorFrontPOS = establish("RSP-150003", Facility.SalesFloor, Personality.POS);
        sensorFrontExit = establish("RSP-150004", Facility.SalesFloor, Personality.EXIT);

        sensorBack01 = establish("RSP-150005", Facility.BackStock);
        sensorBack02 = establish("RSP-150006", Facility.BackStock);
        sensorBack03 = establish("RSP-150007", Facility.BackStock);

        sensorCold01 = establish("RSP-150008", Facility.COLD);
        sensorDry01 = establish("RSP-150009", Facility.DRY);

        sensorA01 = establish("RSP-150010", Facility.A);
        sensorB01 = establish("RSP-150011", Facility.B);
        sensorCexit01 = establish("RSP-150012", Facility.C, Personality.EXIT);
        sensorCexit02 = establish("RSP-150013", Facility.C, Personality.EXIT);

        resetTimestamps();

    }

    public void resetTimestamps() {
        now = System.currentTimeMillis();
        time_m10 = now - (0 * 60 * 1000);
        time_m09 = now - (1 * 60 * 1000);
        time_m08 = now - (2 * 60 * 1000);
        time_m07 = now - (3 * 60 * 1000);
        time_m06 = now - (4 * 60 * 1000);
        time_m05 = now - (5 * 60 * 1000);
        time_m04 = now - (6 * 60 * 1000);
        time_m03 = now - (7 * 60 * 1000);
        time_m02 = now - (8 * 60 * 1000);
        time_m01 = now - (9 * 60 * 1000);
        time_m00 = now - (10 * 60 * 1000);
    }

    public MockSensorPlatform establish(String _sensorId, Facility _facility) {
        return establish(_sensorId, _facility, null);
    }

    public MockSensorPlatform establish(String _sensorId, Facility _facility, Personality _personality) {
        MockSensorManager msm = rspController.getMockSensorManager();
        MockSensorPlatform msp = msm.establish(_sensorId);

        msp.setFacilityId(_facility.toString());
        msp.setPersonality(_personality);
        return msp;
    }

    int tagSerialNum = 1;

    public TagRead generateReadData(long _lastReadOn) {
        TagRead tagRead = new TagRead();
        tagRead.epc = String.format("EPC%06d", tagSerialNum);
        tagRead.tid = String.format("TID%06d", tagSerialNum);
        tagSerialNum++;
        tagRead.frequency = 927;
        tagRead.rssi = rssiMin();
        tagRead.last_read_on = _lastReadOn;
        return tagRead;
    }

    public ClusterConfig getRetailUseCaseClusterConfig() {

        ClusterConfig cfg = new ClusterConfig();
        cfg.id = "RetailUseCaseClusterConfigExample";
        Cluster cluster;
        List<String> sensorList;

        cluster = new Cluster();
        cluster.id = Facility.BackStock.toString() + "Cluster";
        cluster.facility_id = Facility.BackStock.toString();
        cluster.behavior_id = "ClusterDeepScan_PORTS_1";
        sensorList = new ArrayList<>();
        sensorList.add(sensorBack01.getDeviceId());
        cluster.sensor_groups.add(sensorList);
        cfg.clusters.add(cluster);

        cluster = new Cluster();
        cluster.id = Facility.SalesFloor.toString() + "Cluster";
        cluster.facility_id = Facility.SalesFloor.toString();
        cluster.behavior_id = "ClusterMobility_PORTS_1";
        sensorList = new ArrayList<>();
        sensorList.add(sensorFront01.getDeviceId());
        cluster.sensor_groups.add(sensorList);
        cfg.clusters.add(cluster);

        cluster = new Cluster();
        cluster.id = Facility.SalesFloor.toString() + "ExitCluster";
        cluster.facility_id = Facility.SalesFloor.toString();
        cluster.behavior_id = "ClusterExit_PORTS_1";
        cluster.personality = Personality.EXIT;
        sensorList = new ArrayList<>();
        sensorList.add(sensorFrontExit.getDeviceId());
        cluster.sensor_groups.add(sensorList);
        cfg.clusters.add(cluster);

        return cfg;
    }
}
