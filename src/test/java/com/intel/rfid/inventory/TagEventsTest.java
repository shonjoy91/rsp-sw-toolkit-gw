package com.intel.rfid.inventory;

import com.intel.rfid.api.data.InventoryEventItem;
import com.intel.rfid.tag.Tag;
import com.intel.rfid.tag.TagEvent;
import com.intel.rfid.tag.TagState;
import com.intel.rfid.api.sensor.TagRead;
import com.intel.rfid.gateway.MockGateway;
import com.intel.rfid.helpers.EnvHelper;
import com.intel.rfid.helpers.TestStore;
import com.intel.rfid.upstream.UpstreamInventoryEventInfo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TagEventsTest implements InventoryManager.UpstreamEventListener {

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



    public TagEventsTest() {
        store = new TestStore();
        gateway = new MockGateway();
        invMgr = gateway.getMockInventoryManager();
        invMgr.unload();
        invMgr.addUpstreamEventListener(this);
    }

    // TODO: additional test cases that exercise varying mobility profiles
    // along with times and signal strength
    
    @Test
    public void testElevatorFacilityMovement() {

        InventoryEventItem item;
        UpstreamInventoryEventInfo uie;

        TagRead tagRead01 = store.generateReadData(store.time_m00);
        TagRead tagRead02 = store.generateReadData(store.time_m00);
        TagRead tagRead03 = store.generateReadData(store.time_m00);
        TagRead tagRead04 = store.generateReadData(store.time_m00);

        // single read will go UNKOWN -> PRESENT even for an exit sensor
        uie = new UpstreamInventoryEventInfo();
        invMgr.processReadData(uie, store.sensorCexit01, tagRead01);
        invMgr.processReadData(uie, store.sensorCexit01, tagRead02);
        invMgr.processReadData(uie, store.sensorCexit02, tagRead03);
        invMgr.processReadData(uie, store.sensorCexit02, tagRead04);

        Tag tag01 = invMgr.inventory.get(tagRead01.epc);
        Tag tag02 = invMgr.inventory.get(tagRead02.epc);
        Tag tag03 = invMgr.inventory.get(tagRead03.epc);
        Tag tag04 = invMgr.inventory.get(tagRead04.epc);


        assertThat(tag01).isNotNull();
        assertThat(tag01.getState()).isEqualTo(TagState.PRESENT);
        assertThat(tag01.getLocation()).isEqualTo(store.sensorCexit01.asLocation());

        assertThat(tag02.getState()).isEqualTo(TagState.PRESENT);
        assertThat(tag03.getState()).isEqualTo(TagState.PRESENT);
        assertThat(tag04.getState()).isEqualTo(TagState.PRESENT);

        item = uie.data.get(0);
        assertThat(item.event_type).isEqualTo(TagEvent.arrival.toString());
        assertThat(item.location).isEqualTo(store.sensorCexit01.asLocation());
        assertThat(item.facility_id).isEqualTo(store.sensorCexit01.getFacilityId());

        item = uie.data.get(1);
        assertThat(item.event_type).isEqualTo(TagEvent.arrival.toString());
        item = uie.data.get(2);
        assertThat(item.event_type).isEqualTo(TagEvent.arrival.toString());
        item = uie.data.get(3);
        assertThat(item.event_type).isEqualTo(TagEvent.arrival.toString());

        // one more read will go PRESENT -> EXITING 
        uie = new UpstreamInventoryEventInfo();
        invMgr.processReadData(uie, store.sensorCexit01, tagRead01);
        invMgr.processReadData(uie, store.sensorCexit01, tagRead02);
        invMgr.processReadData(uie, store.sensorCexit02, tagRead03);
        invMgr.processReadData(uie, store.sensorCexit02, tagRead04);

        assertThat(tag01.getState()).isEqualTo(TagState.EXITING);
        assertThat(tag02.getState()).isEqualTo(TagState.EXITING);
        assertThat(tag03.getState()).isEqualTo(TagState.EXITING);
        assertThat(tag04.getState()).isEqualTo(TagState.EXITING);

        // move a tag between exiting sensors
        tagRead02.last_read_on = store.time_m01;
        uie = new UpstreamInventoryEventInfo();
        for (int x = 0; x < 20; x++) {
            invMgr.processReadData(uie, store.sensorCexit02, tagRead02);
        }

        item = uie.data.get(0);
        assertThat(item.event_type).isEqualTo(TagEvent.moved.toString());
        assertThat(item.location).isEqualTo(store.sensorCexit02.asLocation());


        // walk 3 tags from C to A and B
        tagRead01.rssi = store.rssiWeak();
        tagRead02.rssi = store.rssiWeak();
        tagRead03.rssi = store.rssiWeak();
        tagRead01.last_read_on = store.time_m03;
        tagRead02.last_read_on = store.time_m03;
        tagRead03.last_read_on = store.time_m03;
        uie = new UpstreamInventoryEventInfo();
        for (int x = 0; x < 20; x++) {
            invMgr.processReadData(uie, store.sensorA01, tagRead01);
            invMgr.processReadData(uie, store.sensorA01, tagRead02);
            invMgr.processReadData(uie, store.sensorB01, tagRead03);
        }

        assertThat(tag01.getState()).isEqualTo(TagState.PRESENT);
        assertThat(tag02.getState()).isEqualTo(TagState.PRESENT);
        assertThat(tag03.getState()).isEqualTo(TagState.PRESENT);
        assertThat(tag04.getState()).isEqualTo(TagState.EXITING);

        assertThat(tag01.getLocation()).isEqualTo(store.sensorA01.asLocation());
        assertThat(tag02.getLocation()).isEqualTo(store.sensorA01.asLocation());
        assertThat(tag03.getLocation()).isEqualTo(store.sensorB01.asLocation());
        assertThat(tag04.getLocation()).isEqualTo(store.sensorCexit02.asLocation());
        
        // departure / arrival pairs
        item = uie.data.get(0);
        assertThat(item.event_type).isEqualTo(TagEvent.departed.toString());
        assertThat(item.location).isEqualTo(store.sensorCexit01.asLocation());
        assertThat(item.facility_id).isEqualTo(store.sensorCexit01.getFacilityId());

        item = uie.data.get(1);
        assertThat(item.event_type).isEqualTo(TagEvent.arrival.toString());
        assertThat(item.location).isEqualTo(store.sensorA01.asLocation());
        assertThat(item.facility_id).isEqualTo(store.sensorA01.getFacilityId());

        item = uie.data.get(4);
        assertThat(item.event_type).isEqualTo(TagEvent.departed.toString());
        assertThat(item.location).isEqualTo(store.sensorCexit02.asLocation());
        assertThat(item.facility_id).isEqualTo(store.sensorCexit02.getFacilityId());

        item = uie.data.get(5);
        assertThat(item.event_type).isEqualTo(TagEvent.arrival.toString());
        assertThat(item.location).isEqualTo(store.sensorB01.asLocation());
        assertThat(item.facility_id).isEqualTo(store.sensorB01.getFacilityId());

        // tag 4 should depart
        upstreamEvents.clear();
        invMgr.doAggregateDepartedTask();

        assertThat(tag04.getState()).isEqualTo(TagState.DEPARTED_EXIT);
        assertThat(upstreamEvents).hasSize(1);
        assertThat(upstreamEvents.get(0).data).hasSize(1);
        item = upstreamEvents.get(0).data.get(0);
        assertThat(item.event_type).isEqualTo(TagEvent.departed.toString());
        assertThat(item.location).isEqualTo(store.sensorCexit02.asLocation());

    }


}
