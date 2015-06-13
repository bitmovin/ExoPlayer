package com.google.android.exoplayer.dash.mpd;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
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

  public static SegmentList createInstanceFromXML(XmlPullParser xpp, String baseUrl, SegmentList parent,
                                                  long periodDurationMs) throws XmlPullParserException, IOException {

    long timescale = MediaPresentationDescriptionParser.parseLong(xpp, "timescale", parent != null ? parent.timescale : 1);
    long presentationTimeOffset = MediaPresentationDescriptionParser.parseLong(xpp, "presentationTimeOffset",
        parent != null ? parent.presentationTimeOffset : 0);
    long duration = MediaPresentationDescriptionParser.parseLong(xpp, "duration", parent != null ? parent.duration : -1);
    int startNumber = MediaPresentationDescriptionParser.parseInt(xpp, "startNumber", parent != null ? parent.startNumber : 1);

    RangedUri initialization = null;
    List<SegmentTimelineElement> timeline = null;
    List<RangedUri> segments = null;

    do {
      xpp.next();
      if (MediaPresentationDescriptionParser.isStartTag(xpp, "Initialization")) {
        initialization = MediaPresentationDescriptionParser.parseInitialization(xpp, baseUrl);
      } else if (MediaPresentationDescriptionParser.isStartTag(xpp, "SegmentTimeline")) {
        timeline = MediaPresentationDescriptionParser.parseSegmentTimeline(xpp);
      } else if (MediaPresentationDescriptionParser.isStartTag(xpp, "SegmentURL")) {
        if (segments == null) {
          segments = new ArrayList<>();
        }
        segments.add(MediaPresentationDescriptionParser.parseSegmentUrl(xpp, baseUrl));
      }
    } while (!MediaPresentationDescriptionParser.isEndTag(xpp, "SegmentList"));

    if (parent != null) {
      initialization = initialization != null ? initialization : parent.initialization;
      timeline = timeline != null ? timeline : parent.segmentTimeline;
      segments = segments != null ? segments : parent.mediaSegments;
    }

    return buildSegmentList(initialization, timescale, presentationTimeOffset, periodDurationMs,
        startNumber, duration, timeline, segments);
  }

  protected static SegmentList buildSegmentList(RangedUri initialization, long timescale,
                                         long presentationTimeOffset, long periodDurationMs, int startNumber, long duration,
                                         List<SegmentTimelineElement> timeline, List<RangedUri> segments) {
    return new SegmentList(initialization, timescale, presentationTimeOffset, periodDurationMs,
        startNumber, duration, timeline, segments);
  }
}
