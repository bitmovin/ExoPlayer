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

import android.util.Base64;

import com.google.android.exoplayer.util.Assertions;
import com.google.android.exoplayer.util.ParsableByteArray;
import com.google.android.exoplayer.util.Util;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Represents a ContentProtection tag in an AdaptationSet.
 */
public class ContentProtection {

  /**
   * Identifies the content protection scheme.
   */
  public String schemeUriId;

  /**
   * The UUID of the protection scheme. May be null.
   */
  public UUID uuid;

  /**
   * Protection scheme specific data. May be null.
   */
  public byte[] data;

  /**
   * @param schemeUriId Identifies the content protection scheme.
   * @param uuid The UUID of the protection scheme, if known. May be null.
   * @param data Protection scheme specific initialization data. May be null.
   */
  public ContentProtection(String schemeUriId, UUID uuid, byte[] data) {
    init(schemeUriId, uuid, data);
  }

  public ContentProtection(XmlPullParser xpp) throws XmlPullParserException, IOException {
    parseContentProtection(xpp);
  }

  private void init(String schemeUriId, UUID uuid, byte[] data) {
    this.schemeUriId = Assertions.checkNotNull(schemeUriId);
    this.uuid = uuid;
    this.data = data;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ContentProtection)) {
      return false;
    }
    if (obj == this) {
      return true;
    }

    ContentProtection other = (ContentProtection) obj;
    return schemeUriId.equals(other.schemeUriId)
        && Util.areEqual(uuid, other.uuid)
        && Arrays.equals(data, other.data);
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 37 + schemeUriId.hashCode();
    if (uuid != null) {
      hashCode = hashCode * 37 + uuid.hashCode();
    }
    if (data != null) {
      hashCode = hashCode * 37 + Arrays.hashCode(data);
    }
    return hashCode;
  }

  /**
   * Parses a ContentProtection element.
   *
   * @throws org.xmlpull.v1.XmlPullParserException If an error occurs parsing the element.
   * @throws java.io.IOException If an error occurs reading the element.
   **/
  protected void parseContentProtection(XmlPullParser xpp)
      throws XmlPullParserException, IOException {
    String schemeIdUri = xpp.getAttributeValue(null, "schemeIdUri");
    UUID uuid = null;
    byte[] data = null;
    do {
      xpp.next();
      // The cenc:pssh element is defined in 23001-7:2015
      if (MediaPresentationDescriptionParser.isStartTag(xpp, "cenc:pssh") && xpp.next() == XmlPullParser.TEXT) {
        byte[] decodedData = Base64.decode(xpp.getText(), Base64.DEFAULT);
        ParsableByteArray psshAtom = new ParsableByteArray(decodedData);
        psshAtom.skipBytes(12);
        uuid = new UUID(psshAtom.readLong(), psshAtom.readLong());
        int dataSize = psshAtom.readInt();
        data = new byte[dataSize];
        psshAtom.readBytes(data, 0, dataSize);
      }
    } while (!MediaPresentationDescriptionParser.isEndTag(xpp, "ContentProtection"));

    init(schemeIdUri, uuid, data);
  }

}
