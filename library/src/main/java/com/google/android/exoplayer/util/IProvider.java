package com.google.android.exoplayer.util;

/**
 * Created by dweinberger on 6/13/15.
 */

import com.google.android.exoplayer.CodecCounters;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.upstream.BandwidthMeter;

/**
 * Provides debug information about an ongoing playback.
 */
public interface IProvider {
    /**
     * Returns the current playback position, in milliseconds.
     */
    long getCurrentPosition();

    /**
     * Returns a format whose information should be displayed, or null.
     */
    Format getFormat();

    /**
     * Returns a {@link com.google.android.exoplayer.upstream.BandwidthMeter} whose estimate should be displayed, or null.
     */
    BandwidthMeter getBandwidthMeter();

    /**
     * Returns a {@link com.google.android.exoplayer.CodecCounters} whose information should be displayed, or null.
     */
    CodecCounters getCodecCounters();
}
