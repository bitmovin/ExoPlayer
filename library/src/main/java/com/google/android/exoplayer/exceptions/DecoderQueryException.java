package com.google.android.exoplayer.exceptions;

/**
 * Created by dweinberger on 6/13/15.
 */

/**
 * Thrown when an error occurs querying the device for its underlying media capabilities.
 * <p>
 * Such failures are not expected in normal operation and are normally temporary (e.g. if the
 * mediaserver process has crashed and is yet to restart).
 */
public class DecoderQueryException extends Exception {
    public DecoderQueryException(Throwable cause) {
        super("Failed to query underlying media codecs", cause);
    }
}
