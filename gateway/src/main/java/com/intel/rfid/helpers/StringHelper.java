/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.helpers;

import com.intel.rfid.api.Behavior;
import com.intel.rfid.sensor.SensorPlatform;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class StringHelper {

    public static Pattern regexWildcard(String _regex) {
        if (_regex == null || _regex.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        char prevChar = 0;

        for (char c : _regex.toCharArray()) {
            if (c == '*' && prevChar != '.') {
                sb.append('.');
            }
            sb.append(c);
            prevChar = c;
        }

        Pattern p;
        try {
            p = Pattern.compile(sb.toString());
        } catch (PatternSyntaxException e) {
            p = null;
        }

        return p;
    }

    public static String getBehaviorIds(Collection<Behavior> _behaviors) {
        Set<String> ids = new TreeSet<>();
        for (Behavior b : _behaviors) {
            ids.add(b.id);
        }
        return Arrays.toString(ids.toArray());
    }

    public static String getDeviceIds(Collection<SensorPlatform> _rsps) {
        Set<String> ids = new TreeSet<>();
        for (SensorPlatform rsp : _rsps) {
            ids.add(rsp.getDeviceId());
        }
        return Arrays.toString(ids.toArray());
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isNullOrWhitespace(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean startsWithAny(String value, String... values) {
        for (String s : values) {
            if (value.startsWith(s)) {
                return true;
            }
        }
        return false;
    }

    public static String ensureEndsWith(String source, String suffix) {
        return source.endsWith(suffix) ? source : source + suffix;
    }

    /**
     * Remove all non word characters from a string to prevent log forging
     *
     * @param errorMessage
     * @return
     */
    public static String sanitizeErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return errorMessage;
        }
        return errorMessage.replaceAll("[^\\w ]+", " ").replaceAll(" {2,}", " ").trim();
    }

}
