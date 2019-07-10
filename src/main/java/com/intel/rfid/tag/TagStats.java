/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.tag;

import com.intel.rfid.api.sensor.TagRead;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import static com.intel.rfid.helpers.RfUtils.milliwattsToRssi;
import static com.intel.rfid.helpers.RfUtils.rssiToMilliwatts;

public class TagStats {

    public class Results {
        public final long lastRead;
        public final long n;
        public final double mean;
        public final double stdDev;
        public final double min;
        public final double max;

        public Results(long _lastRead, long _n, double _mean, double _stdDev, double _min, double _max) {
            lastRead = _lastRead;
            n = _n;
            mean = _mean;
            stdDev = _stdDev;
            min = _min;
            max = _max;
        }
    }

    public static final int WINDOW_SIZE = 20;

    private long lastRead = -1L;
    private DescriptiveStatistics rssiMw = new DescriptiveStatistics(WINDOW_SIZE);
    private DescriptiveStatistics readInterval = new DescriptiveStatistics(WINDOW_SIZE);

    public synchronized void reset() {
        lastRead = -1L;
        rssiMw.clear();
        readInterval.clear();
    }

    public synchronized void update(TagRead _tagRead) {
        updateInternal(_tagRead.last_read_on, _tagRead.rssi);
    }

    public synchronized void update(long _lastRead, int _rssi) {
        updateInternal(_lastRead, _rssi);
    }

    private void updateInternal(long _lastRead, int _rssi) {
        if (lastRead != -1L) {
            readInterval.addValue(_lastRead - lastRead);
        }
        lastRead = _lastRead;
        double d = rssiToMilliwatts(_rssi / 10);
        rssiMw.addValue(d);
    }


    public synchronized long getLastRead() {
        return lastRead;
    }

    public synchronized long getN() {
        return rssiMw.getN();
    }

    // long _lastRead, long _n, double _mean, double _stdDev, double _min, double _max
    public synchronized Results inDBM() {
        return new Results(lastRead,
                           rssiMw.getN(),
                           milliwattsToRssi(rssiMw.getMean()),
                           milliwattsToRssi(rssiMw.getStandardDeviation()),
                           milliwattsToRssi(rssiMw.getMin()),
                           milliwattsToRssi(rssiMw.getMax()));
    }

    public synchronized Results inMilliWatts() {
        return new Results(lastRead,
                           rssiMw.getN(),
                           (rssiMw.getMean()),
                           (rssiMw.getStandardDeviation()),
                           (rssiMw.getMin()),
                           (rssiMw.getMax()));
    }

    public synchronized double getRssiMeanDBM() {
        return milliwattsToRssi(rssiMw.getMean());
    }

    public synchronized double getReadIntervalMean() {
        return readInterval.getMean();
    }

    public synchronized double getReadIntervalStdDev() {
        return readInterval.getStandardDeviation();
    }

    /**************************************


     Compute summary statistics for a list of double values

     Using the DescriptiveStatistics aggregate (values are stored in memory):

     // Get a DescriptiveStatistics instance
     DescriptiveStatistics stats = new DescriptiveStatistics();

     // Add the data from the array
     for( int i = 0; i < inputArray.length; i++) {
     stats.addValue(inputArray[i]);
     }

     // Compute some statistics
     double mean = stats.getMean();
     double std = stats.getStandardDeviation();
     double median = stats.getPercentile(50);


     Using the SummaryStatistics aggregate (values are not stored in memory):

     // Get a SummaryStatistics instance
     SummaryStatistics stats = new SummaryStatistics();

     // Read data from an input stream,
     // adding values and updating sums, counters, etc.
     while (line != null) {
     line = in.readLine();
     stats.addValue(Double.parseDouble(line.trim()));
     }
     in.close();

     // Compute the statistics
     double mean = stats.getMean();
     double std = stats.getStandardDeviation();
     //double median = stats.getMedian(); <-- NOT AVAILABLE


     Using the StatUtils utility class:

     // Compute statistics directly from the array
     // assume values is a double[] array
     double mean = StatUtils.mean(values);
     double std = FastMath.sqrt(StatUtils.variance(values));
     double median = StatUtils.percentile(values, 50);

     // Compute the mean of the first three values in the array
     mean = StatUtils.mean(values, 0, 3);




     // Maintain a "rolling mean" of the most recent 100 values from an input stream
     // for windowed stats, use the following
     DescriptiveStatistics stats = new DescriptiveStatistics();
     stats.setWindowSize(100);

     // Read data from an input stream,
     // displaying the mean of the most recent 100 observations
     // after every 100 observations
     long nLines = 0;
     while (line != null) {
     line = in.readLine();
     stats.addValue(Double.parseDouble(line.trim()));
     if (nLines == 100) {
     nLines = 0;
     System.out.println(stats.getMean());
     }
     }
     in.close();




     Compute statistics for multiple samples and overall statistics concurrently

     // Create a AggregateSummaryStatistics instance to accumulate the overall statistics
     // and AggregatingSummaryStatistics for the subsamples
     AggregateSummaryStatistics aggregate = new AggregateSummaryStatistics();
     SummaryStatistics setOneStats = aggregate.createContributingStatistics();
     SummaryStatistics setTwoStats = aggregate.createContributingStatistics();
     // Add values to the subsample aggregates
     setOneStats.addValue(2);
     setOneStats.addValue(3);
     setTwoStats.addValue(2);
     setTwoStats.addValue(4);
     ...
     // Full sample data is reported by the aggregate
     double totalSampleSum = aggregate.getSum();

     The above approach has the disadvantages that the addValue calls must be synchronized on the SummaryStatistics instance maintained by the aggregate and each value addition updates the aggregate as well as the subsample. For applications that can wait to do the aggregation until all values have been added, a static aggregate method is available, as shown in the following example. This method should be used when aggregation needs to be done across threads.



     // Create SummaryStatistics instances for the subsample data
     SummaryStatistics setOneStats = new SummaryStatistics();
     SummaryStatistics setTwoStats = new SummaryStatistics();
     // Add values to the subsample SummaryStatistics instances
     setOneStats.addValue(2);
     setOneStats.addValue(3);
     setTwoStats.addValue(2);
     setTwoStats.addValue(4);
     ...
     // Aggregate the subsample statistics
     Collection<SummaryStatistics> aggregate = new ArrayList<SummaryStatistics>();
     aggregate.add(setOneStats);
     aggregate.add(setTwoStats);
     StatisticalSummary aggregatedStats = AggregateSummaryStatistics.aggregate(aggregate);

     // Full sample data is reported by aggregatedStats
     double totalSampleSum = aggregatedStats.getSum();
     ******************************/

}
