package com.google.android.exoplayer.dash.mpd;

/**
 * Created by dweinberger on 6/13/15.
 */

/**
 * Represents a timeline segment from the MPD's SegmentTimeline list.
 */
public class SegmentTimelineElement {

    /* package */ long startTime;
    /* package */ long duration;

    /**
     * @param startTime The start time of the element. The value in seconds is the division of this
     *     value and the {@code timescale} of the enclosing element.
     * @param duration The duration of the element. The value in seconds is the division of this
     *     value and the {@code timescale} of the enclosing element.
     */
    public SegmentTimelineElement(long startTime, long duration) {
        this.startTime = startTime;
        this.duration = duration;
    }

}
