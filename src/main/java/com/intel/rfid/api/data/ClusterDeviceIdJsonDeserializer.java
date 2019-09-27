package com.intel.rfid.api.data;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.intel.rfid.helpers.StringHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClusterDeviceIdJsonDeserializer extends JsonDeserializer<List<List<String>>> {

    @Override
    public List<List<String>> deserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        
        ObjectCodec oc = jp.getCodec();
        JsonNode jsonNode = oc.readTree(jp);

        List<List<String>> groups = new ArrayList<>();
        if (jsonNode.isArray()) {
            for (JsonNode groupNode : jsonNode) {
                List<String> group = new ArrayList<>();
                groups.add(group);
                if (groupNode.isArray()) {
                    for (JsonNode sensorIdNode : groupNode) {
                        String deviceId = sensorIdNode.asText();
                        if(!deviceId.isEmpty()) {
                            group.add(StringHelper.convertCaseRSPId(deviceId));
                        }
                    }
                }
            }
        }
        return groups;
    }

}
