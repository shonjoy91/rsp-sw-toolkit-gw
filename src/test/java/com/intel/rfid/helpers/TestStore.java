package com.intel.rfid.helpers;

import com.intel.rfid.api.data.EpcRead;
import com.intel.rfid.api.data.Personality;
import com.intel.rfid.gateway.MockGateway;
import com.intel.rfid.sensor.MockSensorManager;
import com.intel.rfid.sensor.MockSensorPlatform;

public class TestStore {

    public enum Facility { BACK, FRONT, COLD, DRY, A, B, C}

    private int minRSSI = -95 * 10;
    private int maxRSSI = -55 * 10;

    public int rssiMax() { return maxRSSI; }

    public int rssiStrong() { return (maxRSSI - (maxRSSI - minRSSI) / 3); }

    public int rssiWeak() { return (minRSSI + (maxRSSI - minRSSI) / 3); }

    public int rssiMin() { return minRSSI; }
    
    public MockGateway gateway;
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
        gateway = new MockGateway();

        sensorFront01 = establish("sensorFront01", Facility.FRONT);
        sensorFront02 = establish("sensorFront02", Facility.FRONT);
        sensorFront03 = establish("sensorFront03", Facility.FRONT);

        sensorFrontPOS = establish("sensorFrontPOS", Facility.FRONT, Personality.POS);
        sensorFrontExit = establish("sensorExit01", Facility.FRONT, Personality.EXIT);

        sensorBack01 = establish("sensorBack01", Facility.BACK);
        sensorBack02 = establish("sensorBack02", Facility.BACK);
        sensorBack03 = establish("sensorBack03", Facility.BACK);

        sensorCold01 = establish("sensorCold01", Facility.COLD);
        sensorDry01 = establish("sensorDry01", Facility.DRY);

        sensorA01 = establish("sensorA01", Facility.A);
        sensorB01 = establish("sensorB01", Facility.B);
        sensorCexit01 = establish("sensorCexit01", Facility.C, Personality.EXIT);
        sensorCexit02 = establish("sensorCexit02", Facility.C, Personality.EXIT);
        
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
        MockSensorManager msm = gateway.getMockSensorManager();
        MockSensorPlatform msp = msm.establishRSP(_sensorId);

        msp.setFacilityId(_facility.toString());
        msp.setPersonality(_personality);
        return msp;
    }

    int tagSerialNum = 1;
    public EpcRead.Data generateReadData(long _lastReadOn) {
        EpcRead.Data tagRead = new EpcRead.Data();
        tagRead.epc = String.format("EPC%06d", tagSerialNum);
        tagRead.tid = String.format("TID%06d", tagSerialNum);
        tagSerialNum++;
        tagRead.frequency = 927;
        tagRead.rssi = rssiMin();
        tagRead.last_read_on = _lastReadOn;
        return tagRead;
    }

}
