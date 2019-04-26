package com.intel.rfid.api;

public class TagReadSummary {
    public long reads_per_second;
    public int within_last_01_min;
    public int from_01_to_05_min;
    public int from_05_to_30_min;
    public int from_30_to_60_min;
    public int from_60_min_to_24_hr;
    public int more_than_24_hr;

    public void copyFrom(TagReadSummary _other) {
        reads_per_second = _other.reads_per_second;
        within_last_01_min = _other.within_last_01_min;
        from_01_to_05_min = _other.from_01_to_05_min;
        from_05_to_30_min = _other.from_05_to_30_min;
        from_30_to_60_min = _other.from_30_to_60_min;
        from_60_min_to_24_hr = _other.from_60_min_to_24_hr;
        more_than_24_hr = _other.more_than_24_hr;
    }

}
