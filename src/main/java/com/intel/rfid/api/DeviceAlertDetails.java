package com.intel.rfid.api;

import java.util.Map;

public class DeviceAlertDetails {
    public long sent_on;
    public String device_id;
    public String facility_id;
    public int alert_number;
    public String alert_description;
    public String severity;
    public Map<String, Object> optional;
}
