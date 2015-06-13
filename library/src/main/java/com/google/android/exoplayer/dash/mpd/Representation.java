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

import android.net.Uri;
import android.text.TextUtils;

import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.chunk.FormatWrapper;
import com.google.android.exoplayer.dash.DashSegmentIndex;
import com.google.android.exoplayer.dash.DashSingleSegmentIndex;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.regex.Matcher;

/**
 * A DASH representation.
 */
public abstract class Representation implements FormatWrapper {

  /**
   * Identifies the piece of content to which this {@link Representation} belongs.
   * <p>
   * For example, all {@link Representation}s belonging to a video should have the same
   * {@link #contentId}, which should uniquely identify that video.
   */
  public final String contentId;

  /**
   * Identifies the revision of the content.
   * <p>
   * If the media for a given ({@link #contentId} can change over time without a change to the
   * {@link #format}'s {@link Format#id} (e.g. as a result of re-encoding the media with an
   * updated encoder), then this identifier must uniquely identify the revision of the media. The
   * timestamp at which the media was encoded is often a suitable.
   */
  public final long revisionId;

  /**
   * The format of the representation.
   */
  public final Format format;

  /**
   * The start time of the enclosing period in milliseconds since the epoch.
   */
  public final long periodStartMs;

  /**
   * The duration of the enclosing period in milliseconds.
   */
  public final long periodDurationMs;

  /**
   * The offset of the presentation timestamps in the media stream relative to media time.
   */
  public final long presentationTimeOffsetUs;

  private final RangedUri initializationUri;

  /**
   * Constructs a new instance.
   *
   * @param periodStartMs The start time of the enclosing period in milliseconds.
   * @param periodDurationMs The duration of the enclosing period in milliseconds, or -1 if the
   *     duration is unknown.
   * @param contentId Identifies the piece of content to which this representation belongs.
   * @param revisionId Identifies the revision of the content.
   * @param format The format of the representation.
   * @param segmentBase A segment base element for the representation.
   * @return The constructed instance.
   */
  public static Representation newInstance(long periodStartMs, long periodDurationMs, String contentId, long revisionId, Format format, SegmentBase segmentBase) {
    if (segmentBase instanceof SingleSegmentBase) {
      return new SingleSegmentRepresentation(periodStartMs, periodDurationMs, contentId, revisionId, format, (SingleSegmentBase) segmentBase, -1);
    } else if (segmentBase instanceof MultiSegmentBase) {
      return new MultiSegmentRepresentation(periodStartMs, periodDurationMs, contentId, revisionId, format, (MultiSegmentBase) segmentBase);
    } else {
      throw new IllegalArgumentException("segmentBase must be of type SingleSegmentBase or MultiSegmentBase");
    }
  }

  public static Representation newInstance(XmlPullParser xpp, String baseUrl, long periodStartMs, long periodDurationMs, String mimeType, String language,
                                           SegmentBase segmentBase, ContentProtectionsBuilder contentProtectionsBuilder, String contentId)
         throws XmlPullParserException, IOException {
    return parse(xpp, baseUrl, periodStartMs, periodDurationMs, mimeType, language, segmentBase, contentProtectionsBuilder, contentId);
  }

  protected Representation(long periodStartMs, long periodDurationMs, String contentId, long revisionId, Format format, SegmentBase segmentBase) {
    this.periodStartMs = periodStartMs;
    this.periodDurationMs = periodDurationMs;
    this.contentId = contentId;
    this.revisionId = revisionId;
    this.format = format;
    initializationUri = segmentBase.getInitialization(this);
    presentationTimeOffsetUs = segmentBase.getPresentationTimeOffsetUs();
  }

  @Override
  public Format getFormat() {
    return format;
  }

  /**
   * Gets a {@link RangedUri} defining the location of the representation's initialization data.
   * May be null if no initialization data exists.
   *
   * @return A {@link RangedUri} defining the location of the initialization data, or null.
   */
  public RangedUri getInitializationUri() {
    return initializationUri;
  }

  /**
   * Gets a {@link RangedUri} defining the location of the representation's segment index. Null if
   * the representation provides an index directly.
   *
   * @return The location of the segment index, or null.
   */
  public abstract RangedUri getIndexUri();

