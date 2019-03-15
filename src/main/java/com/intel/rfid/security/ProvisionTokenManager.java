/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.ProvisionToken;
import com.intel.rfid.gateway.Env;
import com.intel.rfid.helpers.Jackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class ProvisionTokenManager {

    protected Logger log = LoggerFactory.getLogger(getClass());
    protected ObjectMapper mapper = Jackson.getMapper();

    public static final long DEFAULT_VAILIDITY_MILLIS = TimeUnit.HOURS.toMillis(24);
    public static final long INFINITE_VALIDITY = -1;

    public static final Path CACHE_PATH = Env.getCachePath().resolve("provision_tokens.json");
    protected static final Map<String, ProvisionToken> tokens = new HashMap<>();

    ProvisionTokenManager() {
        restore();
    }

    public ProvisionToken generate(long _validityDurationMillis) {

        // generate a random series of hex characters
        int numOfChars = 64;
        SecureRandom random = new SecureRandom();
        StringBuilder tokenBuf = new StringBuilder();
        while (tokenBuf.length() < numOfChars) {
            tokenBuf.append(Integer.toHexString(random.nextInt()));
        }
        String token = tokenBuf.toString().substring(0, numOfChars).toUpperCase();

        // get the duration set up
        long created = System.currentTimeMillis();
        long expires;

        if (_validityDurationMillis > 0) {
            expires = created + _validityDurationMillis;
        } else {
            expires = -1;
        }

        if (expires < 0) { expires = INFINITE_VALIDITY; }

        ProvisionToken pt = new ProvisionToken(System.getProperty("user.name"), token);
        pt.generatedTimestamp = created;
        pt.expirationTimestamp = expires;

        synchronized (tokens) {
            tokens.put(pt.token, pt);
        }
        persist();
        return pt;
    }

    public boolean removeToken(String _token) {
        ProvisionToken pt;
        synchronized (tokens) {
            pt = tokens.remove(_token);
        }
        if (pt != null) {
            persist();
            log.info("removed token entry: {}", pt);
            return true;
        } else {
            log.info("no token matched");
            return false;
        }
    }

    public boolean isTokenValid(String _token) {
        boolean valid = false;
        ProvisionToken pt;
        synchronized (tokens) {
            pt = tokens.get(_token);
        }
        if (pt != null) {
            if (isExpired(pt)) {
                synchronized (tokens) {
                    tokens.remove(_token);
                }
                persist();
            } else {
                valid = true;
            }
        }
        return valid;
    }

    protected Collection<String> getTokensOnly() {
        TreeSet<String> copy = new TreeSet<>();
        synchronized (tokens) {
            copy.addAll(tokens.keySet());
        }
        return copy;
    }

    protected Collection<ProvisionToken> getAll() {
        TreeSet<ProvisionToken> copy = new TreeSet<>(new ExpiredSorter());
        synchronized (tokens) {
            copy.addAll(tokens.values());
        }
        return copy;
    }

    public static class ExpiredSorter implements Comparator<ProvisionToken> {

        @Override
        public int compare(ProvisionToken _pt1, ProvisionToken _pt2) {
            // put _pt2 first so latest comes first
            return Long.compare(_pt2.expirationTimestamp, _pt1.expirationTimestamp);
        }
    }

    public boolean isExpired(ProvisionToken _pt) {
        return _pt.expirationTimestamp != INFINITE_VALIDITY &&
               _pt.expirationTimestamp > 0 &&
               _pt.expirationTimestamp < System.currentTimeMillis();
    }


    public static class FileCache {
        // since a tag is not just in a single facility at a time, a simple list
        // will be used going forward.
        public List<ProvisionToken> entries = new ArrayList<>();
    }

    protected void restore() {

        if (!Files.exists(CACHE_PATH)) { return; }

        FileCache fileCache = null;

        try (InputStream fis = Files.newInputStream(CACHE_PATH)) {
            fileCache = mapper.readValue(fis, FileCache.class);
        } catch (IOException e) {
            log.error("Failed to restore {}", CACHE_PATH, e);
        }

        if (fileCache == null) { return; }

        synchronized (tokens) {
            for (ProvisionToken cachedToken : fileCache.entries) {
                if (!isExpired(cachedToken)) {
                    tokens.put(cachedToken.token, cachedToken);
                }
            }
        }
    }

    protected void persist() {


        synchronized (tokens) {
            FileCache fileCache = new FileCache();
            fileCache.entries.addAll(tokens.values());
            try (OutputStream os = Files.newOutputStream(CACHE_PATH)) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(os, fileCache);
                log.info("wrote {}", CACHE_PATH);
            } catch (IOException e) {
                log.error("failed persisting tokens {}", e.getMessage());
            }
        }
    }
}
