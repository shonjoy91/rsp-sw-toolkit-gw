package com.intel.rfid.api.data;

import com.intel.rfid.api.data.ConnectionState;
import com.intel.rfid.api.data.Personality;
import com.intel.rfid.api.data.ReadState;

import java.util.ArrayList;
import java.util.List;

public class SensorBasicInfo {

    public String device_id;
    public ConnectionState connection_state;
    public ReadState read_state;
    public String behavior_id;
    public String facility_id;
    public Personality personality;
    public List<String> aliases = new ArrayList<>();
    public List<DeviceAlertDetails> alerts = new ArrayList<>();

    public SensorBasicInfo(String _device_id,
                           ConnectionState _connectionState,
                           ReadState _readState,
                           String _behaviorId,
                           String _facilityId,
                           Personality _personality,
                           List<String> _aliases,
                           List<DeviceAlertDetails> _alerts) {

        device_id = _device_id;
        connection_state = _connectionState;
        read_state = _readState;
        behavior_id = _behaviorId;
        facility_id = _facilityId;
        personality = _personality;
        aliases.addAll(_aliases);
        alerts.addAll(_alerts);
    }

}
