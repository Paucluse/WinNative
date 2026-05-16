package com.winlator.cmod.runtime.display.framegen;

import android.util.Log;
import com.winlator.cmod.runtime.display.renderer.GPUImage;
import com.winlator.cmod.runtime.display.renderer.Texture;

public class FrameGenerationCaptureController {
  private static final String TAG = "FrameGenCapture";

  public static final class OutputState {
    public final boolean ready;
    public final int width;
    public final int height;
    public final long generation;

    public OutputState(boolean ready, int width, int height, long generation) {
      this.ready = ready;
      this.width = width;
      this.height = height;
      this.generation = generation;
    }
  }

  private Texture previousFrameTexture = new Texture();
  private Texture currentFrameTexture = new Texture();
  private final FrameGenerationBridge frameGenerationBridge = new FrameGenerationBridge();
  private boolean enabled = false;
  private boolean hasPreviousFrame = false;
  private int frameWidth = 0;
  private int frameHeight = 0;
  private long capturedFrameCount = 0L;
  private boolean hasReadyOutput = false;
  private int outputWidth = 0;
  private int outputHeight = 0;
  private long outputGeneration = 0L;
  private boolean losslessRenderLoopActive = false;

  public synchronized void setEnabled(boolean enabled) {
    if (this.enabled == enabled) return;
    this.enabled = enabled;
    if (!enabled) {
      reset();
    }
  }

  public synchronized boolean isEnabled() {
    return enabled;
  }

  public synchronized void setLosslessRenderLoopActive(boolean active) {
    losslessRenderLoopActive = active;
    if (!active) {
      hasReadyOutput = false;
      outputWidth = 0;
      outputHeight = 0;
      outputGeneration = 0L;
    }
  }

  public synchronized void onSurfaceChanged(int width, int height) {
    if (width != frameWidth || height != frameHeight) {
      frameWidth = width;
      frameHeight = height;
      reset();
    }
  }

  public synchronized void capturePresentedFrame(int framebuffer, int width, int height) {
    if (!enabled || width <= 0 || height <= 0) return;

    if (width != frameWidth || height != frameHeight) {
      frameWidth = width;
      frameHeight = height;
      reset();
    }

    if (!(currentFrameTexture instanceof GPUImage) && GPUImage.isSupported()) {
      reset();
    }

    Texture oldPrevious = previousFrameTexture;
    previousFrameTexture = currentFrameTexture;
    currentFrameTexture = oldPrevious;
    currentFrameTexture.copyFromFramebuffer(framebuffer, (short) width, (short) height);

    hasPreviousFrame = previousFrameTexture.isAllocated();
    capturedFrameCount++;

    if (hasFramePair()) {
      long previousHardwareBufferPtr =
          previousFrameTexture instanceof GPUImage
              ? ((GPUImage) previousFrameTexture).getHardwareBufferPtr()
              : 0L;
      long currentHardwareBufferPtr =
          currentFrameTexture instanceof GPUImage
              ? ((GPUImage) currentFrameTexture).getHardwareBufferPtr()
              : 0L;
      if (losslessRenderLoopActive && currentHardwareBufferPtr != 0L) {
        FrameGenerationBridge.pushLosslessFrame(currentHardwareBufferPtr, System.nanoTime());
      } else {
        frameGenerationBridge.submitFramePair(
            previousHardwareBufferPtr,
            currentHardwareBufferPtr,
            previousFrameTexture.getTextureId(),
            currentFrameTexture.getTextureId(),
            frameWidth,
            frameHeight,
            capturedFrameCount);
      }
      refreshOutputState();
    }
  }

  public synchronized boolean hasFramePair() {
    return enabled && hasPreviousFrame && currentFrameTexture.isAllocated();
  }

  public synchronized int getPreviousFrameTextureId() {
    return previousFrameTexture.getTextureId();
  }

  public synchronized int getCurrentFrameTextureId() {
    return currentFrameTexture.getTextureId();
  }

  public synchronized int getFrameWidth() {
    return frameWidth;
  }

  public synchronized int getFrameHeight() {
    return frameHeight;
  }

  public synchronized long getCapturedFrameCount() {
    return capturedFrameCount;
  }

  public synchronized boolean hasReadyOutput() {
    return hasReadyOutput;
  }

  public synchronized int getOutputWidth() {
    return outputWidth;
  }

  public synchronized int getOutputHeight() {
    return outputHeight;
  }

  public synchronized long getOutputGeneration() {
    return outputGeneration;
  }

  public synchronized OutputState getOutputState() {
    return new OutputState(hasReadyOutput, outputWidth, outputHeight, outputGeneration);
  }

  public synchronized void destroy() {
    frameGenerationBridge.destroy();
    previousFrameTexture.destroy();
    currentFrameTexture.destroy();
    hasPreviousFrame = false;
    hasReadyOutput = false;
    outputWidth = 0;
    outputHeight = 0;
    outputGeneration = 0L;
  }

  private void reset() {
    destroy();
    previousFrameTexture = createCaptureTexture();
    currentFrameTexture = createCaptureTexture();
    capturedFrameCount = 0L;
  }

  private void refreshOutputState() {
    if (losslessRenderLoopActive) {
      hasReadyOutput = FrameGenerationBridge.getLosslessGeneratedFrameCount() > 0L;
      outputWidth = frameWidth;
      outputHeight = frameHeight;
      outputGeneration = FrameGenerationBridge.getLosslessPostedFrameCount();
      return;
    }
    boolean readyOutput = frameGenerationBridge.hasReadyOutput();
    int[] outputDimensions =
        readyOutput ? frameGenerationBridge.getOutputDimensions() : new int[] {0, 0};
    long latestOutputGeneration = readyOutput ? frameGenerationBridge.getOutputGeneration() : 0L;
    hasReadyOutput = readyOutput;
    outputWidth = outputDimensions.length > 0 ? outputDimensions[0] : 0;
    outputHeight = outputDimensions.length > 1 ? outputDimensions[1] : 0;
    outputGeneration = latestOutputGeneration;

    if ((capturedFrameCount % 120L) == 0L) {
      Log.d(
          TAG,
          "captureFrames="
              + capturedFrameCount
              + " framePair="
              + hasFramePair()
              + " outputReady="
              + hasReadyOutput
              + " output="
              + outputWidth
              + "x"
              + outputHeight
              + " generation="
              + outputGeneration);
    }
  }

  private Texture createCaptureTexture() {
    if (GPUImage.isSupported() && frameWidth > 0 && frameHeight > 0) {
      return new GPUImage((short) frameWidth, (short) frameHeight, false);
    }
    return new Texture();
  }
}
