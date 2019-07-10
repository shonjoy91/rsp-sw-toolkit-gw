/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.inventory;

import com.intel.rfid.api.data.InventoryEventItem;
import com.intel.rfid.api.sensor.InventoryDataNotification;
import com.intel.rfid.api.sensor.TagRead;
import com.intel.rfid.gateway.ConfigManager;
import com.intel.rfid.gateway.MockGateway;
import com.intel.rfid.helpers.EnvHelper;
import com.intel.rfid.helpers.EpcHelper;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.helpers.StringHelper;
import com.intel.rfid.helpers.TestStore;
import com.intel.rfid.tag.Tag;
import com.intel.rfid.tag.TagEvent;
import com.intel.rfid.tag.TagState;
import com.intel.rfid.upstream.UpstreamInventoryEventInfo;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.intel.rfid.inventory.InventoryManager.CFG_KEY_AGEOUT;
import static com.intel.rfid.inventory.InventoryManager.DEFAULT_AGEOUT_HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InventoryManagerTest implements InventoryManager.UpstreamEventListener {

    @BeforeClass
    public static void beforeClass() throws Exception {
        EnvHelper.beforeBasicTests();
    }

    @AfterClass
    public static void afterClass() { EnvHelper.afterTests(); }

    private List<UpstreamInventoryEventInfo> upstreamEvents = new ArrayList<>();

    public void onUpstreamEvent(UpstreamInventoryEventInfo _uie) {
        upstreamEvents.add(_uie);
    }

    TestStore store;
    MockGateway gateway;
    MockInventoryManager invMgr;


    public InventoryManagerTest() {
        store = new TestStore();
        gateway = new MockGateway();
        invMgr = gateway.getMockInventoryManager();
        invMgr.unload();
        invMgr.addUpstreamEventListener(this);
    }

    @Test
    public void testTagArrival() {

        long readTimeOrig = System.currentTimeMillis();
        TagRead tagRead01 = store.generateReadData(readTimeOrig);
        TagRead tagRead02 = store.generateReadData(readTimeOrig);
        TagRead tagRead03 = store.generateReadData(readTimeOrig);
        InventoryEventItem item;

        store.sensorBack01.setMinRssiDbm10X(-600);
        tagRead01.rssi = -580;
        tagRead02.rssi = -620;
        UpstreamInventoryEventInfo uie = new UpstreamInventoryEventInfo();
        invMgr.processReadData(uie, store.sensorBack01, tagRead01);
        invMgr.processReadData(uie, store.sensorBack01, tagRead02);
        invMgr.processReadData(uie, store.sensorFrontPOS, tagRead03);

        Tag tag01 = invMgr.inventory.get(tagRead01.epc);
        Tag tag02 = invMgr.inventory.get(tagRead02.epc);
        Tag tag03 = invMgr.inventory.get(tagRead03.epc);

        assertThat(tag01).isNotNull();
        assertThat(tag01.getState()).isEqualTo(TagState.PRESENT);
        assertThat(tag01.getLocation()).isEqualTo(store.sensorBack01.asLocation());
        assertThat(tag02).isNull();
        assertThat(tag03).isNotNull();
        assertThat(tag03.getState()).isEqualTo(TagState.UNKNOWN);
        assertThat(tag03.getLocation()).isEqualTo(store.sensorFrontPOS.asLocation());

        assertThat(uie.data.size()).isEqualTo(1);

        item = uie.data.get(0);
        assertThat(item.event_type).isEqualTo(TagEvent.arrival.toString());
        assertThat(item.location).isEqualTo(store.sensorBack01.asLocation());

    }

    @Test
    public void testTagMove() {

        long readTimeOrig = System.currentTimeMillis();
        TagRead tagRead01 = store.generateReadData(readTimeOrig);
        TagRead tagRead02 = store.generateReadData(readTimeOrig);
        TagRead tagRead03 = store.generateReadData(readTimeOrig);
        InventoryEventItem item;

        UpstreamInventoryEventInfo uie = new UpstreamInventoryEventInfo();
        invMgr.processReadData(uie, store.sensorBack01, tagRead01);
        invMgr.processReadData(uie, store.sensorBack01, tagRead02);
        invMgr.processReadData(uie, store.sensorBack01, tagRead03);

        Tag tag01 = invMgr.inventory.get(tagRead01.epc);
        Tag tag02 = invMgr.inventory.get(tagRead02.epc);
        Tag tag03 = invMgr.inventory.get(tagRead03.epc);

        // move tag01 to the front
        uie = new UpstreamInventoryEventInfo();
        tagRead01.rssi = store.rssiStrong();
        for (int x = 0; x < 4; x++) {
            invMgr.processReadData(uie, store.sensorFront01, tagRead01);
        }
        assertThat(uie.data.size()).isGreaterThanOrEqualTo(2);

        item = uie.data.get(0);
        assertThat(item.event_type).isEqualTo(TagEvent.departed.toString());
        assertThat(item.location).isEqualTo(store.sensorBack01.asLocation());

        item = uie.data.get(1);
        assertThat(item.event_type).isEqualTo(TagEvent.arrival.toString());
        assertThat(item.location).isEqualTo(store.sensorFront01.asLocation());

        assertThat(tag01.getLocation()).isEqualTo(store.sensorFront01.asLocation());

        // move tag02 to same facility, different sensor
        uie = new UpstreamInventoryEventInfo();
        tagRead02.rssi = store.rssiStrong();
        for (int x = 0; x < 4; x++) {
            invMgr.processReadData(uie, store.sensorBack02, tagRead02);
        }
        assertThat(uie.data.size()).isGreaterThanOrEqualTo(1);
        item = uie.data.get(0);
        assertThat(item.event_type).isEqualTo(TagEvent.moved.toString());
        assertThat(item.location).isEqualTo(store.sensorBack02.asLocation());

        // test that tag stays at new location even with concurrent reads from weaker sensor
        // MOVE back doesn't happen with weak RSSI and  departed
        uie = new UpstreamInventoryEventInfo();
        tagRead02.rssi = store.rssiWeak();
        for (int x = 0; x < 4; x++) {
            invMgr.processReadData(uie, store.sensorBack03, tagRead02);
        }
        assertThat(uie.data.size()).isEqualTo(0);
        assertThat(tag02.getLocation()).isEqualTo(store.sensorBack02.asLocation());


        // move tag03 to a different antenna port on same sensor
        tagRead03.antenna_id = 33;
        uie = new UpstreamInventoryEventInfo();
        tagRead03.rssi = store.rssiStrong();
        for (int x = 0; x < 4; x++) {
            invMgr.processReadData(uie, store.sensorBack01, tagRead03);
        }
        assertThat(uie.data.size()).isGreaterThanOrEqualTo(1);
        item = uie.data.get(0);
        assertThat(item.event_type).isEqualTo(TagEvent.moved.toString());
        assertThat(item.location).isEqualTo(store.sensorBack01.getDeviceId() + "-33");
        assertThat(tag03.getLocation()).isEqualTo(store.sensorBack01.getDeviceId() + "-33");

        StringWriter sw = new StringWriter();
        invMgr.showDetail(null, new PrettyPrinter(sw));
        sw.flush();
        LoggerFactory.getLogger("tagMoved").info("inventory detail follows:\n{}", sw.toString());
    }

    @Test
    public void testBasicExit() {

        InventoryEventItem item;
        TagRead tagRead01 = store.generateReadData(System.currentTimeMillis());
        tagRead01.rssi = store.rssiMin();

        // one tag read by an EXIT will not make the tag go exiting.
        UpstreamInventoryEventInfo uie = new UpstreamInventoryEventInfo();
        for (int x = 0; x < 4; x++) {
            invMgr.processReadData(uie, store.sensorBack01, tagRead01);
        }
        invMgr.processReadData(uie, store.sensorFrontExit, tagRead01);
        Tag tag01 = invMgr.inventory.get(tagRead01.epc);
        assertThat(tag01).isNotNull();
        assertThat(tag01.getState()).isEqualTo(TagState.PRESENT);

        // moving to an exit sensor will put tag in exiting
        // moving to an exit sensor in another facility will generate departure / arrival
        uie = new UpstreamInventoryEventInfo();
        tagRead01.rssi = store.rssiWeak();
        for (int x = 0; x < 10; x++) {
            invMgr.processReadData(uie, store.sensorFrontExit, tagRead01);
        }
        assertThat(tag01.getState()).isEqualTo(TagState.EXITING);
        assertThat(uie.data.size()).isGreaterThanOrEqualTo(2);
        item = uie.data.get(0);
        assertThat(item.event_type).isEqualTo(TagEvent.departed.toString());
        assertThat(item.location).isEqualTo(store.sensorBack01.asLocation());
        assertThat(item.facility_id).isEqualTo(store.sensorBack01.getFacilityId());

        item = uie.data.get(1);
        assertThat(item.event_type).isEqualTo(TagEvent.arrival.toString());
        assertThat(item.location).isEqualTo(store.sensorFrontExit.asLocation());
        assertThat(item.facility_id).isEqualTo(store.sensorFrontExit.getFacilityId());

        for (int x = 0; x < 20; x++) {
            // clear exiting by moving to another sensor
            tagRead01.rssi = store.rssiMin();
            invMgr.processReadData(uie, store.sensorFrontExit, tagRead01);
            tagRead01.rssi = store.rssiStrong();
            invMgr.processReadData(uie, store.sensorFront01, tagRead01);
        }
        assertThat(tag01.getState()).isEqualTo(TagState.PRESENT);

        tagRead01.rssi = store.rssiMax();
        for (int x = 0; x < 20; x++) {
            invMgr.processReadData(uie, store.sensorFrontExit, tagRead01);
        }
        assertThat(tag01.getState()).isEqualTo(TagState.EXITING);
    }

    @Test
    public void testExitingArrivalDepartures() {

        TagRead tagRead01 = store.generateReadData(System.currentTimeMillis());
        tagRead01.rssi = store.rssiMin();

        UpstreamInventoryEventInfo uie = new UpstreamInventoryEventInfo();
        for (int x = 0; x < 4; x++) {
            invMgr.processReadData(uie, store.sensorBack01, tagRead01);
        }
        // one tag read by an EXIT will not make the tag go exiting.
        invMgr.processReadData(uie, store.sensorFrontExit, tagRead01);
        Tag tag01 = invMgr.inventory.get(tagRead01.epc);
        assertThat(tag01).isNotNull();
        assertThat(tag01.getState()).isEqualTo(TagState.PRESENT);

        tagRead01.rssi = store.rssiWeak();
        for (int x = 0; x < 10; x++) {
            invMgr.processReadData(uie, store.sensorFrontExit, tagRead01);
        }
        assertThat(tag01.getState()).isEqualTo(TagState.EXITING);

        for (int x = 0; x < 20; x++) {
            // clear exiting by moving to another sensor
            tagRead01.rssi = store.rssiMin();
            invMgr.processReadData(uie, store.sensorFrontExit, tagRead01);
            tagRead01.rssi = store.rssiStrong();
            invMgr.processReadData(uie, store.sensorFront01, tagRead01);
        }
        assertThat(tag01.getState()).isEqualTo(TagState.PRESENT);

        tagRead01.rssi = store.rssiMax();
        for (int x = 0; x < 20; x++) {
            invMgr.processReadData(uie, store.sensorFrontExit, tagRead01);
        }
        assertThat(tag01.getState()).isEqualTo(TagState.EXITING);
    }

    @Test
    public void testTagDepartAndReturnFromExit() {

        long readTimeOrig = System.currentTimeMillis();
        TagRead tagRead01 = store.generateReadData(readTimeOrig);
        TagRead tagRead02 = store.generateReadData(readTimeOrig);
        TagRead tagRead03 = store.generateReadData(readTimeOrig);
        TagRead tagRead04 = store.generateReadData(readTimeOrig);
        InventoryEventItem item;

        UpstreamInventoryEventInfo uie = new UpstreamInventoryEventInfo();
        invMgr.processReadData(uie, store.sensorBack01, tagRead01);
        invMgr.processReadData(uie, store.sensorBack01, tagRead02);
        invMgr.processReadData(uie, store.sensorBack01, tagRead03);
        invMgr.processReadData(uie, store.sensorBack01, tagRead04);

        Tag tag01 = invMgr.inventory.get(tagRead01.epc);
        Tag tag02 = invMgr.inventory.get(tagRead02.epc);
        Tag tag03 = invMgr.inventory.get(tagRead03.epc);
        Tag tag04 = invMgr.inventory.get(tagRead04.epc);

        // dampen the rssi from the current sensor
        tagRead01.rssi = store.rssiMin();
        tagRead02.rssi = store.rssiMin();
        tagRead03.rssi = store.rssiMin();
        for (int x = 0; x < 20; x++) {
            invMgr.processReadData(uie, store.sensorFront01, tagRead01);
            invMgr.processReadData(uie, store.sensorFront01, tagRead02);
            invMgr.processReadData(uie, store.sensorFront01, tagRead03);
        }

        tagRead01.rssi = store.rssiMax();
        tagRead02.rssi = store.rssiMax();
        tagRead03.rssi = store.rssiMax();
        for (int x = 0; x < 20; x++) {
            invMgr.processReadData(uie, store.sensorFrontExit, tagRead01);
            invMgr.processReadData(uie, store.sensorFrontExit, tagRead02);
            invMgr.processReadData(uie, store.sensorFrontExit, tagRead03);
        }
        // exit personalities do not trigger exiting tags when scheduler
        // is DYNAMIC and not in MOBILITY which is the default scheduler state
        // so even though the tag moved to the exit, it is not in the exiting table
        assertThat(tag01.getLocation()).isEqualTo(store.sensorFrontExit.asLocation());
        assertThat(tag02.getLocation()).isEqualTo(store.sensorFrontExit.asLocation());

        assertThat(tag01.getState()).isEqualTo(TagState.EXITING);
        assertThat(tag02.getState()).isEqualTo(TagState.EXITING);
        assertThat(tag03.getState()).isEqualTo(TagState.EXITING);
        assertThat(tag04.getState()).isEqualTo(TagState.PRESENT);

        upstreamEvents.clear();
        invMgr.doAggregateDepartedTask();
        assertThat(upstreamEvents).isEmpty();

        upstreamEvents.clear();
        tagRead01.last_read_on = readTimeOrig - (invMgr.getAggregateDepartedThreshold() + 5);

        invMgr.processReadData(uie, store.sensorFrontExit, tagRead01);

        invMgr.doAggregateDepartedTask();
        assertThat(tag01.getState()).isEqualTo(TagState.DEPARTED_EXIT);
        assertThat(upstreamEvents).hasSize(1);
        assertThat(upstreamEvents.get(0).data).hasSize(1);
        item = upstreamEvents.get(0).data.get(0);
        assertThat(item.event_type).isEqualTo(TagEvent.departed.toString());
        assertThat(item.location).isEqualTo(store.sensorFrontExit.asLocation());

        // abort an exiting state by moving tag to different sensor
        tagRead02.rssi = store.rssiMin();
        for (int x = 0; x < 20; x++) {
            invMgr.processReadData(uie, store.sensorFrontExit, tagRead02);
        }

        tagRead02.rssi = store.rssiMax();
        for (int x = 0; x < 20; x++) {
            invMgr.processReadData(uie, store.sensorFront03, tagRead02);
        }
        assertThat(tag02.getState()).isEqualTo(TagState.PRESENT);

        // read an exiting tag from a POS reader 
        // - without enough time after arrival for depart due to POS
        assertThat(tag03.getState()).isEqualTo(TagState.EXITING);
        assertThat(tag03.getLastArrived()).isEqualTo(readTimeOrig);
        tagRead03.last_read_on = readTimeOrig + invMgr.getPOSDepartedThreshold() - 500;
        uie = new UpstreamInventoryEventInfo();
        invMgr.processReadData(uie, store.sensorFrontPOS, tagRead03);
        assertThat(tag03.getState()).isEqualTo(TagState.EXITING);
        assertThat(uie.data.size()).isEqualTo(0);

        // read an exiting tag from a POS reader 
        // - without enough time after arrival for depart due to POS
        tagRead03.last_read_on = readTimeOrig + invMgr.getPOSDepartedThreshold() + 500;
        uie = new UpstreamInventoryEventInfo();
        invMgr.processReadData(uie, store.sensorFrontPOS, tagRead03);
        assertThat(tag03.getState()).isEqualTo(TagState.DEPARTED_POS);
        assertThat(uie.data.size()).isEqualTo(1);


        // RETURN
        // move all the tags to the exit sensor as this is what
        // it would be for real
        tagRead01.last_read_on += TimeUnit.MINUTES.toMillis(15);
        tagRead02.last_read_on += TimeUnit.MINUTES.toMillis(15);
        tagRead03.last_read_on += TimeUnit.MINUTES.toMillis(15);
        tagRead04.last_read_on += TimeUnit.MINUTES.toMillis(15);
        for (int x = 0; x < 20; x++) {
            invMgr.processReadData(uie, store.sensorFrontExit, tagRead01);
            invMgr.processReadData(uie, store.sensorFrontExit, tagRead02);
            invMgr.processReadData(uie, store.sensorFrontExit, tagRead03);
            invMgr.processReadData(uie, store.sensorFrontExit, tagRead04);
        }

        tagRead01.last_read_on += TimeUnit.MINUTES.toMillis(15);
        tagRead02.last_read_on += TimeUnit.MINUTES.toMillis(15);
        tagRead03.last_read_on += TimeUnit.MINUTES.toMillis(15);
        tagRead04.last_read_on += TimeUnit.MINUTES.toMillis(15);

        tag01.setState(TagState.DEPARTED_EXIT, readTimeOrig);
        tag02.setState(TagState.DEPARTED_EXIT, readTimeOrig);
        tag03.setState(TagState.DEPARTED_EXIT, readTimeOrig);
        tag04.setState(TagState.DEPARTED_EXIT, readTimeOrig);
        uie = new UpstreamInventoryEventInfo();
        for (int x = 0; x < 20; x++) {
            invMgr.processReadData(uie, store.sensorFront02, tagRead01);
            invMgr.processReadData(uie, store.sensorBack03, tagRead02);
            invMgr.processReadData(uie, store.sensorFrontExit, tagRead03);
            invMgr.processReadData(uie, store.sensorFrontPOS, tagRead04);
        }
        assertThat(tag01.getState()).isEqualTo(TagState.PRESENT);
        assertThat(tag02.getState()).isEqualTo(TagState.PRESENT);
        assertThat(tag03.getState()).isEqualTo(TagState.EXITING);
        assertThat(tag04.getState()).isEqualTo(TagState.DEPARTED_EXIT);
        // even though there were 20 reads at the return location, it is not enough
        // to cause a location change in the tag due to the hysterisis. In real life,
        // a return will likely be several minutes later at least, but to keep the unit
        // tests running quickly, we don't exercise that much of a time gap.
        // assertThat(tag01.getLocation()).isEqualTo(store.sensorFront02.getDeviceId());

        assertThat(uie.data.size()).isGreaterThanOrEqualTo(3);
        item = uie.data.get(0);
        assertThat(item.event_type).isEqualTo(TagEvent.returned.toString());
        assertThat(tag01.getEPC()).isEqualTo(item.epc_code);
        assertThat(tag01.getLastArrived()).isEqualTo(tagRead01.last_read_on);

        item = uie.data.get(1);
        assertThat(tag02.getEPC()).isEqualTo(item.epc_code);
        assertThat(item.event_type).isEqualTo(TagEvent.returned.toString());

        item = uie.data.get(2);
        assertThat(tag03.getEPC()).isEqualTo(item.epc_code);
        assertThat(item.event_type).isEqualTo(TagEvent.returned.toString());

    }

    @Test
    public void testTagDepartAndReturnPOS() {

        long readTimeOrig = System.currentTimeMillis();
        TagRead tagRead01 = store.generateReadData(readTimeOrig);
        TagRead tagRead02 = store.generateReadData(readTimeOrig);
        TagRead tagRead03 = store.generateReadData(readTimeOrig);
        InventoryEventItem item;

        UpstreamInventoryEventInfo uie = new UpstreamInventoryEventInfo();
        invMgr.processReadData(uie, store.sensorBack01, tagRead01);
        invMgr.processReadData(uie, store.sensorBack01, tagRead02);
        invMgr.processReadData(uie, store.sensorBack01, tagRead03);

        Tag tag01 = invMgr.inventory.get(tagRead01.epc);
        Tag tag02 = invMgr.inventory.get(tagRead02.epc);

        // POS immediate departure
        uie = new UpstreamInventoryEventInfo();
        invMgr.processReadData(uie, store.sensorFrontPOS, tagRead01);
        invMgr.processReadData(uie, store.sensorFrontPOS, tagRead02);
        assertThat(tag01.getState()).isEqualTo(TagState.PRESENT);

        tagRead01.last_read_on = readTimeOrig + invMgr.getPOSDepartedThreshold() + 500;
        tagRead02.last_read_on = readTimeOrig + invMgr.getPOSDepartedThreshold() + 500;

        invMgr.processReadData(uie, store.sensorFrontPOS, tagRead01);
        invMgr.processReadData(uie, store.sensorFrontPOS, tagRead02);
        assertThat(uie.data.size()).isGreaterThanOrEqualTo(2);
        item = uie.data.get(0);
        assertThat(item.event_type).isEqualTo(TagEvent.departed.toString());
        item = uie.data.get(1);
        assertThat(item.event_type).isEqualTo(TagEvent.departed.toString());
        assertThat(tag01.getState()).isEqualTo(TagState.DEPARTED_POS);
        assertThat(tag02.getState()).isEqualTo(TagState.DEPARTED_POS);

        // and it should stay gone for a while
        tagRead01.last_read_on = readTimeOrig + invMgr.getPOSReturnThreshold() - 500;
        tagRead02.last_read_on = readTimeOrig + invMgr.getPOSReturnThreshold() - 500;

        uie = new UpstreamInventoryEventInfo();
        for (int x = 0; x < 20; x++) {
            invMgr.processReadData(uie, store.sensorFront02, tagRead01);
            invMgr.processReadData(uie, store.sensorFront02, tagRead02);
        }
        assertThat(tag01.getState()).isEqualTo(TagState.DEPARTED_POS);
        assertThat(tag02.getState()).isEqualTo(TagState.DEPARTED_POS);
        assertThat(uie.data.size()).isEqualTo(0);

        // but it should return from DEPARTED_POS
        tagRead01.last_read_on = readTimeOrig + (2 * invMgr.getPOSReturnThreshold()) + 500;
        tagRead02.last_read_on = readTimeOrig + (2 * invMgr.getPOSReturnThreshold()) + 500;

        uie = new UpstreamInventoryEventInfo();
        invMgr.processReadData(uie, store.sensorFront02, tagRead01);
        invMgr.processReadData(uie, store.sensorFrontPOS, tagRead02);
        assertThat(tag01.getState()).isEqualTo(TagState.PRESENT);
        assertThat(tag02.getState()).isEqualTo(TagState.DEPARTED_POS);
        assertThat(uie.data.size()).isGreaterThanOrEqualTo(1);
        item = uie.data.get(0);
        assertThat(item.event_type).isEqualTo(TagEvent.returned.toString());
    }

    @Test
    public void testAdjuster() {
        // check that default is asset trackingx`
        MobilityProfile mp = new MobilityProfile();
        assertTrue(mp.getM() < 0.0);
        assertThat(mp.getT()).isEqualTo(mp.getB());
    }

    @Test
    public void testRegEx() {
        String regex = "wanna*work";
        Pattern pattern = StringHelper.regexWildcard(regex);
        assertNotNull(pattern);
        String testWord;

        testWord = "wannawork";
        assertTrue(pattern.matcher(testWord).matches());
        testWord = "wannaAABBwork";
        assertTrue(pattern.matcher(testWord).matches());
        testWord = "wannaworkxyz";
        assertFalse(pattern.matcher(testWord).matches());
        testWord = "xyzwannawork";
        assertFalse(pattern.matcher(testWord).matches());

        regex = "wanna.*wo*rk";
        pattern = StringHelper.regexWildcard(regex);
        assertNotNull(pattern);

        testWord = "wannawork";
        assertTrue(pattern.matcher(testWord).matches());
        testWord = "wannaAABBwork";
        assertTrue(pattern.matcher(testWord).matches());
        testWord = "wannaworkxyz";
        assertFalse(pattern.matcher(testWord).matches());
        testWord = "xyzwannawork";
        assertFalse(pattern.matcher(testWord).matches());

    }

    @Test
    public void testStats() {

        double[] strongRSSI = {-88.0, -87.0, -80.0, -72.0, -65.0, -50.0};
        double[] fadeRSSI = {-49.0, -52.0, -55.0, -57.0, -60.0, -62.0};

        long now = System.currentTimeMillis();

        double[] basicX = {1, 2, 3, 4, 5, 6};

        //double[] times = new double[6];
        //for(int x = 0; x < 6; x++) {
        //    times[x] = now + basicX[x];
        //}
        double[] times = {
                (double) now,
                (double) now + 999,
                (double) now + 1999,
                (double) now + 1599,
                (double) now + 3599,
                (double) now + 3799};

        SimpleRegression r1 = new SimpleRegression();
        SimpleRegression r2 = new SimpleRegression();
        SimpleRegression r3 = new SimpleRegression();
        SimpleRegression r4 = new SimpleRegression();

        for (int x = 0; x < 6; x++) {
            r1.addData(basicX[x], strongRSSI[x]);
            r2.addData(basicX[x], fadeRSSI[x]);
            r3.addData(times[x], strongRSSI[x]);
            r4.addData(times[x], fadeRSSI[x]);
        }

        printReg(r1);
        printReg(r2);
        printReg(r3);
        printReg(r4);
    }

    private void printReg(SimpleRegression _r) {
        System.out.println(
                String.format("%d, %f, %f, %f",
                              _r.getN(),
                              _r.getSlope(),
                              _r.getSlopeStdErr(),
                              _r.getSlopeConfidenceInterval())
        );
    }

    @Test
    public void testAgeInventories() {

        assertNotNull(store.sensorFront03);

        long ageout = ConfigManager.instance.getOptLong(CFG_KEY_AGEOUT, DEFAULT_AGEOUT_HOURS);
        long expiration = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(ageout);

        InventoryDataNotification readData = EpcHelper.generateBeforeAfterTime(expiration);
        invMgr.onInventoryData(readData, store.sensorFront03);

        // check that all inventory is there
        Collection<Tag> tags = invMgr.getTags(null);

        assertThat(tags).hasSameSizeAs(readData.params.data);

        invMgr.ageout();

        tags = invMgr.getTags(null);
        assertThat(tags).hasSize(2)
                        .noneMatch(_tag -> _tag.getEPC().equals(EpcHelper.EPC_HOUR_BEFORE))
                        .noneMatch(_tag -> _tag.getEPC().equals(EpcHelper.EPC_DAY_BEFORE));

    }

}
