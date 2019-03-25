package com.intel.rfid.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.helpers.Jackson;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class InventoryTest {

    public static class TestValues {
        public double value;
    }

    @Test
    public void testDoubleParse() throws IOException {
        
        TestValues tv = new TestValues();
        tv.value = 23.456;
        ObjectMapper mapper = Jackson.getMapper();
        String s = mapper.writeValueAsString(tv);
        TestValues tv2 = mapper.readValue(s, TestValues.class);
        
        assertThat(tv.value).isEqualTo(tv2.value);
    }

}
