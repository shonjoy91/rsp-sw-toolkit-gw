/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

import java.util.List;

public class ConnectResponse extends JsonResponseOK {

    public ConnectResponse(String _id, String _facilityId,
                           long _currTime,
                           String _ntpHost, List<String> _softwareRepos,
                           String _sshPublicKey) {
        super(_id, Boolean.TRUE);
        result = new Result(_facilityId, _currTime, _ntpHost, _softwareRepos, _sshPublicKey);
    }

    public static class Result {
        public String facility_id;
        public long sent_on;
        public String ntp_host;
        public List<String> software_repos;
        public String ssh_public_key;

        public Result(String _facilityId, long _currTime,
                      String _ntpHost, List<String> _softwareRepos,
                      String _sshPublicKey) {
            facility_id = _facilityId;
            sent_on = _currTime;
            ntp_host = _ntpHost;
            software_repos = _softwareRepos;
            ssh_public_key = _sshPublicKey;
        }
    }

}
