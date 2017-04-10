package com.dlut.cs.utils;

/**
 * Created by john_liu on 2017/3/18.
 */
public interface OffsetHandler {
    public OffsetResponse getOffset();
    public boolean saveOffset(long tsOffset,long increment,long delayTime);
    public boolean saveStartingPosition(long tsOffset);

}
