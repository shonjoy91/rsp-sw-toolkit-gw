package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonNotification;

public class EpcFilterPatternNotification extends JsonNotification {

    public static final String METHOD_NAME = "epc_filter_pattern";
    public Params params = new Params();

    public EpcFilterPatternNotification(String _epcTagFilter) {
        method = METHOD_NAME;
        params.epc_filter_pattern = _epcTagFilter;
    }
    
    public static class Params {
        public String epc_filter_pattern;
    }
}
