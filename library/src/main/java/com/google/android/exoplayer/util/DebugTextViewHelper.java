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
package com.google.android.exoplayer.util;

import android.widget.TextView;

import com.google.android.exoplayer.CodecCounters;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.upstream.BandwidthMeter;

/**
 * A helper class for periodically updating debug information displayed by a {@link TextView}.
 */
public final class DebugTextViewHelper implements Runnable {
  private static final int REFRESH_INTERVAL_MS = 1000;

  private final TextView textView;
  private final IProvider debuggable;

  /**
   * @param debuggable The {@link com.google.android.exoplayer.util.DebugTextViewHelper.IProvider} from which debug information should be obtained.
   * @param textView The {@link TextView} that should be updated to display the information.
   */
  public DebugTextViewHelper(IProvider debuggable, TextView textView) {
    this.debuggable = debuggable;
    this.textView = textView;
  }

  /**
   * Starts periodic updates of the {@link TextView}.
   * <p>
   * Should be called from the application's main thread.
   */
  public void start() {
    stop();
    run();
  }

  /**
   * Stops periodic updates of the {@link TextView}.
   * <p>
   * Should be called from the application's main thread.
   */
  public void stop() {
    textView.removeCallbacks(this);
  }

  @Override
  public void run() {
    textView.setText(getRenderString());
    textView.postDelayed(this, REFRESH_INTERVAL_MS);
  }

  private String getRenderString() {
    return getTimeString() + " " + getQualityString() + " " + getBandwidthString() + " "
        + getVideoCodecCountersString();
  }

  private String getTimeString() {
    return "ms(" + debuggable.getCurrentPosition() + ")";
  }

  private String getQualityString() {
    Format format = debuggable.getFormat();
    return format == null ? "id:? br:? h:?"
        : "id:" + format.id + " br:" + format.bitrate + " h:" + format.height;
  }

  private String getBandwidthString() {
    BandwidthMeter bandwidthMeter = debuggable.getBandwidthMeter();
    if (bandwidthMeter == null
        || bandwidthMeter.getBitrateEstimate() == BandwidthMeter.NO_ESTIMATE) {
      return "bw:?";
    } else {
      return "bw:" + (bandwidthMeter.getBitrateEstimate() / 1000);
    }
  }

  private String getVideoCodecCountersString() {
    CodecCounters codecCounters = debuggable.getCodecCounters();
    return codecCounters == null ? "" : codecCounters.getDebugString();
  }

}
