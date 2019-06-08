package com.intel.rfid.upstream;

public class MockMqttUpstream extends MqttUpstream {

    public MockMqttUpstream(Dispatch _dispatch) {
        super(_dispatch);
    }

    public void publish(String _topic, byte[] _msg, QOS _qos) {
        log.debug("{} published {}", getClass().getSimpleName(), _topic);
    }
}
