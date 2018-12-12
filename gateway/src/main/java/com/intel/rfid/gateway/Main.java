/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.gateway;

import com.intel.rfid.console.ConsoleAuthenticator;
import com.intel.rfid.console.ConsoleShellFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.apache.sshd.common.config.keys.KeyUtils.RSA_ALGORITHM;

public class Main {

    protected static final Logger log = LoggerFactory.getLogger(Gateway.class);

    static volatile boolean keepGoing = true;

    public static void main(String[] _args) {
        log.info("--");
        log.info("-- RFID Gateway ");
        log.info("-- " + Version.asString());
        log.info("--");
        log.info("-- Starting gateway services...");

        System.out.println("--");
        System.out.println("-- RFID Gateway ");
        System.out.println("-- " + Version.asString());
        System.out.println("--");
        System.out.print("-- Starting gateway services");
        TimeIndicator ti = new TimeIndicator();
        ti.start();
        Gateway gateway = Gateway.build();
        gateway.start();
        ti.stop();
        System.out.println("--");

        SshServer sshd = SshServer.setUpDefaultServer();
        try {

            File keyFile = new File(Env.resolveCache("consolekey.ser").toAbsolutePath().toString());
            sshd.setPort(ConfigManager.instance.getConsolePort());
            AbstractGeneratorHostKeyProvider hkp = new SimpleGeneratorHostKeyProvider(keyFile);
            hkp.setAlgorithm(RSA_ALGORITHM);
            sshd.setKeyPairProvider(hkp);
            sshd.setPasswordAuthenticator(new ConsoleAuthenticator());
            sshd.setShellFactory(new ConsoleShellFactory(gateway));
            sshd.start();
        } catch (Exception e) {
            log.error("Problem starting sshd for remote console acccess:", e);
        }

        keepGoing = true;
        final Thread mainThread = Thread.currentThread();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                keepGoing = false;
                mainThread.interrupt();
                // wait up to 2 minutes before giving up totally
                mainThread.join(TimeUnit.MINUTES.toMillis(2));
            } catch (InterruptedException e) {
                System.err.println("Timed out before shutdown completed successfully");
            }
        }));

        while (keepGoing) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // don't reset the interrupted status so
                // shutdown can continue in an orderly fashion
                keepGoing = false;
                log.info("Main thread interrupted, keepGoing: {}", keepGoing);
            }
        }

        System.out.print("-- Stopping gateway services");
        log.info("-- Stopping gateway services");
        ti.start();

        try {
            sshd.stop();
            log.info("sshd stopped");
        } catch (IOException e) {
            log.error("Problem stopping the SSHD server:", e);
        }

        gateway.stop();

        ti.stop();
        System.out.println("-- goodbye!");

    }


    private static class TimeIndicator implements Runnable {

        Thread t;

        public void start() {
            t = new Thread(this);
            t.start();
        }

        public void stop() {
            if (t != null) {
                t.interrupt();
                try {
                    t.join();
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                }
            }
        }

        public void run() {

            while (true) {
                try {
                    System.out.print(".");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            System.out.println();
        }
    }

}
