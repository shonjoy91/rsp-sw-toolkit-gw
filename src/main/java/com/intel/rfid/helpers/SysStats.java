/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.helpers;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class SysStats {

    public static class MemoryInfo {
        public final long heapUsed;
        public final long heapFree;
        public final long heapTotal;
        public final long heapMax;
        public final long systemPhysical;

        public final String strHeapUsed;
        public final String strHeapFree;
        public final String strHeapTotal;
        public final String strHeapMax;
        public final String strSystemPhysical;

        public MemoryInfo(long _heapTotal, long _heapFree,
                          long _heapMax, long _systemPhysical) {
            heapTotal = _heapTotal;
            heapFree = _heapFree;
            heapUsed = heapTotal - heapFree;
            heapMax = _heapMax;
            systemPhysical = _systemPhysical;

            strHeapTotal = formatMemory(heapTotal);
            strHeapFree = formatMemory(heapFree);
            strHeapUsed = formatMemory(heapUsed);
            strHeapMax = formatMemory(heapMax);
            strSystemPhysical = formatMemory(systemPhysical);
        }
    }

    public static MemoryInfo getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        OperatingSystemMXBean mxbean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return new MemoryInfo(runtime.totalMemory(),
                              runtime.freeMemory(),
                              runtime.maxMemory(),
                              mxbean.getTotalPhysicalMemorySize());
    }

    public static class CPUInfo {
        public final int processThreads;
        public final double processLoad;
        public final double systemLoad;

        public final String strProcessThreads;
        public final String strProcessLoad;
        public final String strSystemLoad;

        public CPUInfo(int _processThreads, double _processLoad, double _systemLoad) {
            processThreads = _processThreads;
            processLoad = _processLoad;
            systemLoad = _systemLoad;

            strProcessThreads = String.valueOf(processThreads);
            strProcessLoad = formatLoad(processLoad);
            strSystemLoad = formatLoad(systemLoad);
        }
    }

    public static CPUInfo getCPUInfo() {

        OperatingSystemMXBean osMxBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        return new CPUInfo(threadMxBean.getThreadCount(),
                           osMxBean.getProcessCpuLoad(),
                           osMxBean.getSystemCpuLoad());
    }


    public static String formatLoad(double _load) {
        return String.format("%.1f%%", _load * 100.0);
    }

    public static final double KB_FACTOR = 1024.0;
    public static final double MB_FACTOR = 1024.0 * 1024.0;
    public static final double GB_FACTOR = 1024.0 * 1024.0 * 1024.0;

    public static String formatMemory(long _bytes) {
        if (_bytes < KB_FACTOR) {
            return String.format("%d B", _bytes);
        } else if (_bytes < MB_FACTOR) {
            return String.format("%d KB", Math.round(_bytes / KB_FACTOR));
        } else if (_bytes < GB_FACTOR) {
            return String.format("%d MB", Math.round(_bytes / MB_FACTOR));
        } else {
            return String.format("%.2f GB", _bytes / GB_FACTOR);
        }
    }
}
