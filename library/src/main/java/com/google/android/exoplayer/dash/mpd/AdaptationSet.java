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

import android.text.TextUtils;

import com.google.android.exoplayer.util.Assertions;
import com.google.android.exoplayer.util.MimeTypes;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a set of interchangeable encoded versions of a media content component.
 */
public class AdaptationSet {

  public static final int TYPE_UNKNOWN = -1;
  public static final int TYPE_VIDEO = 0;
  public static final int TYPE_AUDIO = 1;
  public static final int TYPE_TEXT = 2;

  public int id;

  public int type;

  public List<Representation> representations;
  public List<ContentProtection> contentProtections;

  public AdaptationSet(XmlPullParser xpp, String baseUrl, long periodStartMs,
  long periodDurationMs, SegmentBase segmentBase, String contentId) throws XmlPullParserException, IOException{
    parseAdaptationSet(xpp, baseUrl, periodStartMs, periodDurationMs, segmentBase, contentId);
  }

  public AdaptationSet(int id, int type, List<Representation> representations,
      List<ContentProtection> contentProtections) {
    init(id, type, representations, contentProtections);
  }

  public AdaptationSet(int id, int type, List<Representation> representations) {
    this(id, type, representations, null);
  }

  private void init(int id, int type, List<Representation> representations,
                    List<ContentProtection> contentProtections) {
    this.id = id;
    this.type = type;
    this.representations = Collections.unmodifiableList(representations);
    if (contentProtections == null) {
      this.contentProtections = Collections.emptyList();
    } else {
      this.contentProtections = Collections.unmodifiableList(contentProtections);
    }
  }

  public boolean hasContentProtection() {
    return !contentProtections.isEmpty();
  }

  protected void parseAdaptationSet(XmlPullParser xpp, String baseUrl, long periodStartMs,
                                             long periodDurationMs, SegmentBase segmentBase, String contentId) throws XmlPullParserException, IOException {

    String mimeType = xpp.getAttributeValue(null, "mimeType");
    String language = xpp.getAttributeValue(null, "lang");
    int contentType = parseAdaptationSetTypeFromMimeType(mimeType);

    int id = -1;
    ContentProtectionsBuilder contentProtectionsBuilder = new ContentProtectionsBuilder();
    List<Representation> representations = new ArrayList<>();
    do {
      xpp.next();
      if (MediaPresentationDescriptionParser.isStartTag(xpp, "BaseURL")) {
        baseUrl = MediaPresentationDescriptionParser.parseBaseUrl(xpp, baseUrl);
      } else if (MediaPresentationDescriptionParser.isStartTag(xpp, "ContentProtection")) {
        contentProtectionsBuilder.addAdaptationSetProtection(new ContentProtection(xpp));
      } else if (MediaPresentationDescriptionParser.isStartTag(xpp, "ContentComponent")) {
        id = Integer.parseInt(xpp.getAttributeValue(null, "id"));
        contentType = checkAdaptationSetTypeConsistency(contentType,
                parseAdaptationSetType(xpp.getAttributeValue(null, "contentType")));
      } else if (MediaPresentationDescriptionParser.isStartTag(xpp, "Representation")) {
        Representation representation = Representation.newInstance(xpp, baseUrl, periodStartMs,
                periodDurationMs, mimeType, language, segmentBase, contentProtectionsBuilder, contentId);
        contentProtectionsBuilder.endRepresentation();
        contentType = checkAdaptationSetTypeConsistency(contentType,
                parseAdaptationSetTypeFromMimeType(representation.format.mimeType));
        representations.add(representation);
      } else if (MediaPresentationDescriptionParser.isStartTag(xpp, "SegmentBase")
          || MediaPresentationDescriptionParser.isStartTag(xpp, "SegmentList")
          || MediaPresentationDescriptionParser.isStartTag(xpp, "SegmentTemplate")) {
        segmentBase = SegmentList.createInstanceFromXML(xpp, baseUrl, segmentBase, periodDurationMs);
      } else if (MediaPresentationDescriptionParser.isStartTag(xpp)) {
        parseAdaptationSetChild(xpp);
      }
    } while (!MediaPresentationDescriptionParser.isEndTag(xpp, "AdaptationSet"));

    init(id, contentType, representations, contentProtectionsBuilder.build());
  }

  protected int parseAdaptationSetType(String contentType) {
    return TextUtils.isEmpty(contentType) ? AdaptationSet.TYPE_UNKNOWN
        : MimeTypes.BASE_TYPE_AUDIO.equals(contentType) ? AdaptationSet.TYPE_AUDIO
        : MimeTypes.BASE_TYPE_VIDEO.equals(contentType) ? AdaptationSet.TYPE_VIDEO
        : MimeTypes.BASE_TYPE_TEXT.equals(contentType) ? AdaptationSet.TYPE_TEXT
        : AdaptationSet.TYPE_UNKNOWN;
  }

  protected int parseAdaptationSetTypeFromMimeType(String mimeType) {
    return TextUtils.isEmpty(mimeType) ? AdaptationSet.TYPE_UNKNOWN
        : MimeTypes.isAudio(mimeType) ? AdaptationSet.TYPE_AUDIO
        : MimeTypes.isVideo(mimeType) ? AdaptationSet.TYPE_VIDEO
        : MimeTypes.isText(mimeType) || MimeTypes.isTtml(mimeType) ? AdaptationSet.TYPE_TEXT
        : AdaptationSet.TYPE_UNKNOWN;
  }

  /**
   * Checks two adaptation set types for consistency, returning the consistent type, or throwing an
   * {@link IllegalStateException} if the types are inconsistent.
   * <p>
   * Two types are consistent if they are equal, or if one is {@link AdaptationSet#TYPE_UNKNOWN}.
   * Where one of the types is {@link AdaptationSet#TYPE_UNKNOWN}, the other is returned.
   *
   * @param firstType The first type.
   * @param secondType The second type.
   * @return The consistent type.
   */
  private int checkAdaptationSetTypeConsistency(int firstType, int secondType) {
    if (firstType == AdaptationSet.TYPE_UNKNOWN) {
      return secondType;
    } else if (secondType == AdaptationSet.TYPE_UNKNOWN) {
      return firstType;
    } else {
      Assertions.checkState(firstType == secondType);
      return firstType;
    }
  }

  /**
   * Parses children of AdaptationSet elements not specifically parsed elsewhere.
   *
   * @param xpp The XmpPullParser from which the AdaptationSet child should be parsed.
   * @throws XmlPullParserException If an error occurs parsing the element.
   * @throws IOException If an error occurs reading the element.
   **/
  protected void parseAdaptationSetChild(XmlPullParser xpp)
      throws XmlPullParserException, IOException {
    // pass
  }

}
