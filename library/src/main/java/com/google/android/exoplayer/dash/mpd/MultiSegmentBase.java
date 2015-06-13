package com.google.android.exoplayer.dash.mpd;

/**
 * Created by dweinberger on 6/13/15.
 */

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.dash.DashSegmentIndex;
import com.google.android.exoplayer.util.Util;

import java.util.List;

/**
 * A {@link SegmentBase} that consists of multiple segments.
 */
public abstract class MultiSegmentBase extends SegmentBase {

    /* package */ final long periodDurationMs;
    /* package */ final int startNumber;
    /* package */ final long duration;
    /* package */ final List<SegmentTimelineElement> segmentTimeline;

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
     */
    public MultiSegmentBase(RangedUri initialization, long timescale, long presentationTimeOffset,
                            long periodDurationMs, int startNumber, long duration,
                            List<SegmentTimelineElement> segmentTimeline) {
        super(initialization, timescale, presentationTimeOffset);
        this.periodDurationMs = periodDurationMs;
        this.startNumber = startNumber;
        this.duration = duration;
        this.segmentTimeline = segmentTimeline;
    }

    /**
     * @see com.google.android.exoplayer.dash.DashSegmentIndex#getSegmentNum(long)
     */
    public int getSegmentNum(long timeUs) {
        final int firstSegmentNum = getFirstSegmentNum();
        int lowIndex = firstSegmentNum;
        int highIndex = getLastSegmentNum();
        if (segmentTimeline == null) {
            // All segments are of equal duration (with the possible exception of the last one).
            long durationUs = (duration * C.MICROS_PER_SECOND) / timescale;
            int segmentNum = startNumber + (int) (timeUs / durationUs);
            // Ensure we stay within bounds.
            return segmentNum < lowIndex ? lowIndex
                    : highIndex != DashSegmentIndex.INDEX_UNBOUNDED && segmentNum > highIndex ? highIndex
                    : segmentNum;
        } else {
            // The high index cannot be unbounded. Identify the segment using binary search.
            while (lowIndex <= highIndex) {
                int midIndex = (lowIndex + highIndex) / 2;
                long midTimeUs = getSegmentTimeUs(midIndex);
                if (midTimeUs < timeUs) {
                    lowIndex = midIndex + 1;
                } else if (midTimeUs > timeUs) {
                    highIndex = midIndex - 1;
                } else {
                    return midIndex;
                }
            }
            return lowIndex == firstSegmentNum ? lowIndex : highIndex;
        }
    }

    /**
     * @see DashSegmentIndex#getDurationUs(int)
     */
    public final long getSegmentDurationUs(int sequenceNumber) {
        if (segmentTimeline != null) {
            long duration = segmentTimeline.get(sequenceNumber - startNumber).duration;
            return (duration * C.MICROS_PER_SECOND) / timescale;
        } else {
            return sequenceNumber == getLastSegmentNum()
                    ? ((periodDurationMs * 1000) - getSegmentTimeUs(sequenceNumber))
                    : ((duration * C.MICROS_PER_SECOND) / timescale);
        }
    }

    /**
     * @see DashSegmentIndex#getTimeUs(int)
     */
    public final long getSegmentTimeUs(int sequenceNumber) {
        long unscaledSegmentTime;
        if (segmentTimeline != null) {
            unscaledSegmentTime = segmentTimeline.get(sequenceNumber - startNumber).startTime
                    - presentationTimeOffset;
        } else {
            unscaledSegmentTime = (sequenceNumber - startNumber) * duration;
        }
        return Util.scaleLargeTimestamp(unscaledSegmentTime, C.MICROS_PER_SECOND, timescale);
    }

    /**
     * Returns a {@link RangedUri} defining the location of a segment for the given index in the
     * given representation.
     *
     * @see DashSegmentIndex#getSegmentUrl(int)
     */
    public abstract RangedUri getSegmentUrl(Representation representation, int index);

    /**
     * @see DashSegmentIndex#getFirstSegmentNum()
     */
    public int getFirstSegmentNum() {
        return startNumber;
    }

    /**
     * @see DashSegmentIndex#getLastSegmentNum()
     */
    public abstract int getLastSegmentNum();

    /**
     * @see DashSegmentIndex#isExplicit()
     */
    public boolean isExplicit() {
        return segmentTimeline != null;
    }

}
