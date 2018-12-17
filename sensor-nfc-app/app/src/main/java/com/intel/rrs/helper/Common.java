/*
 * Project: RRS NFC App
 * Module: Common
 *
 * Copyright (C) 2018 Intel Corporation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of Intel Corporation nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.intel.rrs.helper;

import android.os.Environment;

import com.intel.rrs.data.ProvisioningToken;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class Common {

    public static final String SHARED_PREFS_FILE = Common.class.getName() + ".shared.preferences";

    public static final String KEY_TOKEN_FILENAME = "token_filename";
    public static final String KEY_ROOT_CA_CERT_FILENAME = "root_ca_cert_filenname";

    public static File getDownloadsDir() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    public static void getTokenFiles(List<File> _files, List<String> _names) {
        for (File file : Common.getDownloadsDir().listFiles()) {
            String name = file.toString();
            if (name.contains("token") && name.endsWith(".json")) {
                _files.add(file);
                _names.add(file.getName());
            }
        }
    }

    public static void getRootCACertFiles(List<File> _files, List<String> _names) {
        for (File file : Common.getDownloadsDir().listFiles()) {
            if (file.toString().endsWith(".crt")) {
                _files.add(file);
                _names.add(file.getName());
            }
        }
    }

    private static DateFormat userLocalDTF;

    public static String toUserLocalDate(String _epochTime) {
        long l;
        try {
            l = Long.parseLong(_epochTime);
            return toUserLocalDate(l);
        } catch (NumberFormatException _nfe) {
            return "???";
        }
    }

    public static String toUserLocalDate(long _epochTime) {
        if (userLocalDTF == null) {
            userLocalDTF = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        }
        return userLocalDTF.format(new Date(_epochTime));
    }

    public static boolean isExpired(ProvisioningToken _provisioningToken) {
        long expires;
        if(_provisioningToken == null || _provisioningToken.getExpirationTimestamp() == null) {
            return false;
        }
        try {
            expires = Long.parseLong(_provisioningToken.getExpirationTimestamp());
        } catch (NumberFormatException _e) {
            expires = 0;
        }

        // a -1 timestamp is meant to indicate an infinite time
        return expires >= 0 && System.currentTimeMillis() > expires;
    }


    /*

    public static String DateTime2TimeStamp(long timeStamp) {
        String dataFormat = "yyyy-MM-dd";
        if (timeStamp == 0) {
            return "";
        }
        timeStamp = timeStamp * 1000;
        String result = "";
        SimpleDateFormat format = new SimpleDateFormat(dataFormat);
        result = format.format(new Date(timeStamp));

        //String result = "2018-1-1";

        Log.i("rrs", "formatData:" + result);
        return result;
    }


    public static String DateTime2TimeStamp(String timeStamp_str) {

//        long timeStamp = 0;
//        if(timeStamp_str != "") {
//            timeStamp = Long.parseLong(timeStamp_str);
//        }
//
//        String dataFormat = "yyyy-MM-dd";
//        if (timeStamp == 0) {
//            return "";
//        }
//        timeStamp = timeStamp * 1000;
        String result = "2018-1-1";
        //SimpleDateFormat format = new SimpleDateFormat(dataFormat);
        //result = format.format(new Date(timeStamp));
        //result = format.toString();
        Log.i("rrs", "formatData:" + result);
        return result;
    }

*/


}
