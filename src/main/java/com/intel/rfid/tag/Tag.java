/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.tag;

import com.intel.rfid.api.data.TagStatsInfo;
import com.intel.rfid.api.sensor.TagRead;
import com.intel.rfid.helpers.DateTimeHelper;
import com.intel.rfid.inventory.RssiAdjuster;
import com.intel.rfid.sensor.SensorPlatform;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static com.intel.rfid.tag.TagState.DEPARTED_EXIT;
import static com.intel.rfid.tag.TagState.DEPARTED_POS;
import static com.intel.rfid.tag.TagState.EXITING;
import static com.intel.rfid.tag.TagState.PRESENT;

public class Tag implements Comparable<Tag> {

    private String epc;
    private String tid;
    private String location = "UNKNOWN";
    private String deviceLocation = "UNKNOWN";
    private String facility = "UNKNOWN";
    private long lastRead = 0;
    private long lastDeparted = 0;
    private long lastArrived = 0;
    private TagState state = TagState.UNKNOWN;
    private TagDirection direction = TagDirection.Stationary;
    private TagHistory history = new TagHistory(10);


    private Map<String, TagStats> deviceStatsMap = new TreeMap<>();

    public Tag(String _epc) {
        epc = _epc;
    }

    public String getEPC() { return epc; }

    public String getTID() { return tid; }

    public String getLocation() { return location; }

    public String getDeviceLocation() { return deviceLocation; }

    public String getFacility() { return facility; }

    public long getLastRead() { return lastRead; }

    public long getLastArrived() { return lastArrived; }

    public long getLastDeparted() { return lastDeparted; }

    public TagState getState() { return state; }

    public TagDirection getDirection() { return direction; }

    public void setState(TagState _state) {
        setState(_state, lastRead);
    }

    // capture transition times
    public void setState(TagState _state, long _time) {

        switch (_state) {
            case PRESENT:
                lastArrived = _time;
                state = PRESENT;
                break;
            case DEPARTED_EXIT:
                state = DEPARTED_EXIT;
                lastDeparted = _time;
                break;
            case DEPARTED_POS:
                state = DEPARTED_POS;
                lastDeparted = _time;
                break;
            default:
                state = _state;
        }
    }

    public synchronized void update(SensorPlatform _rsp,
                                    TagRead _tagRead,
                                    RssiAdjuster _weighter) {

        if (_tagRead == null || _rsp == null) {
            throw new IllegalArgumentException("null arguments to TagEvent.update are not allowed");
        }
        String srcAlias = _rsp.getAlias(_tagRead.antenna_id);

        lastRead = _tagRead.last_read_on;

        TagStats curStats = deviceStatsMap.get(srcAlias);
        if (curStats == null) {
            curStats = new TagStats();
            deviceStatsMap.put(srcAlias, curStats);
        }
        curStats.update(_tagRead);

        if (_tagRead.tid != null) {
            tid = _tagRead.tid;
        }

        if (!location.equals(srcAlias)) {

            TagStats locationStats = deviceStatsMap.get(location);
            if (locationStats == null) {
                // this means the tag has never been read (somehow)
                location = srcAlias;
                deviceLocation = _rsp.getDeviceId();
                facility = _rsp.getFacilityId();
                history.add(location, lastRead);
            } else if (curStats.getN() > 2) {
                double w = 0.0;
                if (_weighter != null) {
                    w = _weighter.getWeight(locationStats.getLastRead(), _rsp);
                }
                if (curStats.getRssiMeanDBM() > locationStats.getRssiMeanDBM() + w) {
                    location = srcAlias;
                    deviceLocation = _rsp.getDeviceId();
                    facility = _rsp.getFacilityId();
                    history.add(location, lastRead);
                }
            }
        }
    }

    public static class Cached {
        public String epc;
        public String tid;
        public TagState state;
        public String location;
        public String deviceLocation;
        public String facility;
        public long lastRead;
    }

    public Cached toCached() {
        Cached ct = new Cached();
        ct.epc = epc;
        ct.tid = tid;
        ct.state = state;
        ct.location = location;
        ct.deviceLocation = deviceLocation;
        ct.facility = facility;
        ct.lastRead = lastRead;
        return ct;
    }

