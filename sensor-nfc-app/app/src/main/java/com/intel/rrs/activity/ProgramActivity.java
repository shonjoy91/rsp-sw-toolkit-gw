/*
 * Project: RRS NFC App
 * Module: RunNfc
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

package com.intel.rrs.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.Gson;
import com.intel.rrs.R;
import com.intel.rrs.data.ProvisioningToken;
import com.intel.rrs.helper.Common;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;


public class ProgramActivity extends Activity {

    private ProvisioningToken provisioningToken;
    private String rootCAHash;
    private NfcAdapter nfcAdapter;

    private TextView textViewRootCAHash;
    private TextView textViewToken;
    private TextView textViewIssued;
    private TextView textViewExpires;
    private TextView textViewStatus;

    private Tag nfcTag;


    final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_program);
        TextView title = findViewById(R.id.title_bar);
        title.setText(getString(R.string.program_title));

        textViewRootCAHash = findViewById(R.id.program_root_ca_cert_hash);
        textViewToken = findViewById(R.id.program_token_id);
        textViewIssued = findViewById(R.id.program_token_issued);
        textViewExpires = findViewById(R.id.program_token_expires);
        textViewStatus = findViewById(R.id.program_token_status);

        SharedPreferences pref = getSharedPreferences(Common.SHARED_PREFS_FILE, MODE_PRIVATE);
        String filePath;
        filePath = pref.getString(Common.KEY_TOKEN_FILENAME, "");
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            provisioningToken = gson.fromJson(br, ProvisioningToken.class);
        } catch (IOException _e) {
            Log.e(getClass().getName(), "lost the token file: " + filePath);
            finish();
        }


        filePath = pref.getString(Common.KEY_ROOT_CA_CERT_FILENAME, "");
        try (FileInputStream is = new FileInputStream(filePath)) {

            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) fact.generateCertificate(is);
            byte[] certBytes = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());
            rootCAHash = String.format("%064x", new BigInteger(1, certBytes));
        } catch (Exception _e) {
            Log.e(getClass().getName(), "lost the root ca cert file: " + filePath);
            finish();
        }


        //        String tokenJson = pref.getString(Common.KEY_CURRENT_TOKEN, null);
//        // how to abort activity and go back?
//        if (tokenJson != null) {
//            provisioningToken = gson.fromJson(tokenJson, ProvisioningToken.class);
//        }

//        rootCAHash = pref.getString(Common.KEY_ROOT_CA_HASH, "");
    }

    @Override
    public void onStart() {
        super.onStart();
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        textViewRootCAHash.setText(rootCAHash);
        textViewToken.setText(provisioningToken.getToken());
        textViewIssued.setText(provisioningToken.formatIssued());
        if(provisioningToken.isInfinite()) {
            textViewExpires.setText(getString(R.string.token_never_expires));
        } else if (provisioningToken.isExpired()) {
            textViewExpires.setText(getString(R.string.token_expired));
            textViewExpires.setTextAppearance(R.style.WarnContent);
        } else {
            textViewExpires.setText(provisioningToken.formatExpiration());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("BaseNfcActivity", "NFC onResume");

        if (rootCAHash == null ||
            rootCAHash.isEmpty() ) {
            showDialog(getString(R.string.err_missing_root_ca_cert));
        }

        if (provisioningToken == null ||
            provisioningToken.getToken() == null ||
            provisioningToken.getToken().isEmpty()) {
            showDialog(getString(R.string.err_missing_token));
        }


        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            showDialog(getString(R.string.err_no_nfc_support));
        } else {
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()), 0);
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }

        if (nfcTag == null) {
            textViewStatus.setText(getString(R.string.nfc_program_state_searching));
            return;
        }

        try {
            writeJSONtoNFCTag();

            textViewStatus.setText(getString(R.string.nfc_program_state_completed));
        } catch (JSONException | IOException | FormatException e) {
            nfcTag = null;
            textViewStatus.setText(getString(R.string.nfc_program_state_searching));
            StringBuilder sb = new StringBuilder(getString(R.string.nfc_program_state_failed));
            sb.append(": ").append(e.getClass().getSimpleName());
            if(e.getMessage() != null) {
                sb.append(": ").append(e.getMessage());
            }
            e.printStackTrace();
            showDialog(sb.toString());
        }

    }

    @Override
    public void onNewIntent(Intent intent) {

        // the nfc adapter will send a new intent here with the tag in it when a tag is discovered
        // it is OK if the tag is not there
        nfcTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
        nfcTag = null;
    }

    private void showDialog(String _message) {
        AlertDialog alertDialog = new AlertDialog.Builder(ProgramActivity.this).create();
        alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL,
                getText(android.R.string.ok),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        alertDialog.setMessage(_message);
        alertDialog.show();
    }

    public static final String GW_TAG_ID = "rsp-software-toolkit-gateway";

    public void writeJSONtoNFCTag() throws JSONException, IOException, FormatException {
        Log.i("ProgramActivity", "writeJSONtoNFC start");

        if (nfcTag == null ||
            rootCAHash == null ||
            rootCAHash.isEmpty() ||
            provisioningToken == null ||
            provisioningToken.getToken() == null ||
            provisioningToken.getToken().isEmpty()) {

            throw new IOException("bad parameters for programming tag");
        }

        JSONObject jsonData = new JSONObject();
        jsonData.put("token", provisioningToken.getToken());
        jsonData.put("fingerprint", rootCAHash);
        jsonData.put("gateway", GW_TAG_ID);

        String strData = jsonData.toString();
        strData = strData.replace("\\", "");
        Log.i("ProgramActivity", "NFC Data :" + strData);
        if (strData.length() == 0) {
            throw new FormatException("bad format of json data");
        }
        Ndef ndef = Ndef.get(nfcTag);
        if (ndef == null) {
            throw new FormatException("NFC Tag doesn't have NDEF format");
        }

        Log.i("ProgramActivity", "NDEF format");
        ndef.connect();
/*                NdefRecord ndefRecord0 = new NdefRecord(
                        NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT,
                        new byte[] {}, hexStrtoBytes(token_value));
                NdefRecord ndefRecord1 = new NdefRecord(
                        NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT,
                        new byte[] {}, hexStrtoBytes(fingerprint256));*/
        NdefRecord ndefRecord = new NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_TEXT,
            new byte[]{},
            strData.getBytes());
        NdefRecord[] records = {ndefRecord};
        NdefMessage ndefMessage = new NdefMessage(records);
        ndef.writeNdefMessage(ndefMessage);
        nfcTag = null;
    }

}
