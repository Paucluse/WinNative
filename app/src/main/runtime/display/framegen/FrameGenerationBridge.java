package com.winlator.cmod.runtime.display.framegen;

import android.util.Log;
import android.view.Surface;

/** Native bridge for the Lossless/LSFG render loop integration. */
public class FrameGenerationBridge {
  private static final String TAG = "FrameGenBridge";
  private final boolean backendAvailable = safeIsFrameGenerationBackendAvailable();
  private long sessionPtr = 0L;

  public static synchronized boolean configureUserLosslessDll(String dllPath) {
    try {
      return nativeConfigureUserLosslessDll(dllPath);
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "configureUserLosslessDll unavailable", e);
      return false;
    }
  }

  public static synchronized boolean hasConfiguredUserLosslessDll() {
    try {
      return nativeHasConfiguredUserLosslessDll();
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "hasConfiguredUserLosslessDll unavailable", e);
      return false;
    }
  }

  public static synchronized String getFrameGenerationBackendName() {
    try {
      String backendName = nativeGetFrameGenerationBackendName();
      return backendName != null ? backendName : "unknown";
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "getFrameGenerationBackendName unavailable", e);
      return "unavailable";
    }
  }

  public static synchronized int extractLosslessShaders(String dllPath, String cacheDir) {
    try {
      return nativeExtractLosslessShaders(dllPath, cacheDir);
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "extractLosslessShaders unavailable", e);
      return -100;
    }
  }

  public static synchronized int probeLosslessShaders(String cacheDir) {
    try {
      return nativeProbeLosslessShaders(cacheDir);
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "probeLosslessShaders unavailable", e);
      return -100;
    }
  }

  public static synchronized boolean isFramegenFp16Supported(String cacheDir) {
    try {
      return nativeIsFramegenFp16Supported(cacheDir);
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "isFramegenFp16Supported unavailable", e);
      return false;
    }
  }

  public static synchronized int initLosslessRenderLoop(
      String cacheDir,
      int width,
      int height,
      int multiplier,
      float flowScale,
      boolean performance,
      boolean hdr,
      boolean antiArtifacts,
      boolean framegenFp16,
      int targetFpsCap,
      float emaAlpha,
      float outlierRatio,
      float vsyncSlackMs,
      int queueDepth) {
    try {
      return nativeInitLosslessRenderLoop(
          cacheDir,
          width,
          height,
          multiplier,
          flowScale,
          performance,
          hdr,
          antiArtifacts,
          framegenFp16,
          targetFpsCap,
          emaAlpha,
          outlierRatio,
          vsyncSlackMs,
          queueDepth);
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "initLosslessRenderLoop unavailable", e);
      return -100;
    }
  }

  public static synchronized void shutdownLosslessRenderLoop() {
    try {
      nativeShutdownLosslessRenderLoop();
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "shutdownLosslessRenderLoop unavailable", e);
    }
  }

  public static synchronized void setLosslessOutputSurface(
      Surface surface, int width, int height) {
    try {
      nativeSetLosslessOutputSurface(surface, width, height);
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "setLosslessOutputSurface unavailable", e);
    }
  }

  public static synchronized void setLosslessVsyncPeriodNs(long periodNs) {
    try {
      nativeSetLosslessVsyncPeriodNs(periodNs);
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "setLosslessVsyncPeriodNs unavailable", e);
    }
  }

  public static synchronized void setLosslessPacingParams(
      int targetFpsCap,
      float emaAlpha,
      float outlierRatio,
      float vsyncSlackMs,
      int queueDepth) {
    try {
      nativeSetLosslessPacingParams(
          targetFpsCap, emaAlpha, outlierRatio, vsyncSlackMs, queueDepth);
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "setLosslessPacingParams unavailable", e);
    }
  }

  public static synchronized void pushLosslessFrame(long hardwareBufferPtr, long timestampNs) {
    try {
      nativePushLosslessFrame(hardwareBufferPtr, timestampNs);
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "pushLosslessFrame unavailable", e);
    }
  }

  public static synchronized long getLosslessGeneratedFrameCount() {
    try {
      return nativeGetLosslessGeneratedFrameCount();
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "getLosslessGeneratedFrameCount unavailable", e);
      return 0L;
    }
  }

  public static synchronized long getLosslessPostedFrameCount() {
    try {
      return nativeGetLosslessPostedFrameCount();
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "getLosslessPostedFrameCount unavailable", e);
      return 0L;
    }
  }

  public static synchronized long getLosslessUniqueCaptureCount() {
    try {
      return nativeGetLosslessUniqueCaptureCount();
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "getLosslessUniqueCaptureCount unavailable", e);
      return 0L;
    }
  }

  public synchronized boolean isBackendAvailable() {
    return backendAvailable;
  }

  public synchronized boolean ensureSession() {
    if (!backendAvailable) return false;
    if (sessionPtr != 0L) return true;
    sessionPtr = safeCreateFrameGenerationSession();
    return sessionPtr != 0L;
  }

  public synchronized boolean submitFramePair(
      long previousHardwareBufferPtr,
      long currentHardwareBufferPtr,
      int previousTextureId,
      int currentTextureId,
      int width,
      int height,
      long frameIndex) {
    if (!ensureSession()) return false;
    try {
      return nativeSubmitFramePair(
          sessionPtr,
          previousHardwareBufferPtr,
          currentHardwareBufferPtr,
          previousTextureId,
          currentTextureId,
          width,
          height,
          frameIndex);
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "submitFramePair unavailable", e);
      return false;
    }
  }

  public synchronized boolean hasReadyOutput() {
    if (sessionPtr == 0L) return false;
    try {
      return nativeHasReadyOutput(sessionPtr);
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "hasReadyOutput unavailable", e);
      return false;
    }
  }

  public synchronized int[] getOutputDimensions() {
    if (sessionPtr == 0L) return new int[] {0, 0};
    try {
      return nativeGetOutputDimensions(sessionPtr);
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "getOutputDimensions unavailable", e);
      return new int[] {0, 0};
    }
  }

  public synchronized long getOutputGeneration() {
    if (sessionPtr == 0L) return 0L;
    try {
      return nativeGetOutputGeneration(sessionPtr);
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "getOutputGeneration unavailable", e);
      return 0L;
    }
  }

  public synchronized void destroy() {
    if (sessionPtr != 0L) {
      try {
        nativeDestroyFrameGenerationSession(sessionPtr);
      } catch (UnsatisfiedLinkError e) {
        Log.w(TAG, "destroy unavailable", e);
      }
      sessionPtr = 0L;
    }
  }

  private static boolean safeIsFrameGenerationBackendAvailable() {
    try {
      return nativeIsFrameGenerationBackendAvailable();
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "native backend availability probe unavailable", e);
      return false;
    }
  }

  private static long safeCreateFrameGenerationSession() {
    try {
      return nativeCreateFrameGenerationSession();
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, "native create session unavailable", e);
      return 0L;
    }
  }

  private static native boolean nativeIsFrameGenerationBackendAvailable();

  private static native long nativeCreateFrameGenerationSession();

  private static native void nativeDestroyFrameGenerationSession(long sessionPtr);

  private static native boolean nativeSubmitFramePair(
      long sessionPtr,
      long previousHardwareBufferPtr,
      long currentHardwareBufferPtr,
      int previousTextureId,
      int currentTextureId,
      int width,
      int height,
      long frameIndex);

  private static native boolean nativeHasReadyOutput(long sessionPtr);

  private static native int[] nativeGetOutputDimensions(long sessionPtr);

  private static native long nativeGetOutputGeneration(long sessionPtr);

  private static native boolean nativeConfigureUserLosslessDll(String dllPath);

  private static native boolean nativeHasConfiguredUserLosslessDll();

  private static native String nativeGetFrameGenerationBackendName();

  private static native int nativeExtractLosslessShaders(String dllPath, String cacheDir);

  private static native int nativeProbeLosslessShaders(String cacheDir);

  private static native boolean nativeIsFramegenFp16Supported(String cacheDir);

  private static native int nativeInitLosslessRenderLoop(
      String cacheDir,
      int width,
      int height,
      int multiplier,
      float flowScale,
      boolean performance,
      boolean hdr,
      boolean antiArtifacts,
      boolean framegenFp16,
      int targetFpsCap,
      float emaAlpha,
      float outlierRatio,
      float vsyncSlackMs,
      int queueDepth);

  private static native void nativeShutdownLosslessRenderLoop();

  private static native void nativeSetLosslessOutputSurface(
      Surface surface, int width, int height);

  private static native void nativeSetLosslessVsyncPeriodNs(long periodNs);

  private static native void nativeSetLosslessPacingParams(
      int targetFpsCap,
      float emaAlpha,
      float outlierRatio,
      float vsyncSlackMs,
      int queueDepth);

  private static native void nativePushLosslessFrame(long hardwareBufferPtr, long timestampNs);

  private static native long nativeGetLosslessGeneratedFrameCount();

  private static native long nativeGetLosslessPostedFrameCount();

  private static native long nativeGetLosslessUniqueCaptureCount();
}
