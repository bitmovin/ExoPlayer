package com.google.android.exoplayer.exceptions;

/**
 * Created by dweinberger on 6/13/15.
 */

/**
 * Thrown when a failure occurs instantiating an {@link android.media.AudioTrack}.
 */
public final class InitializationException extends Exception {
    /** The state as reported by {@link android.media.AudioTrack#getState()}. */
    public final int audioTrackState;

    public InitializationException(
            int audioTrackState, int sampleRate, int channelConfig, int bufferSize) {
        super("AudioTrack init failed: " + audioTrackState + ", Config(" + sampleRate + ", "
                + channelConfig + ", " + bufferSize + ")");
        this.audioTrackState = audioTrackState;
    }
}
