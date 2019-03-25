/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.sensor;

import com.intel.rfid.api.EpcRead;
import com.intel.rfid.api.InventoryData;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

import static com.intel.rfid.helpers.RfUtils.milliwattsToRssi;
import static com.intel.rfid.helpers.RfUtils.rssiToMilliwatts;

public class SensorStats {

    protected Logger log = LoggerFactory.getLogger(getClass());

    public static class Data {
        public final double avgTagCount;
        public final double avgRssiMw;
        public final double avgRssiDbm;
        public final double avgUtilization;


        public Data(double _avgTagCount, double _avgRssiMw, double _avgUtilization) {
            avgTagCount = _avgTagCount;
            avgRssiMw = _avgRssiMw;
            avgRssiDbm = milliwattsToRssi(avgRssiMw);
            avgUtilization = _avgUtilization;

        }

        @Override
        public String toString() {
            return "Data{" +
                   "avgTagCount=" + avgTagCount +
                   ", avgRssiMw=" + avgRssiMw +
                   ", avgRssiDbm=" + avgRssiDbm +
                   ", avgUtilization=" + avgUtilization +
                   '}';
        }

    }

    public Data prev10Minutes() {
        return compile(0, 10 / SAMPLE_RATE_MINUTES);
    }

    public Data prevHour() {
        return compile(0, 60 / SAMPLE_RATE_MINUTES);
    }

    public Data prevDay() {
        return compile(0, 24 * 60 / SAMPLE_RATE_MINUTES);
    }

    public Data prevWeek() {
        return compile(0, 7 * 24 * 60 / SAMPLE_RATE_MINUTES);
    }

    private Data compile(int _from, int _n) {
        // stay with data collected boundaries
        int to = _from + _n;
        if (_from < 0 || to > dequeTagCount.size()) {
            return null;
        }
        SummaryStatistics count = new SummaryStatistics();
        SummaryStatistics rssi = new SummaryStatistics();
        SummaryStatistics util = new SummaryStatistics();
        for (int i = _from; i < to; i++) {
            count.addValue(dequeTagCount.get(i));
            rssi.addValue(dequeRssiMw.get(i));
            util.addValue(dequeUtilization.get(i));
        }

        return new Data(count.getMean(), rssi.getMean(), util.getMean());
    }

    // sample every 10 minutes and keep those samples for one week.
    static final int SAMPLE_RATE_MINUTES = 10;
    private final static int TOTAL_N = 6 * 24 * 7;

    // for each time segment will contain
    // number of reads total
    // avg rssi
    // utilization %
    private LinkedList<Double> dequeTagCount = new LinkedList<>();
    private LinkedList<Double> dequeRssiMw = new LinkedList<>();
    private LinkedList<Double> dequeUtilization = new LinkedList<>();

    private SummaryStatistics curReadStats = new SummaryStatistics();

    synchronized void onInventoryData(InventoryData _invData) {
        for (EpcRead.Data readData : _invData.params.data) {
            double d = rssiToMilliwatts(readData.rssi);
            curReadStats.addValue(d);
        }
    }

    // utilization
    private SensorUtilizationStats curUtilStats = new SensorUtilizationStats();

    synchronized void startedReading() {
        curUtilStats.markOn();
    }

    synchronized void stoppedReading() {
        curUtilStats.markOff();
    }

    public synchronized void connected() {
        curUtilStats.markAvailable();
    }

    public synchronized void disconnected() {
        curUtilStats.markUnavailable();
    }

    // copy over the stats
    synchronized void sample() {

        // adjust deques
        while (dequeTagCount.size() >= TOTAL_N) {
            dequeTagCount.removeLast();
            dequeRssiMw.removeLast();
            dequeUtilization.removeLast();
        }

        dequeTagCount.addFirst((double) curReadStats.getN());
        dequeRssiMw.addFirst(curReadStats.getMean());
        dequeUtilization.addFirst(curUtilStats.getUtilizationPercent());

        curReadStats = new SummaryStatistics();
        curUtilStats.snapForward();
    }

    public static String csv(Data _d) {
        if (_d == null) { return "N/A,N/A,N/A,"; }
        return String.format("%.1f,%.1f,%.1f,",
                             _d.avgTagCount, _d.avgRssiDbm, (_d.avgUtilization * 100));
    }

    // TODO: accumulate stats for a facility.
    // TODO: distributions of each stat category and raise alerts for any sensor more than x stddev out
}
