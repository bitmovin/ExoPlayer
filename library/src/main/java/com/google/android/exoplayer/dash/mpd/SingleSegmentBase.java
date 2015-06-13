package com.google.android.exoplayer.dash.mpd;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

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

  public static SingleSegmentBase createInstanceFromXML(XmlPullParser xpp, String baseUrl,
                                               SingleSegmentBase parent) throws XmlPullParserException, IOException {

    long timescale = MediaPresentationDescriptionParser.parseLong(xpp, "timescale", parent != null ? parent.timescale : 1);
    long presentationTimeOffset = MediaPresentationDescriptionParser.parseLong(xpp, "presentationTimeOffset",
        parent != null ? parent.presentationTimeOffset : 0);

    long indexStart = parent != null ? parent.indexStart : 0;
    long indexLength = parent != null ? parent.indexLength : -1;
    String indexRangeText = xpp.getAttributeValue(null, "indexRange");
    if (indexRangeText != null) {
      String[] indexRange = indexRangeText.split("-");
      indexStart = Long.parseLong(indexRange[0]);
      indexLength = Long.parseLong(indexRange[1]) - indexStart + 1;
    }

    RangedUri initialization = parent != null ? parent.initialization : null;
    do {
      xpp.next();
      if (MediaPresentationDescriptionParser.isStartTag(xpp, "Initialization")) {
        initialization = MediaPresentationDescriptionParser.parseInitialization(xpp, baseUrl);
      }
    } while (!MediaPresentationDescriptionParser.isEndTag(xpp, "SegmentBase"));

    return buildSingleSegmentBase(initialization, timescale, presentationTimeOffset, baseUrl,
        indexStart, indexLength);
  }

  protected static SingleSegmentBase buildSingleSegmentBase(RangedUri initialization, long timescale,
                                                     long presentationTimeOffset, String baseUrl, long indexStart, long indexLength) {
    return new SingleSegmentBase(initialization, timescale, presentationTimeOffset, baseUrl,
        indexStart, indexLength);
  }
}