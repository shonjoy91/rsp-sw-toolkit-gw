package com.intel.rfid.api.data;

public enum GeoRegion {
    AUSTRALIA(923250),
    BRAZIL(915250),
    CHINA(921875),
    ETSI(866300),
    ETSI_UPPER(917500),
    HONG_KONG(922250),
    INDIA(866250),
    INDONESIA(924250),
    JAPAN(918000),
    KOREA(919100),
    MALAYSIA(921250),
    NEW_ZEALAND(925250),
    RUSSIA(866600),
    SINGAPORE(922250),
    TAIWAN(925250),
    THAILAND(922250),
    USA(915250),
    VIETNAM(922250);

    public final int centerFrequency;

    GeoRegion(int _centerFrequency) { centerFrequency = _centerFrequency; }

}

