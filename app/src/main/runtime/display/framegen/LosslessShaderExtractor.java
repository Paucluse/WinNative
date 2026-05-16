package com.winlator.cmod.runtime.display.framegen;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;

public final class LosslessShaderExtractor {
  public static final String PREF_SHADERS_READY = "superframe_lossless_shaders_ready";
  public static final String PREF_LAST_STATUS = "superframe_lossless_last_status";

  public static final class ExtractResult {
    public final boolean success;
    public final String message;

    public ExtractResult(boolean success, String message) {
      this.success = success;
      this.message = message;
    }
  }

  private LosslessShaderExtractor() {}

  public static File getShaderCacheDir(Context context) {
    return new File(context.getFilesDir(), "framegen/spirv");
  }

  public static boolean isShaderCacheReady(SharedPreferences preferences) {
    return preferences.getBoolean(PREF_SHADERS_READY, false);
  }

  public static String getLastStatus(SharedPreferences preferences) {
    return preferences.getString(PREF_LAST_STATUS, "Lossless shaders have not been prepared yet.");
  }

  public static void persistResult(SharedPreferences preferences, ExtractResult result) {
    preferences
        .edit()
        .putBoolean(PREF_SHADERS_READY, result.success)
        .putString(PREF_LAST_STATUS, result.message)
        .apply();
  }

  public static ExtractResult extractAndProbe(Context context) {
    File dllFile = LosslessDllManager.getDllFile(context);
    if (!dllFile.isFile()) {
      return new ExtractResult(false, "Lossless.dll has not been imported.");
    }

    File cacheDir = getShaderCacheDir(context);
    if (!cacheDir.exists() && !cacheDir.mkdirs()) {
      return new ExtractResult(false, "Failed to create shader cache directory.");
    }

    int extractCode =
        FrameGenerationBridge.extractLosslessShaders(
            dllFile.getAbsolutePath(), cacheDir.getAbsolutePath());
    if (extractCode != 0) {
      return new ExtractResult(false, describe(extractCode));
    }

    int probeCode = FrameGenerationBridge.probeLosslessShaders(cacheDir.getAbsolutePath());
    if (probeCode != 0) {
      return new ExtractResult(false, describe(probeCode));
    }

    return new ExtractResult(true, "Lossless shaders extracted and verified.");
  }

  private static String describe(int code) {
    switch (code) {
      case 0:
        return "OK";
      case -1:
        return "The selected file could not be parsed as a valid Lossless.dll.";
      case -2:
        return "Lossless.dll is missing required frame-generation shader resources.";
      case -3:
        return "DXBC to SPIR-V translation failed for at least one Lossless shader.";
      case -4:
        return "Failed to write the extracted shader cache.";
      case -10:
        return "No Vulkan loader is available on this device.";
      case -11:
        return "The shader cache is incomplete or malformed.";
      case -12:
        return "The device Vulkan driver rejected one or more extracted shaders.";
      case -100:
        return "Native LSFG bridge is not wired into this build yet.";
      default:
        return "Frame-generation preparation failed (" + code + ").";
    }
  }
}
