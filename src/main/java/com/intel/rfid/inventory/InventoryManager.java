/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.data.InventorySummary;
import com.intel.rfid.api.data.ScheduleRunState;
import com.intel.rfid.api.data.TagInfo;
import com.intel.rfid.api.data.TagStatsInfo;
import com.intel.rfid.api.sensor.InventoryDataNotification;
import com.intel.rfid.api.sensor.TagRead;
import com.intel.rfid.downstream.DownstreamManager;
import com.intel.rfid.gateway.ConfigManager;
import com.intel.rfid.gateway.Env;
import com.intel.rfid.helpers.DateTimeHelper;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.StringHelper;
import com.intel.rfid.helpers.SysStats;
import com.intel.rfid.schedule.ScheduleManager;
import com.intel.rfid.schedule.SchedulerSummary;
import com.intel.rfid.sensor.SensorPlatform;
import com.intel.rfid.tag.Tag;
import com.intel.rfid.tag.TagEvent;
import com.intel.rfid.tag.TagState;
import com.intel.rfid.upstream.UpstreamInventoryEventInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.intel.rfid.api.data.Personality.EXIT;
import static com.intel.rfid.api.data.Personality.POS;
import static com.intel.rfid.tag.TagState.DEPARTED_EXIT;
import static com.intel.rfid.tag.TagState.DEPARTED_POS;
import static com.intel.rfid.tag.TagState.EXITING;
import static com.intel.rfid.tag.TagState.PRESENT;

