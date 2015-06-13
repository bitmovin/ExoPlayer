package com.google.android.exoplayer.exceptions;

/**
 * Created by dweinberger on 6/13/15.
 */

/**
 * Thrown when a failure occurs writing to an {@link android.media.AudioTrack}.
 */
public final class WriteException extends Exception {

    /** The value returned from {@link android.media.AudioTrack#write(byte[], int, int)}. */
    public final int errorCode;

    public WriteException(int errorCode) {
        super("AudioTrack write failed: " + errorCode);
        this.errorCode = errorCode;
    }
}
