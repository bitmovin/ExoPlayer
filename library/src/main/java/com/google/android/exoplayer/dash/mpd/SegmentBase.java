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

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.util.Util;

/**
 * An approximate representation of a SegmentBase manifest element.
 */
public abstract class SegmentBase {

  /* package */ final RangedUri initialization;
  /* package */ final long timescale;
  /* package */ final long presentationTimeOffset;

  /**
   * @param initialization A {@link RangedUri} corresponding to initialization data, if such data
   *     exists.
   * @param timescale The timescale in units per second.
   * @param presentationTimeOffset The presentation time offset. The value in seconds is the
   *     division of this value and {@code timescale}.
   */
  public SegmentBase(RangedUri initialization, long timescale, long presentationTimeOffset) {
    this.initialization = initialization;
    this.timescale = timescale;
    this.presentationTimeOffset = presentationTimeOffset;
  }

  /**
   * Gets the {@link RangedUri} defining the location of initialization data for a given
   * representation. May be null if no initialization data exists.
   *
   * @param representation The {@link Representation} for which initialization data is required.
   * @return A {@link RangedUri} defining the location of the initialization data, or null.
   */
  public RangedUri getInitialization(Representation representation) {
    return initialization;
  }

  /**
   * Gets the presentation time offset, in microseconds.
   *
   * @return The presentation time offset, in microseconds.
   */
  public long getPresentationTimeOffsetUs() {
    return Util.scaleLargeTimestamp(presentationTimeOffset, C.MICROS_PER_SECOND, timescale);
  }

}