public class InventoryManager
        implements DownstreamManager.InventoryDataListener,
                   ScheduleManager.RunStateListener {

    protected Logger log = LoggerFactory.getLogger(getClass());

    public interface UpstreamEventListener {
        void onUpstreamEvent(UpstreamInventoryEventInfo uie);
    }

    private final HashSet<UpstreamEventListener> upstreamEventListeners = new HashSet<>();

    public void addUpstreamEventListener(UpstreamEventListener _l) {
        synchronized (upstreamEventListeners) {
            upstreamEventListeners.add(_l);
        }
    }

    public void removeUpstreamEventListener(UpstreamEventListener _l) {
        synchronized (upstreamEventListeners) {
            upstreamEventListeners.remove(_l);
        }
    }

    private void publish(UpstreamInventoryEventInfo uie) {
        if (uie != null && !uie.data.isEmpty()) {
            synchronized (upstreamEventListeners) {
                for (UpstreamEventListener l : upstreamEventListeners) {
                    try {
                        l.onUpstreamEvent(uie);
                    } catch (Throwable t) {
                        log.error("error:", t);
                    }
                }
            }
        }
    }

    // public interface StatsUpdateListener {
    //     void onStatsUpdate(TagStatsUpdate _statsUpdate);
    // }
    //
    // private final HashMap<String, HashSet<StatsUpdateListener>> statsUpdateListeners = new HashMap<>();
    //
    // public void subscribeStats(String _epc, StatsUpdateListener _s) {
    //     synchronized (statsUpdateListeners) {
    //         statsUpdateListeners.computeIfAbsent(_epc, k -> new HashSet<>()).add(_s);
    //     }
    // }
    //
    // public void unsubscribeStats(String _epc, StatsUpdateListener _s) {
    //     synchronized (statsUpdateListeners) {
    //         HashSet<StatsUpdateListener> listeners = statsUpdateListeners.get(_epc);
    //         if (listeners == null) { return; }
    //         listeners.remove(_s);
    //         if (listeners.isEmpty()) {
    //             statsUpdateListeners.remove(_epc);
    //         }
    //     }
    // }
    //
    // private void notifyTagStats(Tag _tag) {
    //     synchronized (statsUpdateListeners) {
    //         if (statsUpdateListeners.isEmpty()) { return; }
    //         if (!statsUpdateListeners.containsKey(_tag.getEPC())) { return; }
    //
    //         HashSet<StatsUpdateListener> listeners = statsUpdateListeners.get(_tag.getEPC());
    //
    //         TagStatsUpdate update = _tag.getStatsUpdate();
    //         for (StatsUpdateListener l : listeners) {
    //             try {
    //                 l.onStatsUpdate(update);
    //             } catch (Throwable t) {
    //                 log.error("error:", t);
    //             }
    //         }
    //     }
    // }


    protected final TreeMap<String, Tag> inventory = new TreeMap<>();
    private final Map<String, Set<Tag>> exitingTags = new TreeMap<>();

    // since access to the inventory needs to always be synchronized anyway
    // this executor could probably be single threaded...or perhaps an executor per datastore?
    private static final int SCHED_THREAD_POOL_SIZE = 3;
    protected ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SCHED_THREAD_POOL_SIZE);

    public boolean start() {
        if (scheduler.isShutdown()) {
            scheduler = Executors.newScheduledThreadPool(SCHED_THREAD_POOL_SIZE);
        }
        restore();
        ageout();
        scheduleAggregateDepartedTask();
        scheduleReadRateStatsTask();
        schedulePersistence();
        log.info(getClass().getSimpleName() + " started");
        return true;
    }

    public boolean stop() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                    log.error("timeout waiting for scheduler to finish");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("interrupted waiting for scheduler to shut down");
        }
        closeTagStatsZip();
        ageout();
        persist();
        log.info(getClass().getSimpleName() + " stopped");
        return true;
    }

    private AtomicLong currentReadsPerSecond = new AtomicLong(0);

    private AtomicLong cumulativeReads = new AtomicLong(0);
    private static final int READ_RATE_STATS_INTERVAL_SEC = 3;

    private void scheduleReadRateStatsTask() {
        scheduler.scheduleWithFixedDelay(this::doReadRateStatsTask,
                                         READ_RATE_STATS_INTERVAL_SEC,
                                         READ_RATE_STATS_INTERVAL_SEC,
                                         TimeUnit.SECONDS);
    }

    private void doReadRateStatsTask() {
        try {
            long curReadRate = currentReadsPerSecond.getAndSet(cumulativeReads.getAndSet(0) / READ_RATE_STATS_INTERVAL_SEC);
            if (curReadRate > 0 && log.isInfoEnabled()) {
                SysStats.MemoryInfo memInfo = SysStats.getMemoryInfo();
                SysStats.CPUInfo cpuInfo = SysStats.getCPUInfo();
                log.info(String.format("rds/sec: %6d   h-used: %6s   h-tot: %6s   h-max: %6s   sys: %6s",
                                       curReadRate,
                                       memInfo.strHeapUsed, memInfo.strHeapTotal, memInfo.strHeapMax,
                                       cpuInfo.strSystemLoad));
            }
        } catch (Throwable _t) {
            log.error("error:", _t);
        }
    }

    private void schedulePersistence() {
        scheduler.scheduleWithFixedDelay(this::persist, 3, 3, TimeUnit.MINUTES);
    }

    public void unload() {
        snapshot();
        synchronized (inventory) {
            inventory.clear();
            File f = CACHE_PATH.toFile();
            if (f.exists() && !f.delete()) {
                log.error("Unable to delete inventory cache");
            }
        }
        synchronized (exitingTags) {
            exitingTags.clear();
        }
    }

    @Override
    public synchronized void onInventoryData(InventoryDataNotification _invDataNotification, SensorPlatform _rsp) {
        UpstreamInventoryEventInfo uie = new UpstreamInventoryEventInfo();
        synchronized (inventory) {
            for (TagRead tagRead : _invDataNotification.params.data) {
                processReadData(uie, _rsp, tagRead);
            }
        }
        publish(uie);
        cumulativeReads.addAndGet(_invDataNotification.params.data.size());
    }

    protected void processReadData(UpstreamInventoryEventInfo uie,
                                   SensorPlatform _rsp,
                                   TagRead _tagRead) {

        if (_tagRead.rssi < _rsp.getMinRssiDbm10X()) {
            return;
        }
        String epc = _tagRead.epc;

        // assume that invetory is locked outside of this method (looping)
        Tag tag = inventory.get(epc);
        if (tag == null) {
            tag = new Tag(epc);
            inventory.put(epc, tag);
        }

        PreviousTag prev = new PreviousTag(tag);
        tag.update(_rsp, _tagRead, rssiAdjuster);

        // check for state transitions
        switch (prev.state) {

            case UNKNOWN:
                // Point of sale NEVER adds new tags to the inventory
                // for the use case of POS reader might be the first
                // sensor in the store hallway to see a tag etc. so
                // need to prevent premature departures
                if (_rsp.hasPersonality(POS)) {
                    break;
                }
                uie.add(tag, TagEvent.arrival);
                tag.setState(PRESENT);
                break;

            case PRESENT:
                // check POS first so it has priority in case a sensor has been assigned
                // both personalities
                if (_rsp.hasPersonality(POS)) {
                    if (!checkDepartPOS(tag, uie)) {
                        checkMovement(tag, prev, uie);
                    }
                } else {
                    checkExiting(_rsp, tag);
                    checkMovement(tag, prev, uie);
                }
                break;

            case EXITING:
                if (_rsp.hasPersonality(POS)) {
                    checkDepartPOS(tag, uie);
                } else {

                    if (!_rsp.hasPersonality(EXIT) &&
                            _rsp.getDeviceId().equals(tag.getDeviceLocation())) {
                        tag.setState(PRESENT);
                    }

                    checkMovement(tag, prev, uie);
                }
                break;

            case DEPARTED_EXIT:
                if (_rsp.hasPersonality(POS)) {
                    break;
                }
                doTagReturn(tag, prev, uie);
                checkExiting(_rsp, tag);
                break;

            case DEPARTED_POS:
                if (_rsp.hasPersonality(POS)) {
                    break;
                }
                // Such a tag must remain in the DEPARTED state for 
                // a configurable amount of time (i.e. 1 day) 
                long onOrBefore = tag.getLastRead() - getPOSReturnThreshold();
                if (tag.getLastDeparted() < onOrBefore) {
                    doTagReturn(tag, prev, uie);
                    checkExiting(_rsp, tag);
                }
                break;
        }
    }

    protected void doTagReturn(Tag _tag, PreviousTag _prev, UpstreamInventoryEventInfo _uie) {

        if (_prev.facility != null && _prev.facility.equals(_tag.getFacility())) {
            _uie.add(_tag, TagEvent.returned);
        } else {
            _uie.add(_tag, TagEvent.arrival);
        }
        _tag.setState(PRESENT);

    }

    protected void checkMovement(Tag _tag, PreviousTag _prev, UpstreamInventoryEventInfo _uie) {

        if (_prev.location != null && !_prev.location.equals(_tag.getLocation())) {
            if (_prev.facility != null && !_prev.facility.equals(_tag.getFacility())) {
                // change facility
                _uie.add(_tag.getEPC(), _tag.getTID(), _prev.location, _prev.facility, TagEvent.departed,
                         _prev.lastRead);
                _uie.add(_tag, TagEvent.arrival);
            } else {
                _uie.add(_tag, TagEvent.moved);
            }
        }
    }

    protected boolean checkDepartPOS(Tag _tag, UpstreamInventoryEventInfo _uie) {

        // if tag is ever read by a POS, it immediately generates a departed event
        long expiration = _tag.getLastRead() - getPOSDepartedThreshold();
        if (_tag.getLastArrived() < expiration) {
            _tag.setState(DEPARTED_POS);
            _uie.add(_tag, TagEvent.departed);
            log.info("{}  {}", DEPARTED_POS, _tag);
            return true;
        }

        return false;
    }

    protected ScheduleRunState scheduleRunState = ScheduleRunState.INACTIVE;

    protected void checkExiting(SensorPlatform _rsp, Tag _tag) {
        if (!_rsp.hasPersonality(EXIT)) { return; }
        // the tag has to belong to the current sensor for anything to happen
        if (!_rsp.getDeviceId().equals(_tag.getDeviceLocation())) { return; }
        _tag.setState(EXITING);
        addExiting(_rsp.getFacilityId(), _tag);
    }

    protected void clearExiting() {
        synchronized (exitingTags) {
            for (Set<Tag> tags : exitingTags.values()) {
                for (Tag t : tags) {
                    // test just to be sure, this should not be necessary but belt and suspenders
                    if (t.getState() == EXITING) {
                        t.setState(PRESENT, t.getLastArrived());
                    }
                }
                tags.clear();
            }
            exitingTags.clear();
        }
    }

    protected void addExiting(String _facilityId, Tag tag) {
        synchronized (exitingTags) {
            Set<Tag> tags = exitingTags.computeIfAbsent(_facilityId, k -> new TreeSet<>());
            tags.add(tag);
        }
    }

    @Override
    public void onScheduleRunState(ScheduleRunState _current, SchedulerSummary _summary) {
        log.info("onScheduleRunState: {}", _current);
        clearExiting();
        scheduleRunState = _current;
    }


    private final RssiAdjuster rssiAdjuster = new RssiAdjuster();

    public RssiAdjuster getRssiAdjuster() {
        return rssiAdjuster;
    }

    public static final String CFG_KEY_AGEOUT = "inventory.ageout.hours";
    // 14 days worth, use hours because that is the unit used in the configuation
    public static final long DEFAULT_AGEOUT_HOURS = 336;

    public void ageout() {
        // default 14 days ago
        long h = ConfigManager.instance.getOptLong(CFG_KEY_AGEOUT, DEFAULT_AGEOUT_HOURS);
        long expiration = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(h);
        int numRemoved = 0;
        synchronized (inventory) {
            Iterator<Tag> tagIter = inventory.values().iterator();
            while (tagIter.hasNext()) {
                Tag tag = tagIter.next();
                if (tag.getLastRead() < expiration) {
                    tagIter.remove();
                    numRemoved++;
                }
            }
        }
        log.info("inventory ageout removed: {}", numRemoved);
    }

    public void getSummary(InventorySummary _summary) {

        Map<TagState, AtomicInteger> invStateMap = new HashMap<>();
        invStateMap.put(PRESENT, new AtomicInteger(0));
        invStateMap.put(EXITING, new AtomicInteger(0));
        invStateMap.put(DEPARTED_EXIT, new AtomicInteger(0));
        invStateMap.put(DEPARTED_POS, new AtomicInteger(0));

        Map<TimeBucket, AtomicInteger> timeBucketMap = new HashMap<>();
        timeBucketMap.put(TimeBucket.within_last_01_min, new AtomicInteger(0));
        timeBucketMap.put(TimeBucket.from_01_to_05_min, new AtomicInteger(0));
        timeBucketMap.put(TimeBucket.from_05_to_30_min, new AtomicInteger(0));
        timeBucketMap.put(TimeBucket.from_30_to_60_min, new AtomicInteger(0));
        timeBucketMap.put(TimeBucket.from_60_min_to_24_hr, new AtomicInteger(0));
        timeBucketMap.put(TimeBucket.more_than_24_hr, new AtomicInteger(0));

        aggregateInventoryStates(invStateMap, timeBucketMap);

        _summary.tag_state_summary.PRESENT = invStateMap.get(PRESENT).get();
        _summary.tag_state_summary.EXITING = invStateMap.get(EXITING).get();
        _summary.tag_state_summary.DEPARTED_EXIT = invStateMap.get(DEPARTED_EXIT).get();
        _summary.tag_state_summary.DEPARTED_POS = invStateMap.get(DEPARTED_POS).get();

        _summary.tag_read_summary.reads_per_second = currentReadsPerSecond.get();
        _summary.tag_read_summary.within_last_01_min = timeBucketMap.get(TimeBucket.within_last_01_min).get();
        _summary.tag_read_summary.from_01_to_05_min = timeBucketMap.get(TimeBucket.from_01_to_05_min).get();
        _summary.tag_read_summary.from_05_to_30_min = timeBucketMap.get(TimeBucket.from_05_to_30_min).get();
        _summary.tag_read_summary.from_30_to_60_min = timeBucketMap.get(TimeBucket.from_30_to_60_min).get();
        _summary.tag_read_summary.from_60_min_to_24_hr = timeBucketMap.get(TimeBucket.from_60_min_to_24_hr).get();
        _summary.tag_read_summary.more_than_24_hr = timeBucketMap.get(TimeBucket.more_than_24_hr).get();

    }

    public void getTagInfo(String _filterPattern, Collection<TagInfo> _infoCollection) {
        Pattern p = StringHelper.regexWildcard(_filterPattern);
        try {
            synchronized (inventory) {
                for (Tag tag : inventory.values()) {
                    if (p == null || p.matcher(tag.getEPC()).matches()) {
                        _infoCollection.add(new TagInfo(tag.getEPC(),
                                                        tag.getTID(),
                                                        tag.getState(),
                                                        tag.getLocation(),
                                                        tag.getLastRead(),
                                                        tag.getFacility()));


                    }
                }
            }
        } catch (Exception e) {
            log.error("error:", e);
        }
    }

    public Collection<Tag> getTags(String _filterPattern) {
        Collection<Tag> tags = new TreeSet<>();
        Pattern p = StringHelper.regexWildcard(_filterPattern);
        try {
            synchronized (inventory) {
                for (Tag tag : inventory.values()) {
                    if (p == null || p.matcher(tag.getEPC()).matches()) {
                        tags.add(tag);
                    }
                }
            }
        } catch (Exception e) {
            log.error("error:", e);
        }
        return tags;
    }

    private void scheduleAggregateDepartedTask() {
        long interval = (getAggregateDepartedThreshold() / 5);
        scheduler.scheduleWithFixedDelay(this::doAggregateDepartedTask, interval, interval, TimeUnit.MILLISECONDS);
    }

    protected void doAggregateDepartedTask() {

        UpstreamInventoryEventInfo uie = new UpstreamInventoryEventInfo();
        long now = System.currentTimeMillis();
        long expiration = now - getAggregateDepartedThreshold();
        synchronized (exitingTags) {
            for (Set<Tag> tagSet : exitingTags.values()) {
                for (Iterator<Tag> iter = tagSet.iterator(); iter.hasNext(); ) {
                    Tag tag = iter.next();
                    // there may be some edge cases where the tag state
                    if (tag.getState() != EXITING) {
                        iter.remove();
                    } else if (tag.getLastRead() < expiration) {
                        tag.setState(DEPARTED_EXIT, now);
                        uie.add(tag, TagEvent.departed);
                        log.info("{} {}", TagEvent.departed, tag);
                        iter.remove();
                    }
                }
            }
        }
        publish(uie);
    }

    protected long getAggregateDepartedThreshold() {
        return ConfigManager.instance.getOptLong("inventory.aggregate.departed.threshold.millis",
                                                 TimeUnit.SECONDS.toMillis(30));
    }

    protected long getPOSDepartedThreshold() {
        return ConfigManager.instance.getOptLong("inventory.POS.departed.threshold.millis",
                                                 TimeUnit.HOURS.toMillis(1));
    }

    protected long getPOSReturnThreshold() {
        return ConfigManager.instance.getOptLong("inventory.POS.return.threshold.millis",
                                                 TimeUnit.DAYS.toMillis(1));
    }

    public static final Path CACHE_PATH = Env.resolveCache("current_inventory.json");
    protected static final ObjectMapper mapper = Jackson.getMapper();

    // support 2 types of cache formats while moving away from previous facility architecture
    public static class Cache {
        // previous inventory caches will match this object defintion and old
        // versions will restore using this one time
        public Map<String, List<Tag.Cached>> inventory = new HashMap<>();
        // since a tag is not just in a single facility at a time, a simple list
        // will be used going forward.
        public List<Tag.Cached> tags = new ArrayList<>();
    }

    private SimpleDateFormat sdf = DateTimeHelper.newLocalFormatterMachine();

    public String snapshot() {

        // need to check for older files and delete them so they don't pile up.
        Path p = Env.getSnapshotPath();

        File[] files = p.toFile().listFiles();
        if (files != null) {
            // leave files for 21 days
            long threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(21);
            for (File f : files) {
                if (f.lastModified() < threshold) {
                    log.info("deleting snapshot " + f.getAbsolutePath());
                    if (!f.delete()) {
                        log.warn("Unable to delete " + f.getAbsolutePath());
                    }
                }
            }
        }

        Cache cache = new Cache();

        synchronized (inventory) {
            for (Tag tag : inventory.values()) {
                List<Tag.Cached> tags = cache.inventory.computeIfAbsent(tag.getFacility(), k -> new ArrayList<>());
                tags.add(tag.toCached());
            }
        }

        if (cache.inventory.size() == 0) {
            log.warn("snapshot NO_INVENTORY");
            return "NO_INVENTORY";
        }

        String nl = System.getProperty("line.separator");
        String timestamp = sdf.format(new Date());
        Path snapPath = Env.resolveSnapshotPath("gw_inventory_" + timestamp + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(snapPath))) {

            for (String facilityId : cache.inventory.keySet()) {

                zos.putNextEntry(new ZipEntry("gw_inventory_" + facilityId + "_" + timestamp + ".csv"));

                String s = "Facility,Epc,State,Last Seen,Location" + nl;

                zos.write(s.getBytes(StandardCharsets.UTF_8));

                List<Tag.Cached> tags = cache.inventory.get(facilityId);
                if (tags != null) {
                    for (Tag.Cached tag : tags) {

                        s = facilityId + "," +
                                tag.epc + "," +
                                tag.state + "," +
                                tag.lastRead + "," +
                                tag.location + nl;

                        zos.write(s.getBytes(StandardCharsets.UTF_8));
                    }
                }

                zos.closeEntry();
            }
            log.info("snapshot {}", snapPath);
        } catch (IOException e) {
            log.error("failed writing {}:", snapPath, e);
        }

        return snapPath.toString();
    }

    public String snapshotStats(String _statsRegex) {

        // need to check for older files and delete them so they don't pile up.
        File dir = new File("stats");
        try {
            if (!dir.exists()) {
                Files.createDirectories(dir.toPath());
            }
        } catch (IOException e) {
            log.error("{}", dir.getAbsolutePath(), e);
            return dir.getAbsolutePath() + ": " + e.getMessage();
        }

        String timestamp = sdf.format(new Date());
        File file = new File(dir, "stats_snap_" + timestamp + ".csv");

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {

            pw.println(Tag.STATS_DETAIL_CSV_HDR);
            Pattern p = StringHelper.regexWildcard(_statsRegex);
            printStatsDetail(p, pw);

        } catch (IOException e) {
            log.error("{}", dir.getAbsolutePath(), e);
            return dir.getAbsolutePath() + ": " + e.getMessage();
        }

        return file.getAbsolutePath();
    }

    public String snapshotWaypoints(String _statsRegex) {

        // need to check for older files and delete them so they don't pile up.
        File dir = new File("stats");
        try {
            if (!dir.exists()) {
                Files.createDirectories(dir.toPath());
            }
        } catch (IOException e) {
            log.error("{}", dir.getAbsolutePath(), e);
            return dir.getAbsolutePath() + ": " + e.getMessage();
        }

        String timestamp = sdf.format(new Date());
        File file = new File(dir, "waypoints_snap_" + timestamp + ".csv");

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {

            Pattern p = StringHelper.regexWildcard(_statsRegex);

            synchronized (inventory) {
                for (Tag tag : inventory.values()) {
                    if (p == null || p.matcher(tag.getEPC()).matches()) {
                        tag.waypoints(pw);
                    }
                }
            }

        } catch (IOException e) {
            log.error("{}", dir.getAbsolutePath(), e);
            return dir.getAbsolutePath() + ": " + e.getMessage();
        }

        return file.getAbsolutePath();
    }

    private void persist() {

        // get a copy of the inventory into minimal JSON object
        Cache cache = new Cache();
        synchronized (inventory) {
            for (Tag invTag : inventory.values()) {
                cache.tags.add(invTag.toCached());
            }
        }

        if (cache.tags.size() > 0) {
            try (OutputStream os = Files.newOutputStream(CACHE_PATH)) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(os, cache);
                log.info("wrote {}", CACHE_PATH);
            } catch (IOException e) {
                log.error("failed persisting inventory {}", e.getMessage());
            }
        } else {
            try {
                Files.deleteIfExists(CACHE_PATH);
            } catch (IOException _e) {
                log.warn("error {}", _e.getMessage());
            }
        }
    }

    public void restore() {

        if (!Files.exists(CACHE_PATH)) {
            return;
        }

        Cache cache = null;

        try (InputStream fis = Files.newInputStream(CACHE_PATH)) {

            cache = mapper.readValue(fis, Cache.class);
            log.info("Restored {}", CACHE_PATH);

        } catch (IOException e) {
            log.error("Failed to restore {}", CACHE_PATH, e);
        }

        if (cache == null) {
            return;
        }

        synchronized (inventory) {
            if (cache.tags.size() > 0) {
                for (Tag.Cached ct : cache.tags) {
                    inventory.put(ct.epc, Tag.fromCached(ct));
                }

            } else if (cache.inventory.size() > 0) {

                for (String fid : cache.inventory.keySet()) {
                    List<Tag.Cached> cacheTags = cache.inventory.get(fid);
                    if (cacheTags == null) {
                        continue;
                    }
                    for (Tag.Cached ct : cacheTags) {
                        // fix for the NAPA null  facility error
                        ct.facility = fid;
                        inventory.put(ct.epc, Tag.fromCached(ct));
                    }
                }

            }
        }
    }

    public void showSummary(PrintWriter _out) {

        Map<TagState, AtomicInteger> invStateMap = new HashMap<>();
        Map<TimeBucket, AtomicInteger> timeBucketMap = new HashMap<>();

        int totalTags = aggregateInventoryStates(invStateMap, timeBucketMap);

        _out.println("- Total Tags: " + totalTags);
        _out.println();

        _out.println("- State");
        for (TagState t : TagState.values()) {
            AtomicInteger count = invStateMap.get(t);
            if (count != null) {
                _out.println(String.format("%8d %s", count.get(), t));
            }
        }
        _out.println();

        _out.println("- Last Seen");
        for (TimeBucket t : TimeBucket.values()) {
            AtomicInteger count = timeBucketMap.get(t);
            if (count != null) {
                _out.println(String.format("%8d %s", count.get(), t));
            }
        }

    }

    public void showDetail(String _regex, PrintWriter _out) {
        Pattern p = StringHelper.regexWildcard(_regex);
        synchronized (inventory) {
            for (Tag tag : inventory.values()) {
                if (p == null || p.matcher(tag.getEPC()).matches()) {
                    _out.println(tag);
                }
            }
        }
    }

    public void showExiting(String _regex, PrintWriter _out) {
        Pattern p = StringHelper.regexWildcard(_regex);
        synchronized (exitingTags) {
            for (Set<Tag> tagSet : exitingTags.values()) {
                for (Tag tag : tagSet) {
                    if (p == null || p.matcher(tag.getEPC()).matches()) {
                        _out.println(tag);
                    }
                }
            }
        }
    }

    public TagStatsInfo getStatsInfo(String _filterPattern) {
        TagStatsInfo statsUpdate = new TagStatsInfo();
        Pattern p = StringHelper.regexWildcard(_filterPattern);
        synchronized (inventory) {
            for (Tag tag : inventory.values()) {
                if (p == null || p.matcher(tag.getEPC()).matches()) {
                    tag.getStatsUpdate(statsUpdate);
                }
            }
        }
        return statsUpdate;
    }

    public void showStats(String _regex, PrintWriter _out) {
        _out.println(Tag.STATS_SUMMARY_CSV_HDR);
        long now = System.currentTimeMillis();
        Pattern p = StringHelper.regexWildcard(_regex);
        try {
            synchronized (inventory) {
                for (Tag tag : inventory.values()) {
                    if (p == null || p.matcher(tag.getEPC()).matches()) {
                        tag.statsSummary(_out, now);
                    }
                }
            }
        } catch (Exception e) {
            log.error("error:", e);
        }

    }

    public void showWaypoints(String _regex, PrintWriter _out) {
        Pattern p = StringHelper.regexWildcard(_regex);
        synchronized (inventory) {
            for (Tag tag : inventory.values()) {
                if (p == null || p.matcher(tag.getEPC()).matches()) {
                    tag.waypoints(_out);
                }
            }
        }
    }

    private Future<?> statRecordFuture;
    private AtomicBoolean writingStats = new AtomicBoolean(false);
    private Path tagStatsPath;
    private ZipOutputStream tagStatsZipStream;
    private PrintWriter tagStatsWriter;
    private Pattern statsPattern = null;

    public void setInventoryRegex(String _regex) {
        statsPattern = StringHelper.regexWildcard(_regex);
    }

    public void checkInventoryRegex(PrintWriter _pw) {
        _pw.println(Tag.STATS_DETAIL_CSV_HDR);
        printStatsDetail(statsPattern, _pw);
        _pw.println();
        _pw.println("statsRegex: " + statsPattern.toString());
    }

    public String startRecordingStats() {

        if (tagStatsZipStream != null) {
            String msg = "stats recording already in progress";
            log.warn(msg);
            closeTagStatsZip();
        }

        String timestamp = sdf.format(new Date());

        String msg;
        tagStatsPath = Env.resolveStats("tag_stats_" + timestamp + ".zip");
        try {
            tagStatsZipStream = new ZipOutputStream(Files.newOutputStream(tagStatsPath));
            tagStatsZipStream.putNextEntry(new ZipEntry("tag_stats_" + timestamp + ".csv"));
            tagStatsWriter = new PrintWriter(tagStatsZipStream);
            tagStatsWriter.println(Tag.STATS_DETAIL_CSV_HDR);

            // start the task
            statRecordFuture = scheduler.scheduleAtFixedRate(new StatsToZip(), 5, 5, TimeUnit.SECONDS);

            msg = "tagstats file " + tagStatsPath.toString();
            log.info(msg);
        } catch (IOException e) {
            msg = "failed writing " + tagStatsPath.toString() +
                    " : " + e.getMessage();
            log.error(msg);
        }
        return msg;
    }

    public String stopRecordingStats() {

        if (statRecordFuture == null || tagStatsPath == null) {
            return "stats recording already stopped";
        }

        String msg;
        // stop the task
        statRecordFuture.cancel(false);
        // wait for a few seconds to finish
        int waitcount = 0;
        while (writingStats.get() && waitcount++ < 3) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        closeTagStatsZip();

        msg = "closed " + tagStatsPath.toString();
        log.info(msg);
        return msg;
    }

    private class StatsToZip implements Runnable {
        public void run() {
            if (tagStatsWriter != null) {
                writingStats.set(true);
                printStatsDetail(statsPattern, tagStatsWriter);
                writingStats.set(false);
            }
        }
    }

    private void closeTagStatsZip() {

        if (tagStatsWriter != null) {
            tagStatsWriter.flush();
            tagStatsWriter.close();
            tagStatsWriter = null;
        }
        if (tagStatsZipStream != null) {
            try {
                tagStatsZipStream.closeEntry();
                tagStatsZipStream.close();
            } catch (IOException ioe) {
                log.error("error: ", ioe);
            }
            tagStatsZipStream = null;
        }
    }

    // NOTE: don't print the header here,
    // this method can be called repeatedly for a single file or output
    private void printStatsDetail(Pattern _pattern, PrintWriter _writer) {
        try {
            long now = System.currentTimeMillis();
            synchronized (inventory) {
                for (Tag tag : inventory.values()) {
                    if (_pattern == null || _pattern.matcher(tag.getEPC()).matches()) {
                        tag.statsDetail(_writer, now);
                    }
                }
            }
        } catch (Exception e) {
            log.error("error:", e);
        }
    }

    private int aggregateInventoryStates(Map<TagState, AtomicInteger> _invStateMap,
                                         Map<TimeBucket, AtomicInteger> _timeBucketMap) {

        int totalTags = 0;
        synchronized (inventory) {
            long now = System.currentTimeMillis();
            for (Tag tag : inventory.values()) {

                totalTags++;
                AtomicInteger stateCount = _invStateMap.get(tag.getState());
                if (stateCount == null) {
                    stateCount = new AtomicInteger(0);
                    _invStateMap.put(tag.getState(), stateCount);
                }
                stateCount.getAndIncrement();


                long timeDiff = now - tag.getLastRead();
                for (TimeBucket bucket : TimeBucket.values()) {
                    if (timeDiff < bucket.millis) {
                        AtomicInteger bucketCount = _timeBucketMap.get(bucket);
                        if (bucketCount == null) {
                            bucketCount = new AtomicInteger(0);
                            _timeBucketMap.put(bucket, bucketCount);
                        }
                        bucketCount.getAndIncrement();
                        break;
                    }
                }
            }
        }

        return totalTags;
    }

}
