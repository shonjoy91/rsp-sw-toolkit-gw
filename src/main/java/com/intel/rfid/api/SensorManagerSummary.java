package com.intel.rfid.api;

public class SensorManagerSummary {
    public int reading;
    public int connected;
    public int disconnected;

    public void copyFrom(SensorManagerSummary _other) {
        reading = _other.reading;
        connected = _other.connected;
        disconnected = _other.disconnected;
    }
}
