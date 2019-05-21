/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.security;

import com.intel.rfid.exception.ConfigException;
import com.intel.rfid.gateway.ConfigManager;
import com.intel.rfid.gateway.Env;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class KeyManager {

    protected Logger log = LoggerFactory.getLogger(getClass());

    public final Path CA_CERT_PATH = Env.resolveCache("ca.crt");
    public final Path SERVER_CERT_PATH = Env.resolveCache("server.crt");

    protected String getKeystoreType() {
        return "PKCS12";
    }

    protected String getKeystoryPath() throws ConfigException {
        // verify that this path has contents
        Path keystorePath = Env.resolveCache("keystore.p12");
        if (!Files.exists(keystorePath) || !Files.isReadable(keystorePath)) {
            throw new ConfigException("unavailable keystore file " + keystorePath);
        }
        return keystorePath.toString();
    }

    protected String getKeystorePassword() throws ConfigException {
        return ConfigManager.instance.getReqString("provision.keystore.password");
    }

    public PublicKey getServerPublicKey() {
        PublicKey publicKey = null;
        X509Certificate cert = getServerCertificate();
        if (cert != null) {
            publicKey = cert.getPublicKey();
        }
        if (publicKey == null) {
            log.info("Public key not found in server certificate");
        }
        return publicKey;
    }

    /**
     * The sensor has a security path which supports changing the single SSH key used
     * for authenticating connections. The sensor will not update the key if it is NULL
     * or empty. That is why this method returns the empty string by default.
     * <p>
     * Otherwise, returns the server's public key in the ssh-rsa encoding, as defined by RFC4253,
     * which specifies the key as:
     * string "ssh-rsa"
     * mpint e
     * mpint n
     * where 'e' and 'n' form the signature key blob, emitted as an mpint, a multiple precision integer in two's
     * complement, stored as a string, 8 bits per byte, MSB first, as defined by RFC4251.
     *
     * @return NULL by default, otherwise the server's public key
     */
    public String getSshEncodedPublicKey() throws IOException {
        // NOTE: Enabling this code to return this string will result
        // in the sensor changing its ssh access key
        // try {
        //     RSAPublicKey key = (RSAPublicKey) getServerPublicKey();
        //     if (key == null) { return null; }
        //     ByteArrayOutputStream buf = new ByteArrayOutputStream();
        //     writeSshString("ssh-rsa".getBytes("US-ASCII"), buf);
        //     writeSshString(key.getPublicExponent().toByteArray(), buf);
        //     writeSshString(key.getModulus().toByteArray(), buf);
        //     log.info("done generating ssh-rsa public key encoding, but returning null");
        //     return Base64.toBase64String(buf.toByteArray());
        // } catch (ClassCastException e) {
        //     log.error("couldn't generate ssh-rsa public key: certificate public key is not an RSAPublicKey");
        // }
        return null;
    }

    /**
     * Writes a string according to RFC4251, SSH Protocol Architecture.
     * <p>
     * Strings are stored as a uint32 containing its length (number of bytes to follow) and zero (= empty string) or
     * more bytes that are the value of the string. Terminating null characters are not used. Note that US-ASCII should
     * be used for internal names while ISO-10646 UTF-8 should be used for text displayed to the user.
     *
     * @param str a string, represented as an array of bytes, that shall be written int the SSH encoding
     * @param os  an output stream to which the SSH encoded string shall be written
     * @throws IOException if writing to the output stream fails
     */
    private void writeSshString(byte[] str, OutputStream os) throws IOException {
        for (int shift = 24; shift >= 0; shift -= 8) {
            os.write((str.length >>> shift) & 0xFF);
        }
        os.write(str);
    }

    public X509Certificate getCACertificate() {
        String pem = getCACertificateAsPem();
        if (pem == null) { return null; }
        try {
            return pemToX509Cert(pem);
        } catch (IOException | CertificateException e) {
            log.error("unable to retrieve root certificate: ", e);
            return null;
        }
    }

    public String getCACertificateAsPem() {
        try {
            return new String(Files.readAllBytes(Paths.get(CA_CERT_PATH.toString())), StandardCharsets.UTF_8);
        } catch (IOException | UnsupportedOperationException | ClassCastException e) {
            log.error("unable to retrieve root certificate: ", e);
            return null;
        }
    }

    public X509Certificate getServerCertificate() {
        String pem = getServerCertificateAsPem();
        if (pem == null) { return null; }
        try {
            return pemToX509Cert(pem);
        } catch (IOException | CertificateException e) {
            log.error("unable to retrieve root certificate: ", e);
            return null;
        }
    }

    public String getServerCertificateAsPem() {
        try {
            return new String(Files.readAllBytes(Paths.get(SERVER_CERT_PATH.toString())), StandardCharsets.UTF_8);
        } catch (IOException | UnsupportedOperationException | ClassCastException e) {
            log.error("unable to retrieve root certificate: ", e);
            return null;
        }
    }

    public static X509Certificate pemToX509Cert(String _pem) throws CertificateException, IOException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert;
        try (InputStream stream = new ByteArrayInputStream(_pem.getBytes(StandardCharsets.UTF_8))) {
            cert = (X509Certificate) factory.generateCertificate(stream);
        }
        return cert;
    }

}
