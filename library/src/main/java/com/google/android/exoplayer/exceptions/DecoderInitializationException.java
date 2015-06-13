package com.google.android.exoplayer.exceptions;

/**
 * Created by dweinberger on 6/13/15.
 */

import android.annotation.TargetApi;
import android.media.MediaCodec;

import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.util.Util;

/**
 * Thrown when a failure occurs instantiating a decoder.
 */
public class DecoderInitializationException extends Exception {
    public static final int CUSTOM_ERROR_CODE_BASE = -50000;
    public static final int NO_SUITABLE_DECODER_ERROR = CUSTOM_ERROR_CODE_BASE + 1;
    public static final int DECODER_QUERY_ERROR = CUSTOM_ERROR_CODE_BASE + 2;

    /**
     * The name of the decoder that failed to initialize. Null if no suitable decoder was found.
     */
    public final String decoderName;

    /**
     * An optional developer-readable diagnostic information string. May be null.
     */
    public final String diagnosticInfo;

    public DecoderInitializationException(MediaFormat mediaFormat, Throwable cause, int errorCode) {
        super("Decoder init failed: [" + errorCode + "], " + mediaFormat, cause);
        this.decoderName = null;
        this.diagnosticInfo = buildCustomDiagnosticInfo(errorCode);
    }

    public DecoderInitializationException(MediaFormat mediaFormat, Throwable cause,
                                          String decoderName) {
        super("Decoder init failed: " + decoderName + ", " + mediaFormat, cause);
        this.decoderName = decoderName;
        this.diagnosticInfo = Util.SDK_INT >= 21 ? getDiagnosticInfoV21(cause) : null;
    }

    @TargetApi(21)
    private static String getDiagnosticInfoV21(Throwable cause) {
        if (cause instanceof MediaCodec.CodecException) {
            return ((MediaCodec.CodecException) cause).getDiagnosticInfo();
        }
        return null;
    }

    private static String buildCustomDiagnosticInfo(int errorCode) {
        String sign = errorCode < 0 ? "neg_" : "";
        return "com.google.android.exoplayer.MediaCodecTrackRenderer_" + sign + Math.abs(errorCode);
    }

}