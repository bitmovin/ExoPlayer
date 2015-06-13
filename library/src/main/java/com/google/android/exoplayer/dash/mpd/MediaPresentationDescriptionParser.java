/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer.dash.mpd;

import com.google.android.exoplayer.ParserException;
import com.google.android.exoplayer.upstream.UriLoadable;
import com.google.android.exoplayer.util.UriUtil;
import com.google.android.exoplayer.util.Util;

import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A parser of media presentation description files.
 */
public class MediaPresentationDescriptionParser extends DefaultHandler
    implements UriLoadable.Parser<MediaPresentationDescription> {

  public static final Pattern FRAME_RATE_PATTERN = Pattern.compile("(\\d+)(?:/(\\d+))??");

  private final String contentId;
  private final XmlPullParserFactory xmlParserFactory;

  /**
   * Equivalent to calling {@code new MediaPresentationDescriptionParser(null)}.
   */
  public MediaPresentationDescriptionParser() {
    this(null);
  }

  /**
   * @param contentId An optional content identifier to include in the parsed manifest.
   */
  // TODO: Remove the need to inject a content identifier here, by not including it in the parsed
  // manifest. Instead, it should be injected directly where needed (i.e. DashChunkSource).
  public MediaPresentationDescriptionParser(String contentId) {
    this.contentId = contentId;
    try {
      xmlParserFactory = XmlPullParserFactory.newInstance();
    } catch (XmlPullParserException e) {
      throw new RuntimeException("Couldn't create XmlPullParserFactory instance", e);
    }
  }

  // MPD parsing.

  @Override
  public MediaPresentationDescription parse(String connectionUrl, InputStream inputStream)
      throws IOException, ParserException {
    try {
      XmlPullParser xpp = xmlParserFactory.newPullParser();
      xpp.setInput(inputStream, null);
      int eventType = xpp.next();
      if (eventType != XmlPullParser.START_TAG || !"MPD".equals(xpp.getName())) {
        throw new ParserException(
            "inputStream does not contain a valid media presentation description");
      }
      return parseMediaPresentationDescription(xpp, connectionUrl);
    } catch (XmlPullParserException e) {
      throw new ParserException(e);
    } catch (ParseException e) {
      throw new ParserException(e);
    }
  }

  protected MediaPresentationDescription parseMediaPresentationDescription(XmlPullParser xpp,
      String baseUrl) throws XmlPullParserException, IOException, ParseException {
    long availabilityStartTime = parseDateTime(xpp, "availabilityStartTime", -1);
    long durationMs = parseDuration(xpp, "mediaPresentationDuration", -1);
    long minBufferTimeMs = parseDuration(xpp, "minBufferTime", -1);
    String typeString = xpp.getAttributeValue(null, "type");
    boolean dynamic = (typeString != null) ? typeString.equals("dynamic") : false;
    long minUpdateTimeMs = (dynamic) ? parseDuration(xpp, "minimumUpdatePeriod", -1) : -1;
    long timeShiftBufferDepthMs = (dynamic) ? parseDuration(xpp, "timeShiftBufferDepth", -1)
        : -1;
    UtcTimingElement utcTiming = null;

    List<Period> periods = new ArrayList<>();
    do {
      xpp.next();
      if (isStartTag(xpp, "BaseURL")) {
        baseUrl = parseBaseUrl(xpp, baseUrl);
      } else if (isStartTag(xpp, "UTCTiming")) {
        utcTiming = parseUtcTiming(xpp);
      } else if (isStartTag(xpp, "Period")) {
        periods.add(parsePeriod(xpp, baseUrl, durationMs));
      }
    } while (!isEndTag(xpp, "MPD"));

    return buildMediaPresentationDescription(availabilityStartTime, durationMs, minBufferTimeMs,
        dynamic, minUpdateTimeMs, timeShiftBufferDepthMs, utcTiming, periods);
  }

  protected MediaPresentationDescription buildMediaPresentationDescription(
      long availabilityStartTime, long durationMs, long minBufferTimeMs, boolean dynamic,
      long minUpdateTimeMs, long timeShiftBufferDepthMs, UtcTimingElement utcTiming,
      List<Period> periods) {
    return new MediaPresentationDescription(availabilityStartTime, durationMs, minBufferTimeMs,
        dynamic, minUpdateTimeMs, timeShiftBufferDepthMs, utcTiming, periods);
  }

  protected UtcTimingElement parseUtcTiming(XmlPullParser xpp) {
    String schemeIdUri = xpp.getAttributeValue(null, "schemeIdUri");
    String value = xpp.getAttributeValue(null, "value");
    return buildUtcTimingElement(schemeIdUri, value);
  }

  protected UtcTimingElement buildUtcTimingElement(String schemeIdUri, String value) {
    return new UtcTimingElement(schemeIdUri, value);
  }

  protected Period parsePeriod(XmlPullParser xpp, String baseUrl, long mpdDurationMs)
      throws XmlPullParserException, IOException {
    String id = xpp.getAttributeValue(null, "id");
    long startMs = parseDuration(xpp, "start", 0);
    long durationMs = parseDuration(xpp, "duration", mpdDurationMs);
    SegmentBase segmentBase = null;
    List<AdaptationSet> adaptationSets = new ArrayList<>();
    do {
      xpp.next();
      if (isStartTag(xpp, "BaseURL")) {
        baseUrl = parseBaseUrl(xpp, baseUrl);
      } else if (isStartTag(xpp, "AdaptationSet")) {
        adaptationSets.add(new AdaptationSet(xpp, baseUrl, startMs, durationMs, segmentBase, this.contentId));
      } else if (isStartTag(xpp, "SegmentBase") || isStartTag(xpp, "SegmentList") || isStartTag(xpp, "SegmentTemplate")) {
        segmentBase = SegmentBase.createInstanceFromXML(xpp, baseUrl, null, durationMs);
      }
    } while (!isEndTag(xpp, "Period"));

    return buildPeriod(id, startMs, durationMs, adaptationSets);
  }

  protected Period buildPeriod(
      String id, long startMs, long durationMs, List<AdaptationSet> adaptationSets) {
    return new Period(id, startMs, durationMs, adaptationSets);
  }

  public static List<SegmentTimelineElement> parseSegmentTimeline(XmlPullParser xpp)
      throws XmlPullParserException, IOException {
    List<SegmentTimelineElement> segmentTimeline = new ArrayList<>();
    long elapsedTime = 0;
    do {
      xpp.next();
      if (isStartTag(xpp, "S")) {
        elapsedTime = parseLong(xpp, "t", elapsedTime);
        long duration = parseLong(xpp, "d");
        int count = 1 + parseInt(xpp, "r", 0);
        for (int i = 0; i < count; i++) {
          segmentTimeline.add(buildSegmentTimelineElement(elapsedTime, duration));
          elapsedTime += duration;
        }
      }
    } while (!isEndTag(xpp, "SegmentTimeline"));
    return segmentTimeline;
  }

  protected static SegmentTimelineElement buildSegmentTimelineElement(long elapsedTime, long duration) {
    return new SegmentTimelineElement(elapsedTime, duration);
  }

  public static RangedUri parseInitialization(XmlPullParser xpp, String baseUrl) {
    return parseRangedUrl(xpp, baseUrl, "sourceURL", "range");
  }

  public static RangedUri parseSegmentUrl(XmlPullParser xpp, String baseUrl) {
    return parseRangedUrl(xpp, baseUrl, "media", "mediaRange");
  }

  public static RangedUri parseRangedUrl(XmlPullParser xpp, String baseUrl, String urlAttribute,
      String rangeAttribute) {
    String urlText = xpp.getAttributeValue(null, urlAttribute);
    long rangeStart = 0;
    long rangeLength = -1;
    String rangeText = xpp.getAttributeValue(null, rangeAttribute);
    if (rangeText != null) {
      String[] rangeTextArray = rangeText.split("-");
      rangeStart = Long.parseLong(rangeTextArray[0]);
      rangeLength = Long.parseLong(rangeTextArray[1]) - rangeStart + 1;
    }
    return buildRangedUri(baseUrl, urlText, rangeStart, rangeLength);
  }

  public static RangedUri buildRangedUri(String baseUrl, String urlText, long rangeStart,
      long rangeLength) {
    return new RangedUri(baseUrl, urlText, rangeStart, rangeLength);
  }

  // Utility methods.

  public static boolean isEndTag(XmlPullParser xpp, String name) throws XmlPullParserException {
    return xpp.getEventType() == XmlPullParser.END_TAG && name.equals(xpp.getName());
  }

  public static boolean isStartTag(XmlPullParser xpp, String name)
      throws XmlPullParserException {
    return xpp.getEventType() == XmlPullParser.START_TAG && name.equals(xpp.getName());
  }

  public static boolean isStartTag(XmlPullParser xpp) throws XmlPullParserException {
    return xpp.getEventType() == XmlPullParser.START_TAG;
  }

  public static long parseDuration(XmlPullParser xpp, String name, long defaultValue) {
    String value = xpp.getAttributeValue(null, name);
    if (value == null) {
      return defaultValue;
    } else {
      return Util.parseXsDuration(value);
    }
  }

  public static long parseDateTime(XmlPullParser xpp, String name, long defaultValue)
      throws ParseException {
    String value = xpp.getAttributeValue(null, name);
    if (value == null) {
      return defaultValue;
    } else {
      return Util.parseXsDateTime(value);
    }
  }

  public static String parseBaseUrl(XmlPullParser xpp, String parentBaseUrl)
      throws XmlPullParserException, IOException {
    xpp.next();
    return UriUtil.resolve(parentBaseUrl, xpp.getText());
  }

  public static int parseInt(XmlPullParser xpp, String name) {
    return parseInt(xpp, name, -1);
  }

  public static int parseInt(XmlPullParser xpp, String name, int defaultValue) {
    String value = xpp.getAttributeValue(null, name);
    return value == null ? defaultValue : Integer.parseInt(value);
  }

  public static long parseLong(XmlPullParser xpp, String name) {
    return parseLong(xpp, name, -1);
  }

  public static long parseLong(XmlPullParser xpp, String name, long defaultValue) {
    String value = xpp.getAttributeValue(null, name);
    return value == null ? defaultValue : Long.parseLong(value);
  }

  public static String parseString(XmlPullParser xpp, String name, String defaultValue) {
    String value = xpp.getAttributeValue(null, name);
    return value == null ? defaultValue : value;
  }

}
