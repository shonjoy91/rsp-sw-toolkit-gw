package com.intel.rfid.cluster;

import com.intel.rfid.api.Personality;
import com.intel.rfid.api.ProvisionToken;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
    public String id;
    public Personality personality;
    public String facility_id;
    public List<String> aliases = new ArrayList<>();
    public String behavior_id;
    public List<List<String>> sensor_groups = new ArrayList<>();
    public List<ProvisionToken> tokens = new ArrayList<>();
}
