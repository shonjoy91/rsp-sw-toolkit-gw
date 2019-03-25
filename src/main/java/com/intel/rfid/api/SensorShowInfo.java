package com.intel.rfid.api;

import com.intel.rfid.sensor.ConnectionState;
import com.intel.rfid.sensor.ReadState;

import java.util.ArrayList;
import java.util.List;

public class SensorShowInfo {

    public List<Info> info_list = new ArrayList<>();
    
    public class Info {
        public String id;
        public ConnectionState connection_state;
        public ReadState read_state;
        public String behavior_id;
        public String facility;
        public Personality personality;
        public List<DeviceAlertDetails> alerts = new ArrayList<>();

        Info(String _id, ConnectionState _connectionState, ReadState _readState, 
             String _behaviorId, String _facility, Personality _personality, List<DeviceAlertDetails> _alerts) {

            id = _id;
            connection_state = _connectionState;
            read_state = _readState;
            behavior_id = _behaviorId;
            facility = _facility;
            personality = _personality;
            alerts.addAll(_alerts);
        }
    }

    public void add(String _id, ConnectionState _connectionState, ReadState _readState,
                    String _behaviorId, String _facility, Personality _personality, List<DeviceAlertDetails> _alerts) {

        info_list.add(new Info(_id, _connectionState, _readState, _behaviorId, _facility, _personality, _alerts));
    }

    public void copyFrom(SensorShowInfo _other) {
        info_list.addAll(_other.info_list);
    }
}
