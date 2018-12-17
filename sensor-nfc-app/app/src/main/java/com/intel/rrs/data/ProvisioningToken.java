/*
 * Project: RRS NFC App
 * Module: ProvisioningToken
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

package com.intel.rrs.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.intel.rrs.helper.Common;

public class ProvisioningToken implements Parcelable {

    private String username;
    private String token;
    private String generatedTimestamp;
    private String expirationTimestamp;

    public ProvisioningToken() {

    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String _username) {
        this.username = _username;
    }


    public String getToken() {
        if (token == null) {
            return "";
        } else {
            return token;
        }
    }

    public void setToken(String _token) {
        token = _token;
    }


    public String getGeneratedTimestamp() {
        return generatedTimestamp;
    }

    public void setGeneratedTimestamp(String _generatedTimestamp) {
        generatedTimestamp = _generatedTimestamp;
    }


    public String getExpirationTimestamp() {
        return expirationTimestamp;
    }

    public void setExpirationTimestamp(String _expirationTimestamp) {
        expirationTimestamp = _expirationTimestamp;
    }

    public String formatIssued() {
        return Common.toUserLocalDate(parseTimestamp(generatedTimestamp));
    }

    public String formatExpiration() {
        return Common.toUserLocalDate(parseTimestamp(expirationTimestamp));
    }


    public boolean isExpired() {
        long l = parseTimestamp(expirationTimestamp);
        return l >= 0 && System.currentTimeMillis() > l;
    }

    public boolean isInfinite() {
        long l = parseTimestamp(expirationTimestamp);
        return l < 0;
    }

    private long parseTimestamp(String _timestamp) {
        long l;
        try {
            l = Long.parseLong(_timestamp);
        } catch (NumberFormatException _nfe) {
            l = 0;
        }
        return l;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(username);
        dest.writeString(token);
        dest.writeString(generatedTimestamp);
        dest.writeString(expirationTimestamp);
    }


    public static final Parcelable.Creator<ProvisioningToken> CREATOR
            = new Parcelable.Creator<ProvisioningToken>() {
        public ProvisioningToken createFromParcel(Parcel in) {
            return new ProvisioningToken(in);
        }

        public ProvisioningToken[] newArray(int size) {
            return new ProvisioningToken[size];
        }
    };

    private ProvisioningToken(Parcel in) {
        username = in.readString();
        token = in.readString();
        generatedTimestamp = in.readString();
        expirationTimestamp = in.readString();
    }

}
