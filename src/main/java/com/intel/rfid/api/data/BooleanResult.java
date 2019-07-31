package com.intel.rfid.api.data;

public class BooleanResult {
    public final boolean success;
    public final String message;

    public BooleanResult(boolean _success, String _message) {
        success = _success;
        message = _message;
    }
    
    public static BooleanResult True() {
        return new BooleanResult(true, "OK");
    }

    @Override
    public String toString() {
        return "{" +
                "success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
