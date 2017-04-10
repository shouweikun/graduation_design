package com.dlut.cs.utils;


import lombok.Data;
import lombok.Getter;

/**
 * Created by john_liu on 2017/3/18.
 */
@Data
public class OffsetResponse {

    private long timestamp;
    private int increment;
    private boolean success;
    private Exception e;

    public long getTimestamp() {
        return timestamp;
    }

    public OffsetResponse setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public int getIncrement() {
        return increment;
    }

    public OffsetResponse setIncrement(int increment) {
        this.increment = increment;
        return this;
    }

    public boolean isSuccess() {
        return success;
    }

    public OffsetResponse setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public Exception getE() {
        return e;
    }

    public void setE(Exception e) {
        this.e = e;
    }

}
