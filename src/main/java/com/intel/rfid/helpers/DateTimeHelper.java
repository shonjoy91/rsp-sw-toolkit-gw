/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.helpers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateTimeHelper {

    // common format definitions to support consistent names/output across the application
    public static final String FILE_DATE_FORMAT = "yyyyMMdd_HHmmss";
    public static final String CONSOLE_DATE_FORMAT = "HH:mm:ss MM/dd/yyyy";

    /**
     * @return a formatter that avoids special characters
     * and supports sorting by character the same as the
     * actual time represented
     */
    public static SimpleDateFormat newLocalFormatterMachine() {
        return new SimpleDateFormat(FILE_DATE_FORMAT);
    }

    public static String toJsonIsoUtc(Date _date) {
        if (_date == null) { return "null"; }
        Instant i = Instant.ofEpochMilli(_date.getTime());
        return ZonedDateTime.ofInstant(i, ZoneOffset.UTC)
                            .format(DateTimeFormatter.ISO_INSTANT);
    }

    private static SimpleDateFormat fileNameLocalSDF;

    public static synchronized String toFilelNameLocal(Date _date) {
        if (fileNameLocalSDF == null) {
            fileNameLocalSDF = new SimpleDateFormat(FILE_DATE_FORMAT);
        }
        return fileNameLocalSDF.format(_date);
    }

    private static DateTimeFormatter userLocalDTF;

    public static synchronized String toUserLocal(Date _date) {
        if (userLocalDTF == null) {
            userLocalDTF = DateTimeFormatter.ofPattern(CONSOLE_DATE_FORMAT);
        }
        TimeZone dtz = TimeZone.getDefault();
        long offset = dtz.getRawOffset();
        String prefix = (offset < 0) ? "-" : "+";
        offset = Math.abs(offset);
        long h = TimeUnit.MILLISECONDS.toHours(offset);
        offset = offset % (TimeUnit.HOURS.toMillis(1));
        long m = TimeUnit.MILLISECONDS.toMinutes(offset);

        return String.format("%s [UTC %s%02d:%02d %s]",
                             _date.toInstant().atZone(ZoneId.systemDefault()).format(userLocalDTF),
                             prefix, h, m,
                             ZoneId.systemDefault().getId());

    }

    private static final Pattern durationRegex = Pattern.compile(
            "(\\+\\s*)?((?<hours>[0-9]+)\\s*(hours|hour|hrs|hr|h))?[\\s,;:]*((?<minutes>[0-9]+)\\s*(minutes|minute|mins|min|m))?[\\s,;:]*((?<seconds>[0-9]+)\\s*(seconds|second|secs|sec|s))?[\\s,;:]*((?<milliseconds>[0-9]+)\\s*(milliseconds|millisecond|millis|msecs|msec|ms))?");
    private static final String[] dateFormats = {
            "h:mm:ss a",
            "h:mm:ssa",
            "h:mm a",
            "h:mma",
            "h a",
            "ha",
            "h:mm:ss",
            "h:mm",
            "hhmm",
            "h"
    };

    // SimpleDateFormat is not thread safe!!
    private static final List<SimpleDateFormat> dateFormatters;

    static {
        dateFormatters = new ArrayList<>();
        for (String format : dateFormats) {
            dateFormatters.add(new SimpleDateFormat(format));
        }
    }

    public static String epochToHuman(long _epoch) {
        Date date = new Date(_epoch);
        return date.toString();
    }

    protected static final SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm:ss");

    public static String timeFromNow(long _duration, TimeUnit _unit) {
        Date date = new Date(System.currentTimeMillis() + _unit.toMillis(_duration));
        synchronized (sdf24) {
            return sdf24.format(date);
        }
    }

    public static ParseResult tryParseTime(String _time) throws ParseException {
        if (_time.startsWith("+")) {
            Long duration = tryParseDurationToMilliseconds(_time);
            if (duration != null) {
                Date d = new Date();
                d.setTime(d.getTime() + duration);
                ParseResult res = new ParseResult(d);
                res.setWasDuration(true);
                return res;
            }
        }
        // add support for simple 24 hour format
        if (_time.length() == 8 &&
                _time.charAt(2) == ':' &&
                _time.charAt(5) == ':') {

            synchronized (sdf24) {
                Date d = sdf24.parse(_time);
                ParseResult res = new ParseResult(d);
                res.setWasDuration(false);
                return res;
            }

        }
        synchronized (dateFormatters) {
            for (SimpleDateFormat df : dateFormatters) {
                try {
                    Date d = df.parse(_time);
                    ParseResult res = new ParseResult(d);
                    res.setWasDuration(false);
                    return res;
                } catch (ParseException e) {
                    // this is expected code path for non matching formats
                }
            }
        }
        return null;
    }

    public static Long tryParseDurationToMilliseconds(String _duration) {
        Matcher m = durationRegex.matcher(_duration);
        if (m.matches()) {
            return convertToTotalMilliseconds(tryParseInt(m.group("hours")),
                                              tryParseInt(m.group("minutes")),
                                              tryParseInt(m.group("seconds")),
                                              tryParseInt(m.group("milliseconds")));
        } else {
            return null;
        }
    }

    public static Integer tryParseInt(String _value) {
        if (StringHelper.isNullOrEmpty(_value)) {
            return null;
        }
        try {
            return Integer.valueOf(_value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static long convertToTotalMilliseconds(Integer _hours, Integer _mins, Integer _seconds, Integer _millis) {
        long totalMillis = 0;
        if (_hours != null) {
            totalMillis += _hours * 3600000;
        }
        if (_mins != null) {
            totalMillis += _mins * 60000;
        }
        if (_seconds != null) {
            totalMillis += _seconds * 1000;
        }
        if (_millis != null) {
            totalMillis += _millis;
        }
        return totalMillis;
    }

    public static String millisecondsToPrettyTime(long _millis, boolean _printMillis) {
        boolean negative = false;
        if (_millis < 0) {
            negative = true;
            _millis = Math.abs(_millis);
        }
        long originalMillis = _millis;
        long days = TimeUnit.MILLISECONDS.toDays(_millis);
        _millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(_millis);
        _millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(_millis);
        _millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(_millis);
        _millis -= TimeUnit.SECONDS.toMillis(seconds);

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append(hours == 1 ? " hr " : " hrs ");
        }
        if (minutes > 0) {
            sb.append(minutes).append(minutes == 1 ? " min " : " mins ");
        }
        if (seconds > 0) {
            sb.append(seconds).append(seconds == 1 ? " second " : " seconds ");
        }
        if ((_millis > 0 && _printMillis) || originalMillis < 1000) {
            sb.append(_millis).append(" ms ");
        }
        if (negative) {
            sb.append("ago");
        }
        return sb.toString().trim();
    }

    public static String timeAsHMS_MS(long _timestamp) {
        long ms = _timestamp % 1000;
        long s = (_timestamp / 1000) % 60;
        long m = (_timestamp / (60 * 1000)) % 60;
        long h = (_timestamp / (60 * 60 * 1000));
        return String.format("%02d:%02d:%02d:%03d", h, m, s, ms);
    }

    public static class ParseResult {
        private int hours = 0;
        private int minutes = 0;
        private int seconds = 0;
        private int milliseconds = 0;
        private boolean wasDuration = false;

        public ParseResult() {

        }

        public ParseResult(Date _date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(_date);
            this.hours = calendar.get(Calendar.HOUR_OF_DAY);
            this.minutes = calendar.get(Calendar.MINUTE);
            this.seconds = calendar.get(Calendar.SECOND);

            // Get the difference in the last 3 digits of the time which will be milliseconds
            double withMillis = _date.getTime() / 1000.0;
            int withoutMillis = (int) (_date.getTime() / 1000);
            this.milliseconds = (int) ((withMillis - withoutMillis) * 1000.0);
        }

        public int getHours() {
            return hours;
        }

        public void setHours(int _hours) {
            hours = _hours;
        }

        public int getMinutes() {
            return minutes;
        }

        public void setMinutes(int _minutes) {
            minutes = _minutes;
        }

        public int getSeconds() {
            return seconds;
        }

        public void setSeconds(int _seconds) {
            seconds = _seconds;
        }

        public int getMilliseconds() {
            return milliseconds;
        }

        public void setMilliseconds(int _milliseconds) {
            milliseconds = _milliseconds;
        }

        public boolean getWasDuration() {
            return wasDuration;
        }

        public void setWasDuration(boolean _wasDuration) {
            wasDuration = _wasDuration;
        }
    }
}
