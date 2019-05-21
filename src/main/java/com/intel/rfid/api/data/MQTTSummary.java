package com.intel.rfid.api.data;

import java.util.ArrayList;
import java.util.List;

public class MQTTSummary {

    public RunState run_state = RunState.DISCONNECTED;
    public String broker_uri = "unknown";
    public List<String> subscribes = new ArrayList<>();
    public List<String> publishes = new ArrayList<>();
    
    public void copyFrom(MQTTSummary _other) {
        run_state = _other.run_state;
        broker_uri = _other.broker_uri;
        subscribes.addAll(_other.subscribes);
        publishes.addAll(_other.publishes);
    }
}
