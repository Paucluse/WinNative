package com.winlator.cmod.runtime.display.framegen;

import android.content.SharedPreferences;

public final class FrameGenerationConfig {
  public static final String PREF_ENABLED = "enable_superframe";
  public static final String PREF_PRESET = "superframe_preset";
  public static final String PREF_MULTIPLIER = "superframe_multiplier";
  public static final String PREF_DIAGNOSTICS = "superframe_diagnostics";
  public static final String PREF_PREVIEW_SCALE = "superframe_preview_scale";
  public static final String PREF_FLOW_SCALE = "superframe_flow_scale";
  public static final String PREF_PERFORMANCE_MODE = "superframe_performance_mode";
  public static final String PREF_ANTI_ARTIFACTS = "superframe_anti_artifacts";
  public static final String PREF_FRAMEGEN_FP16 = "superframe_framegen_fp16";
  public static final String PREF_TARGET_FPS_CAP = "superframe_target_fps_cap";
  public static final String PREF_EMA_ALPHA = "superframe_ema_alpha";
  public static final String PREF_OUTLIER_RATIO = "superframe_outlier_ratio";
  public static final String PREF_VSYNC_SLACK_MS = "superframe_vsync_slack_ms";
  public static final String PREF_QUEUE_DEPTH = "superframe_queue_depth";
  public static final String PREF_SESSION_DIRTY = "superframe_session_dirty";

  public static final int PRESET_BALANCED = 0;
  public static final int PRESET_QUALITY = 1;
  public static final int PRESET_PERFORMANCE = 2;

  public static final int DEFAULT_PRESET = PRESET_BALANCED;
  public static final int DEFAULT_MULTIPLIER = 2;
  public static final boolean DEFAULT_DIAGNOSTICS = true;
  public static final float DEFAULT_PREVIEW_SCALE = 0.28f;
  public static final float DEFAULT_FLOW_SCALE = 0.70f;
  public static final boolean DEFAULT_PERFORMANCE_MODE = false;
  public static final boolean DEFAULT_ANTI_ARTIFACTS = true;
  public static final boolean DEFAULT_FRAMEGEN_FP16 = false;
  public static final int DEFAULT_TARGET_FPS_CAP = 0;
  public static final float DEFAULT_EMA_ALPHA = 0.125f;
  public static final float DEFAULT_OUTLIER_RATIO = 4.0f;
  public static final float DEFAULT_VSYNC_SLACK_MS = 2.0f;
  public static final int DEFAULT_QUEUE_DEPTH = 4;

  public final boolean enabled;
  public final int preset;
  public final int multiplier;
  public final boolean diagnosticsEnabled;
  public final float previewScale;
  public final float flowScale;
  public final boolean performanceMode;
  public final boolean antiArtifacts;
  public final boolean framegenFp16;
  public final int targetFpsCap;
  public final float emaAlpha;
  public final float outlierRatio;
  public final float vsyncSlackMs;
  public final int queueDepth;

  public FrameGenerationConfig(
      boolean enabled,
      int preset,
      int multiplier,
      boolean diagnosticsEnabled,
      float previewScale,
      float flowScale,
      boolean performanceMode,
      boolean antiArtifacts,
      boolean framegenFp16,
      int targetFpsCap,
      float emaAlpha,
      float outlierRatio,
      float vsyncSlackMs,
      int queueDepth) {
    this.enabled = enabled;
    this.preset = preset;
    this.multiplier = multiplier;
    this.diagnosticsEnabled = diagnosticsEnabled;
    this.previewScale = previewScale;
    this.flowScale = flowScale;
    this.performanceMode = performanceMode;
    this.antiArtifacts = antiArtifacts;
    this.framegenFp16 = framegenFp16;
    this.targetFpsCap = targetFpsCap;
    this.emaAlpha = emaAlpha;
    this.outlierRatio = outlierRatio;
    this.vsyncSlackMs = vsyncSlackMs;
    this.queueDepth = queueDepth;
  }

  public static FrameGenerationConfig fromPreferences(SharedPreferences preferences) {
    boolean enabled = preferences.getBoolean(PREF_ENABLED, false);
    int preset = clampPreset(preferences.getInt(PREF_PRESET, DEFAULT_PRESET));
    int multiplier = clampMultiplier(preferences.getInt(PREF_MULTIPLIER, DEFAULT_MULTIPLIER));
    boolean diagnosticsEnabled =
        preferences.getBoolean(PREF_DIAGNOSTICS, DEFAULT_DIAGNOSTICS);
    float previewScale =
        clampPreviewScale(preferences.getFloat(PREF_PREVIEW_SCALE, DEFAULT_PREVIEW_SCALE));
    float flowScale = clampFlowScale(preferences.getFloat(PREF_FLOW_SCALE, DEFAULT_FLOW_SCALE));
    boolean performanceMode =
        preferences.getBoolean(
            PREF_PERFORMANCE_MODE,
            preset == PRESET_PERFORMANCE ? true : DEFAULT_PERFORMANCE_MODE);
    boolean antiArtifacts =
        preferences.getBoolean(PREF_ANTI_ARTIFACTS, DEFAULT_ANTI_ARTIFACTS);
    boolean framegenFp16 =
        preferences.getBoolean(PREF_FRAMEGEN_FP16, DEFAULT_FRAMEGEN_FP16);
    int targetFpsCap =
        clampTargetFpsCap(preferences.getInt(PREF_TARGET_FPS_CAP, DEFAULT_TARGET_FPS_CAP));
    float emaAlpha =
        clampEmaAlpha(preferences.getFloat(PREF_EMA_ALPHA, DEFAULT_EMA_ALPHA));
    float outlierRatio =
        clampOutlierRatio(preferences.getFloat(PREF_OUTLIER_RATIO, DEFAULT_OUTLIER_RATIO));
    float vsyncSlackMs =
        clampVsyncSlackMs(
            preferences.getFloat(PREF_VSYNC_SLACK_MS, DEFAULT_VSYNC_SLACK_MS));
    int queueDepth =
        clampQueueDepth(preferences.getInt(PREF_QUEUE_DEPTH, DEFAULT_QUEUE_DEPTH));

    return new FrameGenerationConfig(
        enabled,
        preset,
        multiplier,
        diagnosticsEnabled,
        previewScale,
        flowScale,
        performanceMode,
        antiArtifacts,
        framegenFp16,
        targetFpsCap,
        emaAlpha,
        outlierRatio,
        vsyncSlackMs,
        queueDepth);
  }

  public static int clampPreset(int preset) {
    return Math.max(PRESET_BALANCED, Math.min(PRESET_PERFORMANCE, preset));
  }

  public static int clampMultiplier(int multiplier) {
    return Math.max(2, Math.min(4, multiplier));
  }

  public static float clampPreviewScale(float previewScale) {
    return Math.max(0.18f, Math.min(0.5f, previewScale));
  }

  public static float clampFlowScale(float flowScale) {
    return Math.max(0.25f, Math.min(1.0f, flowScale));
  }

  public static int clampTargetFpsCap(int fpsCap) {
    if (fpsCap <= 0) {
      return 0;
    }
    return Math.max(30, Math.min(240, fpsCap));
  }

  public static float clampEmaAlpha(float value) {
    return Math.max(0.05f, Math.min(0.5f, value));
  }

  public static float clampOutlierRatio(float value) {
    return Math.max(2.0f, Math.min(8.0f, value));
  }

  public static float clampVsyncSlackMs(float value) {
    return Math.max(1.0f, Math.min(5.0f, value));
  }

  public static int clampQueueDepth(int value) {
    return Math.max(2, Math.min(6, value));
  }
}