  /**
   * Gets a segment index, if the representation is able to provide one directly. Null if the
   * segment index is defined externally.
   *
   * @return The segment index, or null.
   */
  public abstract DashSegmentIndex getIndex();

  /**
   * Generates a cache key for the {@link Representation}, in the format
   * {@code contentId + "." + format.id + "." + revisionId}.
   *
   * @return A cache key.
   */
  public String getCacheKey() {
    return contentId + "." + format.id + "." + revisionId;
  }

  protected static Representation parse(XmlPullParser xpp, String baseUrl,
                                               long periodStartMs, long periodDurationMs, String mimeType, String language,
                                               SegmentBase segmentBase, ContentProtectionsBuilder contentProtectionsBuilder, String contentId)
      throws XmlPullParserException, IOException {
    String id = xpp.getAttributeValue(null, "id");
    int bandwidth = MediaPresentationDescriptionParser.parseInt(xpp, "bandwidth");
    int audioSamplingRate = MediaPresentationDescriptionParser.parseInt(xpp, "audioSamplingRate");
    int width = MediaPresentationDescriptionParser.parseInt(xpp, "width");
    int height = MediaPresentationDescriptionParser.parseInt(xpp, "height");

    float frameRate = -1;
    String frameRateAttribute = xpp.getAttributeValue(null, "frameRate");
    if (frameRateAttribute != null) {
      Matcher frameRateMatcher = MediaPresentationDescriptionParser.FRAME_RATE_PATTERN.matcher(frameRateAttribute);
      if (frameRateMatcher.matches()) {
        int numerator = Integer.parseInt(frameRateMatcher.group(1));
        String denominatorString = frameRateMatcher.group(2);
        if (!TextUtils.isEmpty(denominatorString)) {
          frameRate = (float) numerator / Integer.parseInt(denominatorString);
        } else {
          frameRate = numerator;
        }
      }
    }

    mimeType = MediaPresentationDescriptionParser.parseString(xpp, "mimeType", mimeType);
    String codecs = MediaPresentationDescriptionParser.parseString(xpp, "codecs", null);

    int numChannels = -1;
    do {
      xpp.next();
      if (MediaPresentationDescriptionParser.isStartTag(xpp, "BaseURL")) {
        baseUrl = MediaPresentationDescriptionParser.parseBaseUrl(xpp, baseUrl);
      } else if (MediaPresentationDescriptionParser.isStartTag(xpp, "AudioChannelConfiguration")) {
        numChannels = Integer.parseInt(xpp.getAttributeValue(null, "value"));
      } else if (MediaPresentationDescriptionParser.isStartTag(xpp, "SegmentBase") ||
                 MediaPresentationDescriptionParser.isStartTag(xpp, "SegmentList") ||
                 MediaPresentationDescriptionParser.isStartTag(xpp, "SegmentTemplate")) {
        segmentBase = SegmentList.createInstanceFromXML(xpp, baseUrl, segmentBase, periodDurationMs);
      } else if (MediaPresentationDescriptionParser.isStartTag(xpp, "ContentProtection")) {
        contentProtectionsBuilder.addRepresentationProtection(new ContentProtection(xpp));
      }
    } while (!MediaPresentationDescriptionParser.isEndTag(xpp, "Representation"));

    Format format = buildFormat(id, mimeType, width, height, frameRate, numChannels,
        audioSamplingRate, bandwidth, language, codecs);

    return buildRepresentation(periodStartMs, periodDurationMs, contentId, -1, format,
        segmentBase != null ? segmentBase : new SingleSegmentBase(baseUrl));
  }

  protected static Format buildFormat(String id, String mimeType, int width, int height, float frameRate,
                               int numChannels, int audioSamplingRate, int bandwidth, String language, String codecs) {
    return new Format(id, mimeType, width, height, frameRate, numChannels, audioSamplingRate,
        bandwidth, language, codecs);
  }

  protected static Representation buildRepresentation(long periodStartMs, long periodDurationMs,
                                               String contentId, int revisionId, Format format, SegmentBase segmentBase) {
    return Representation.newInstance(periodStartMs, periodDurationMs, contentId, revisionId,
        format, segmentBase);
  }
}