    public static Tag fromCached(Cached _ct) {
        Tag t = new Tag(_ct.epc);
        t.tid = _ct.tid;
        t.state = _ct.state;
        if (t.state == EXITING) {
            t.state = PRESENT;
        }
        t.location = _ct.location;
        t.deviceLocation = _ct.deviceLocation;
        t.facility = _ct.facility;
        t.lastRead = _ct.lastRead;
        t.history.add(t.location, t.lastRead);
        return t;
    }

    @Override
    public String toString() {
        long now = System.currentTimeMillis();
        return epc + ", " +
               tid + ", " +
               state.abbrev() + ", " +
               location + ", " +
               DateTimeHelper.timeAsHMS_MS(now - lastRead) + ", " +
               facility;
    }

    public void getStatsUpdate(TagStatsInfo _statsUpdate) {
        for (String srcAlias : deviceStatsMap.keySet()) {
            _statsUpdate.addStat(epc, srcAlias, location.equals(srcAlias), deviceStatsMap.get(srcAlias).inDBM());
        }
    }

    public static final String STATS_SUMMARY_CSV_HDR =
        "epc, tid, state, elapsed, " +
        "location-ind, sensor, " +
        "count, " +
        "mean-dBm, stddev-dBm, min-dBm, max-dBm, " +
        "mean-readInt, stddev-readInt";

    public void statsSummary(PrintWriter _pw, long _timeRef) {
        for (String srcAlias : deviceStatsMap.keySet()) {
            TagStats stats = deviceStatsMap.get(srcAlias);
            TagStats.Results r = stats.inDBM();


            _pw.println(String.format(
                "%s, %s, %s, %s, %s, %s, %2d, %6.1f, %9.1f, %6.1f, %6.1f, %8.0f, %8.0f",
                epc,
                tid,
                state.abbrev(),
                DateTimeHelper.timeAsHMS_MS(_timeRef - r.lastRead),
                (location.equals(srcAlias) ? "@" : " "),
                srcAlias,
                r.n, r.mean, r.stdDev, r.min, r.max,
                stats.getReadIntervalMean(),
                stats.getReadIntervalStdDev()));
        }
    }

    public static final String STATS_DETAIL_CSV_HDR =
        "epc,tid,state,cur-time,last-read,elapsed," +
        "location-ind,sensor," +
        "count," +
        "mean-dBm,stddev-dBm,min-dBm,max-dBm," +
        "mean-mW,stddev-mW,min-mW,max-mW" +
        "mean-readInt,stddev-readInt";

    public void statsDetail(PrintWriter _pw, long _timeRef) {
        for (String srcAlias : deviceStatsMap.keySet()) {
            TagStats stats = deviceStatsMap.get(srcAlias);
            TagStats.Results db = stats.inDBM();
            TagStats.Results mw = stats.inMilliWatts();

            _pw.println(String.format(
                "%s,%s,%s,%s,%s,%s,%s,%s,%2d,%.1f,%.1f,%.1f,%.1f,%.14f,%.14f,%.14f,%.14f,%.1f,%.1f",
                epc, tid, state.abbrev(), _timeRef, db.lastRead,
                DateTimeHelper.timeAsHMS_MS(_timeRef - db.lastRead),
                (location.equals(srcAlias) ? "@" : " "), srcAlias,
                db.n,
                db.mean, db.stdDev, db.min, db.max,
                mw.mean, mw.stdDev, mw.min, mw.max,
                stats.getReadIntervalMean(), stats.getReadIntervalStdDev()));
        }
    }

    public void waypoints(PrintWriter _pw) {
        _pw.print(epc + ", ");
        _pw.print(tid + ", ");
        _pw.print(state.abbrev() + ", ");

        List<TagHistory.Waypoint> waypoints = history.getWaypoints();
        boolean needComma = false;
        for (TagHistory.Waypoint wp : waypoints) {

            if (needComma) { _pw.print(", "); } else { needComma = true; }

            _pw.print(wp.deviceId + ", ");
            _pw.print(wp.timestamp);
        }
        _pw.println();
    }

    @Override
    public boolean equals(Object _o) {
        if (this == _o) { return true; }
        if (_o == null || getClass() != _o.getClass()) { return false; }
        Tag tag = (Tag) _o;
        return Objects.equals(epc, tag.epc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(epc);
    }

    @Override
    public int compareTo(Tag i2) {
        return epc.compareTo(i2.epc);
    }

}
