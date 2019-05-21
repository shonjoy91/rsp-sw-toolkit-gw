/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.behavior;

import java.security.InvalidParameterException;

public class Behavior {

    public Behavior() {}

    public Behavior(String _id) { this.id = _id; }

    public enum ToggleMode {
        OnInvCycle, // single tag protocol-operation cycle
        OnInvRound, // combination of dwell time and invenvtory cycles
        OnReadRate, // read rate is less than threshold
        None
    }

    private ToggleMode toggleMode = ToggleMode.OnInvCycle;

    public void setToggle_mode(String _val) {
        toggleMode = ToggleMode.valueOf(_val);
    }

    public String getToggle_mode() {
        return toggleMode.toString();
    }


    public String id = "Default";

    public enum OperationMode {NonContinuous, Continuous}

    public OperationMode operation_mode = OperationMode.NonContinuous;


    public enum LinkProfile {

        FM0_40(0),
        MILLER_4_250(1),
        MILLER_4_300(2),
        FM0_400(3),
        CW(99);

        public final int value;

        LinkProfile(int _value) { value = _value; }

        public static LinkProfile valueOf(int _value) {
            for (LinkProfile lp : LinkProfile.values()) {
                if (lp.value == _value) {
                    return lp;
                }
            }
            throw new IllegalArgumentException("Unknown Link Profile Id " + _value);
        }
    }

    private LinkProfile link_profile = LinkProfile.MILLER_4_250;

    public void setLink_profile(int _i) {
        link_profile = LinkProfile.valueOf(_i);
    }

    public int getLink_profile() { return link_profile.value; }

    private double power_level = 30.5f;

    public void setPower_level(double _d) {
        checkParam("power_level", 0.0, 31.5, _d);
        power_level = _d;
    }

    public double getPower_level() { return power_level; }

    public enum SelectedState {Any, Deasserted, Asserted}

    public SelectedState selected_state = SelectedState.Any;

    public enum SessionFlag {S0, S1, S2, S3}

    public SessionFlag session_flag = SessionFlag.S1;

    public void toggleSession() {
        session_flag = (session_flag == SessionFlag.S2) ? SessionFlag.S3 : SessionFlag.S2;
    }

    public enum TargetState {A, B}

    public TargetState target_state = TargetState.A;

    public void toggleTarget() {
        target_state = (target_state == TargetState.A) ?
                       TargetState.B : TargetState.A;
    }

    public enum QAlgorithm {Fixed, Dynamic}

    public QAlgorithm q_algorithm = QAlgorithm.Dynamic;

    private int fixed_q_value = 10;

    public void setFixed_q_value(int _i) {
        checkParam("fixed_q_value", 0, 15, _i);
        fixed_q_value = _i;
    }

    public int getFixed_q_value() { return fixed_q_value; }

    private int start_q_value = 7;

    public void setStart_q_value(int _i) {
        checkParam("start_q_value", 0, 15, _i);
        start_q_value = _i;
    }

    public int getStart_q_value() { return start_q_value; }

    private int min_q_value = 3;

    public void setMin_q_value(int _i) {
        checkParam("min_q_value", 0, 15, _i);
        min_q_value = _i;
    }

    public int getMin_q_value() { return min_q_value; }

    private int max_q_value = 15;

    public void setMax_q_value(int _i) {
        checkParam("max_q_value", 0, 15, _i);
        max_q_value = _i;
    }

    public int getMax_q_value() { return max_q_value; }

    private int retry_count = 0;

    public void setRetry_count(int _i) {
        checkParam("retry_count", 0, 255, _i);
        retry_count = _i;
    }

    public int getRetry_count() { return retry_count; }

    private int threshold_multiplier = 2;

    public void setThreshold_multiplier(int _i) {
        checkParam("threshold_multiplier", 0, 255, _i);
        threshold_multiplier = _i;
    }

    public int getThreshold_multiplier() { return threshold_multiplier; }

    private int dwell_time = 10000;

    public void setDwell_time(int _i) {
        checkParam("dwell_time", 0, 65535, _i);
        dwell_time = _i;
    }

    public int getDwell_time() { return dwell_time; }

