package com.google.android.exoplayer.dash.mpd;

/**
 * A {@link SegmentBase} that defines a single segment.
 */
public class SingleSegmentBase extends SegmentBase {

    /**
     * The uri of the segment.
     */
    public final String uri;

    /* package */ final long indexStart;
    /* package */ final long indexLength;

    /**
     * @param initialization A {@link RangedUri} corresponding to initialization data, if such data
     *     exists.
     * @param timescale The timescale in units per second.
     * @param presentationTimeOffset The presentation time offset. The value in seconds is the
     *     division of this value and {@code timescale}.
     * @param uri The uri of the segment.
     * @param indexStart The byte offset of the index data in the segment.
     * @param indexLength The length of the index data in bytes.
     */
    public SingleSegmentBase(RangedUri initialization, long timescale, long presentationTimeOffset,
                             String uri, long indexStart, long indexLength) {
        super(initialization, timescale, presentationTimeOffset);
        this.uri = uri;
        this.indexStart = indexStart;
        this.indexLength = indexLength;
    }

    /**
     * @param uri The uri of the segment.
     */
    public SingleSegmentBase(String uri) {
        this(null, 1, 0, uri, 0, -1);
    }

    public RangedUri getIndex() {
        return indexLength <= 0 ? null : new RangedUri(uri, null, indexStart, indexLength);
    }

}