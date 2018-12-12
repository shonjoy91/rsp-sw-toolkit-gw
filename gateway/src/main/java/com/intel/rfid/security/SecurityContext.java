/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.security;

import com.intel.rfid.exception.ConfigException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;

public class SecurityContext {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private static SecurityContext instance;

    public static SecurityContext instance() {
        if (instance == null) {
            instance = new SecurityContext();
        }
        return instance;
    }

    private KeyManager keyMgr;
    private ProvisionTokenManager provisionTokenMgr;
    private BouncyCastleProvider bouncyCastleProvider;

    private SecurityContext() {
        Security.addProvider(getBouncyCastleProvider());
    }

    public KeyManager getKeyMgr() {
        if (keyMgr == null) { keyMgr = new KeyManager(); }
        return keyMgr;
    }

    public ProvisionTokenManager getProvisionTokenMgr() {
        if (provisionTokenMgr == null) { provisionTokenMgr = new ProvisionTokenManager(); }
        return provisionTokenMgr;
    }

    public BouncyCastleProvider getBouncyCastleProvider() {
        if (bouncyCastleProvider == null) { bouncyCastleProvider = new BouncyCastleProvider(); }
        return bouncyCastleProvider;
    }

    public SSLSocketFactory getSecureSocketFactory() {
        SSLSocketFactory factory = null;
        try {

            // CA certificate is used to authenticate server
            KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
            caKs.load(null, null);
            caKs.setCertificateEntry("ca-certificate", getKeyMgr().getCACertificate());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(caKs);

            // create SSL socket factory
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(null, tmf.getTrustManagers(), null);

            factory = context.getSocketFactory();
        } catch (GeneralSecurityException | IOException _e) {
            log.error("error creating secure socket factory: {}", _e);
        }

        return factory;
    }

    public static final String[] ALLOWED_CIPHERS = {
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
        "TLS_RSA_WITH_AES_256_CBC_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_RSA_WITH_AES_128_CBC_SHA256"
    };

    public SslContextFactory getJettySslContextFactory() throws ConfigException {
        SslContextFactory factory = new SslContextFactory();

        KeyManager km = getKeyMgr();

        factory.setKeyStoreType(km.getKeystoreType());
        factory.setKeyStorePath(km.getKeystoryPath());
        factory.setKeyStorePassword(km.getKeystorePassword());
        factory.setIncludeCipherSuites(ALLOWED_CIPHERS);

        if (log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder();
            for (String s : factory.getExcludeCipherSuites()) {
                sb.append(s).append('\n');
            }
            log.info("excluded cipher suites:\n{}", sb.toString());
            sb = new StringBuilder();
            for (String s : factory.getIncludeCipherSuites()) {
                sb.append(s).append('\n');
            }
            log.info("included cipher suites:\n{}", sb.toString());
            sb = new StringBuilder();
            for (String s : factory.getExcludeProtocols()) {
                sb.append(s).append('\n');
            }
            log.info("excluded protocols:\n{}", sb.toString());
            sb = new StringBuilder();
            for (String s : factory.getIncludeProtocols()) {
                sb.append(s).append('\n');
            }
            log.info("included protocols:\n{}", sb.toString());
        }

        return factory;
    }


}
