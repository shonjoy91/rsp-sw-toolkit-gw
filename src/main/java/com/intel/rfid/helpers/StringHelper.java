/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.helpers;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class StringHelper {

    public static String convertCaseRSPId(String _deviceId) {
        String id = _deviceId;
        int index = _deviceId.indexOf("RSP-");
        if(index >= 0) {
            id = _deviceId.substring(0, (index + 4)) + _deviceId.substring(index + 4).toLowerCase();
        }
        return id;
    }

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

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

}
