package com.google.android.exoplayer.dash.mpd;

import android.net.Uri;

import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.dash.DashSegmentIndex;
import com.google.android.exoplayer.dash.DashSingleSegmentIndex;

/**
 * A DASH representation consisting of a single segment.
 */
public class SingleSegmentRepresentation extends Representation {

    /**
     * The uri of the single segment.
     */
    public final Uri uri;

    /**
     * The content length, or -1 if unknown.
     */
    public final long contentLength;

    private final RangedUri indexUri;
    private final DashSingleSegmentIndex segmentIndex;

    /**
     * @param periodStartMs The start time of the enclosing period in milliseconds.
     * @param periodDurationMs The duration of the enclosing period in milliseconds, or -1 if the
     *     duration is unknown.
     * @param contentId Identifies the piece of content to which this representation belongs.
     * @param revisionId Identifies the revision of the content.
     * @param format The format of the representation.
     * @param uri The uri of the media.
     * @param initializationStart The offset of the first byte of initialization data.
     * @param initializationEnd The offset of the last byte of initialization data.
     * @param indexStart The offset of the first byte of index data.
     * @param indexEnd The offset of the last byte of index data.
     * @param contentLength The content length, or -1 if unknown.
     */
    public static SingleSegmentRepresentation newInstance(long periodStartMs, long periodDurationMs, String contentId, long revisionId, Format format, String uri, long initializationStart,
                                                          long initializationEnd, long indexStart, long indexEnd, long contentLength) {
        RangedUri rangedUri = new RangedUri(uri, null, initializationStart, initializationEnd - initializationStart + 1);
        SingleSegmentBase segmentBase = new SingleSegmentBase(rangedUri, 1, 0, uri, indexStart, indexEnd - indexStart + 1);
        return new SingleSegmentRepresentation(periodStartMs, periodDurationMs, contentId, revisionId, format, segmentBase, contentLength);
    }

    /**
     * @param periodStartMs The start time of the enclosing period in milliseconds.
     * @param periodDurationMs The duration of the enclosing period in milliseconds, or -1 if the
     *     duration is unknown.
     * @param contentId Identifies the piece of content to which this representation belongs.
     * @param revisionId Identifies the revision of the content.
     * @param format The format of the representation.
     * @param segmentBase The segment base underlying the representation.
     * @param contentLength The content length, or -1 if unknown.
     */
    public SingleSegmentRepresentation(long periodStartMs, long periodDurationMs, String contentId, long revisionId, Format format, SingleSegmentBase segmentBase, long contentLength) {
        super(periodStartMs, periodDurationMs, contentId, revisionId, format, segmentBase);
        this.uri = Uri.parse(segmentBase.uri);
        this.indexUri = segmentBase.getIndex();
        this.contentLength = contentLength;
        // If we have an index uri then the index is defined externally, and we shouldn't return one
        // directly. If we don't, then we can't do better than an index defining a single segment.
        segmentIndex = indexUri != null ? null : new DashSingleSegmentIndex(periodStartMs * 1000,
                periodDurationMs * 1000, new RangedUri(segmentBase.uri, null, 0, -1));
    }

    @Override
    public RangedUri getIndexUri() {
        return indexUri;
    }

    @Override
    public DashSegmentIndex getIndex() {
        return segmentIndex;
    }

}