    private int inv_cycles = 0;

    public void setInv_cycles(int _i) {
        checkParam("inv_cycles", 0, 65535, _i);
        inv_cycles = _i;
    }

    public int getInv_cycles() { return inv_cycles; }


    public boolean toggle_target_flag = true;
    public boolean repeat_until_no_tags = false;
    public boolean perform_select = false;
    public boolean perform_post_match = false;
    public boolean filter_duplicates = false;
    public boolean auto_repeat = true;

    private int delay_time = 0;

    public void setDelay_time(int _i) {
        delay_time = _i;
    }

    public int getDelay_time() {
        return delay_time;
    }

    private void checkParam(String _param, int _min, int _max, int _val) {
        if (_val < _min || _val > _max) {
            String msg = _param + " must be between " + _min + " - " + _max + " inclusive";
            throw new InvalidParameterException(msg);
        }
    }

    private void checkParam(String _param, double _min, double _max, double _val) {
        if (_val < _min || _val > _max) {
            String msg = _param + " must be between " + _min + " - " + _max + " inclusive";
            throw new InvalidParameterException(msg);
        }

    }

    @Override
    public boolean equals(Object _o) {
        if (this == _o) { return true; }
        if (_o == null || getClass() != _o.getClass()) { return false; }

        Behavior behavior = (Behavior) _o;

        if (Double.compare(behavior.power_level, power_level) != 0) { return false; }
        if (fixed_q_value != behavior.fixed_q_value) { return false; }
        if (start_q_value != behavior.start_q_value) { return false; }
        if (min_q_value != behavior.min_q_value) { return false; }
        if (max_q_value != behavior.max_q_value) { return false; }
        if (retry_count != behavior.retry_count) { return false; }
        if (threshold_multiplier != behavior.threshold_multiplier) { return false; }
        if (dwell_time != behavior.dwell_time) { return false; }
        if (inv_cycles != behavior.inv_cycles) { return false; }
        if (toggle_target_flag != behavior.toggle_target_flag) { return false; }
        if (repeat_until_no_tags != behavior.repeat_until_no_tags) { return false; }
        if (perform_select != behavior.perform_select) { return false; }
        if (perform_post_match != behavior.perform_post_match) { return false; }
        if (filter_duplicates != behavior.filter_duplicates) { return false; }
        if (auto_repeat != behavior.auto_repeat) { return false; }
        if (delay_time != behavior.delay_time) { return false; }
        if (toggleMode != behavior.toggleMode) { return false; }
        if (!id.equals(behavior.id)) { return false; }
        if (operation_mode != behavior.operation_mode) { return false; }
        if (link_profile != behavior.link_profile) { return false; }
        if (selected_state != behavior.selected_state) { return false; }
        if (session_flag != behavior.session_flag) { return false; }
        if (target_state != behavior.target_state) { return false; }
        return q_algorithm == behavior.q_algorithm;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = toggleMode.hashCode();
        result = 31 * result + id.hashCode();
        result = 31 * result + operation_mode.hashCode();
        result = 31 * result + link_profile.hashCode();
        temp = Double.doubleToLongBits(power_level);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + selected_state.hashCode();
        result = 31 * result + session_flag.hashCode();
        result = 31 * result + target_state.hashCode();
        result = 31 * result + q_algorithm.hashCode();
        result = 31 * result + fixed_q_value;
        result = 31 * result + start_q_value;
        result = 31 * result + min_q_value;
        result = 31 * result + max_q_value;
        result = 31 * result + retry_count;
        result = 31 * result + threshold_multiplier;
        result = 31 * result + dwell_time;
        result = 31 * result + inv_cycles;
        result = 31 * result + (toggle_target_flag ? 1 : 0);
        result = 31 * result + (repeat_until_no_tags ? 1 : 0);
        result = 31 * result + (perform_select ? 1 : 0);
        result = 31 * result + (perform_post_match ? 1 : 0);
        result = 31 * result + (filter_duplicates ? 1 : 0);
        result = 31 * result + (auto_repeat ? 1 : 0);
        result = 31 * result + delay_time;
        return result;
    }
}
