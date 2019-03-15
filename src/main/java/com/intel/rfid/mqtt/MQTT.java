/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.mqtt;

import com.intel.rfid.exception.FailedException;
import com.intel.rfid.exception.GatewayException;
import com.intel.rfid.exception.NotConnectedException;
import com.intel.rfid.gateway.ConfigManager;
import com.intel.rfid.helpers.DateTimeHelper;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.security.SecurityContext;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class MQTT implements MqttCallback {

    public enum QOS {
        AT_MOST_ONCE(0),
        AT_LEAST_ONCE(1),
        EXACTLY_ONCE(2);

        public final int value;

        QOS(int _val) {
            value = _val;
        }
    }

    protected Logger log = LoggerFactory.getLogger(getClass());

    public static final QOS DEFAULT_QOS = QOS.AT_LEAST_ONCE;


    public enum RunState {DISCONNECTED, CONNECTED}

    protected String brokerURI;

    protected ConfigManager.Credentials credentials;
    protected String clientId;
    protected MqttAsyncClient client;
    protected final Object clientLock = new Object();

    protected List<String> subscriptions = new ArrayList<>();
    protected final Object subscriptionLock = new Object();

    protected ExecutorService exec;
    protected Timer connectTimer;

    public MQTT() {
        // generate a unique client id
        clientId = getClass().getSimpleName() +
                   DateTimeHelper.toFilelNameLocal(new Date());

    }

    public void start() {
        exec = Executors.newSingleThreadExecutor();
        exec.submit(getOutboundTask());
        startTimer();
        log.info(getClass().getSimpleName() + " started");
    }

    public void stop() {

        stopTimer();
        try {
            exec.shutdown();
            if (!exec.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                exec.shutdownNow();
                if (!exec.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                    log.error("timeout waiting for executor to finish");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("interrupted waiting for executor to shut down");
        }

        synchronized (clientLock) {
            if (client != null) {
                try {
                    if (runState == RunState.CONNECTED) {

                        for (String topic : subscriptions) {
                            try {
                                client.unsubscribe(topic).waitForCompletion();
                            } catch (MqttException e) {
                                log.error("mqtt -- error unsubscribing {} : {}", topic, unroll(e));
                            }
                        }

                        client.disconnect(5000).waitForCompletion();
                    }
                    client.close();
                    log.info("MQTT client disconnected and closed");
                } catch (MqttException e) {
                    log.warn("mqtt -- error stopping : {}", unroll(e));
                }
                client = null;
            }
        }
        log.info(getClass().getSimpleName() + " stopped");
    }

    public void status(PrettyPrinter _out) {
        _out.line(runState.toString().toLowerCase() + " " + brokerURI);
        synchronized (subscriptionLock) {
            for (String topic : subscriptions) {
                _out.line("sub: " + topic);
            }
        }
    }

    protected void startTimer() {

        if (connectTimer != null) { return; }

        connectTimer = new Timer();
        // task to run every 10 seconds
        connectTimer.schedule(
            new TimerTask() {
                public void run() {
                    connect();
                }
            },
            1000,
            1000 * 10);
    }

    protected void stopTimer() {
        if (connectTimer != null) {
            connectTimer.cancel();
            connectTimer = null;
        }
    }

    protected RunState runState = RunState.DISCONNECTED;

    public boolean isConnected() { return runState == RunState.CONNECTED; }

    protected void setLastWill(MqttConnectOptions _options) { }

    // subclass callback for state changes
    protected void onConnect() {
        log.info("{} connected {}", getClass().getSimpleName(), brokerURI);
    }

    protected void connect() {

        try {
            MqttConnectOptions options = new MqttConnectOptions();
            if (credentials.userId != null && !credentials.userId.isEmpty()) {
                log.info("setting {} userName: {}", getClass().getSimpleName(), credentials.userId);
                options.setUserName(credentials.userId);
            }
            if (credentials.password != null && !credentials.password.isEmpty()) {
                options.setPassword(credentials.password.toCharArray());
            }
            options.setCleanSession(true);

            setLastWill(options);

            if (brokerURI.startsWith("ssl")) {
                SocketFactory factory = SecurityContext.instance().getSecureSocketFactory();
                if (factory != null) {
                    options.setSocketFactory(factory);
                }
                // null / errors are logged already
            }

            log.info("Attempting to connect to mqtt broker {} with clientId {}...", brokerURI, clientId);

            synchronized (clientLock) {
                client = new MqttAsyncClient(brokerURI, clientId, new MemoryPersistence());
                client.setCallback(this);
                client.connect(options).waitForCompletion();
                runState = RunState.CONNECTED;
            }

            synchronized (subscriptionLock) {
                for (String topic : subscriptions) {
                    subscribeToClient(topic);
                }
            }

            stopTimer();

            onConnect();

        } catch (MqttException e) {
            log.error("mqtt -- error connecting : {}", unroll(e));
        }

    }

    protected void subscribe(String _topic) {
        synchronized (subscriptionLock) {
            subscriptions.add(_topic);
            if (runState.equals(RunState.CONNECTED)) {
                subscribeToClient(_topic);
            }
        }
    }

    protected void subscribeToClient(String _topic) {
        try {
            synchronized (clientLock) {
                client.subscribe(_topic, DEFAULT_QOS.value).waitForCompletion(500);
                log.info("Subscribed to[{}]", _topic);
            }
        } catch (MqttException e) {
            // any subscription errors are treated as failures
            log.error("mqtt -- error subscribing {} : {}", unroll(e));
        }
    }

    public static class Outbound {

        public Outbound(String _topic, MqttMessage _mqttMessage) {
            topic = _topic;
            mqttMessage = _mqttMessage;
        }

        public String topic;
        public MqttMessage mqttMessage;
    }


    protected LinkedBlockingQueue<Outbound> outboundQueue = new LinkedBlockingQueue<>(100);

    protected boolean loggedOnce = false;

    public void publish(String _topic, byte[] _msg, QOS _qos) throws GatewayException {

        if (runState != RunState.CONNECTED) {
            throw new NotConnectedException("mqtt " + clientId);
        }
        MqttMessage mmsg = new MqttMessage(_msg);
        mmsg.setQos(_qos.value);
        Outbound outbound = new Outbound(_topic, mmsg);

        if (!outboundQueue.offer(outbound)) {
            if (!loggedOnce) {
                log.warn("outbound queue is full. " +
                         "messages will be dropped until space is available");
                loggedOnce = true;
            }
            throw new FailedException("mqtt outbound queue is full");
        }
        // clear the state to re-enable the message
        loggedOnce = false;
    }


    protected Runnable getOutboundTask() {
        return new OutboundAsyncClientPublisherTask();
    }

    protected class OutboundAsyncClientPublisherTask implements Runnable {

        public void run() {

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Outbound outbound = outboundQueue.take();

                    synchronized (clientLock) {

                        // this is a just in case
                        if (client == null) {
                            continue;
                        }

                        try {
                            client.publish(outbound.topic, outbound.mqttMessage).waitForCompletion(1000);
                        } catch (Exception e) {

                            Throwable t = e;
                            while (t != null) {
                                if (t instanceof InterruptedException) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                                t = t.getCause();
                            }
                            log.error("Error publishing to topic {}", outbound.topic, e);
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            // need to drain the queue
            outboundQueue.clear();
        }
    }


    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        log.info("messageArrived topic: {} msg: {}", topic, message.toString());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.debug("delivery complete for message id {} topics {}",
                  token.getMessageId(), token.getTopics());
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("mqtt -- lost connection to broker {} due to: {}", brokerURI, cause.getMessage());
        runState = RunState.DISCONNECTED;
        startTimer();
    }

    public static synchronized String unroll(MqttException _e) {
        // MQTT paho client is pretty bad for just wrapping other exceptions without
        // providing much explanation
        StringBuilder sb = new StringBuilder();
        sb.append(_e.getMessage());
        Throwable t = _e;
        while (t.getCause() != null) {
            sb.append(" caused by ");
            sb.append(t.getCause().getMessage());
            t = t.getCause();
        }
        return sb.toString();
    }
}
