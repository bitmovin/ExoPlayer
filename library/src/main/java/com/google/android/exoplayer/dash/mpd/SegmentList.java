package com.google.android.exoplayer.dash.mpd;

/**
 * Created by dweinberger on 6/13/15.
 */

import java.util.List;

/**
 * A {@link MultiSegmentBase} that uses a SegmentList to define its segments.
 */
public class SegmentList extends MultiSegmentBase {

    /* package */ final List<RangedUri> mediaSegments;

    /**
     * @param initialization A {@link RangedUri} corresponding to initialization data, if such data
     *     exists.
     * @param timescale The timescale in units per second.
     * @param presentationTimeOffset The presentation time offset. The value in seconds is the
     *     division of this value and {@code timescale}.
     * @param periodDurationMs The duration of the enclosing period in milliseconds.
     * @param startNumber The sequence number of the first segment.
     * @param duration The duration of each segment in the case of fixed duration segments. The
     *     value in seconds is the division of this value and {@code timescale}. If
     *     {@code segmentTimeline} is non-null then this parameter is ignored.
     * @param segmentTimeline A segment timeline corresponding to the segments. If null, then
     *     segments are assumed to be of fixed duration as specified by the {@code duration}
     *     parameter.
     * @param mediaSegments A list of {@link RangedUri}s indicating the locations of the segments.
     */
    public SegmentList(RangedUri initialization, long timescale, long presentationTimeOffset,
                       long periodDurationMs, int startNumber, long duration,
                       List<SegmentTimelineElement> segmentTimeline, List<RangedUri> mediaSegments) {
        super(initialization, timescale, presentationTimeOffset, periodDurationMs, startNumber,
                duration, segmentTimeline);
        this.mediaSegments = mediaSegments;
    }

    @Override
    public RangedUri getSegmentUrl(Representation representation, int sequenceNumber) {
        return mediaSegments.get(sequenceNumber - startNumber);
    }

    @Override
    public int getLastSegmentNum() {
        return startNumber + mediaSegments.size() - 1;
    }

    @Override
    public boolean isExplicit() {
        return true;
    }

}
