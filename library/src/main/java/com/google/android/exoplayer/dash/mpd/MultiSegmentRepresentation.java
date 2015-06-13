package com.google.android.exoplayer.dash.mpd;

import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.dash.DashSegmentIndex;

/**
 * A DASH representation consisting of multiple segments.
 */
public class MultiSegmentRepresentation extends Representation implements DashSegmentIndex {

    private final MultiSegmentBase segmentBase;

    /**
     * @param periodStartMs The start time of the enclosing period in milliseconds.
     * @param periodDurationMs The duration of the enclosing period in milliseconds, or -1 if the
     *     duration is unknown.
     * @param contentId Identifies the piece of content to which this representation belongs.
     * @param revisionId Identifies the revision of the content.
     * @param format The format of the representation.
     * @param segmentBase The segment base underlying the representation.
     */
    public MultiSegmentRepresentation(long periodStartMs, long periodDurationMs, String contentId, long revisionId, Format format, MultiSegmentBase segmentBase) {
        super(periodStartMs, periodDurationMs, contentId, revisionId, format, segmentBase);
        this.segmentBase = segmentBase;
    }

    @Override
    public RangedUri getIndexUri() {
        return null;
    }

    @Override
    public DashSegmentIndex getIndex() {
        return this;
    }

    // DashSegmentIndex implementation.

    @Override
    public RangedUri getSegmentUrl(int segmentIndex) {
        return segmentBase.getSegmentUrl(this, segmentIndex);
    }

    @Override
    public int getSegmentNum(long timeUs) {
        return segmentBase.getSegmentNum(timeUs);
    }

    @Override
    public long getTimeUs(int segmentIndex) {
        return segmentBase.getSegmentTimeUs(segmentIndex);
    }

    @Override
    public long getDurationUs(int segmentIndex) {
        return segmentBase.getSegmentDurationUs(segmentIndex);
    }

    @Override
    public int getFirstSegmentNum() {
        return segmentBase.getFirstSegmentNum();
    }

    @Override
    public int getLastSegmentNum() {
        return segmentBase.getLastSegmentNum();
    }

    @Override
    public boolean isExplicit() {
        return segmentBase.isExplicit();
    }

}