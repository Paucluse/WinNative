package com.winlator.cmod.runtime.display.framegen;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.winlator.cmod.runtime.container.Container;
import com.winlator.cmod.runtime.wine.EnvVars;
import com.winlator.cmod.shared.io.FileUtils;
import java.io.File;
import java.util.Locale;

public final class LsfgVkManager {
  public static final String EXTRA_ARMED = "lsfgEnabled";
  public static final String EXTRA_MULTIPLIER = "lsfgMultiplier";
  public static final String EXTRA_FLOW_SCALE = "lsfgFlowScale";
  public static final String EXTRA_PERFORMANCE_MODE = "lsfgPerformanceMode";

  private static final String ENV_CONFIG = "LSFG_CONFIG";
  private static final String ENV_PROCESS = "LSFG_PROCESS";
  private static final String PROCESS_EXE_IDENTIFIER = "winnative-lsfg";
  private static final String LIB_FILENAME = "liblsfg-vk-layer.so";
  private static final String MANIFEST_FILENAME = "VkLayer_LS_frame_generation.json";
  private static final String DLL_FILENAME = "Lossless.dll";
  private static final String RUNTIME_VERSION = "winnative-v0.1";
  private static final String VERSION_FILENAME = ".lsfg_vk_runtime_version";
  private static final String LIB_RELATIVE_DIR = ".local/lib";
  private static final String LAYER_RELATIVE_DIR = ".local/share/vulkan/implicit_layer.d";
  private static final String DLL_RELATIVE_DIR = ".local/share/lsfg-vk";
  private static final String CONFIG_RELATIVE_PATH = ".config/lsfg-vk/conf.toml";

  private LsfgVkManager() {}

  public static boolean isSupported(@Nullable Container container) {
    return container != null && resolveRootDir(container) != null;
  }

  public static void persistSettings(
      @Nullable Container container, @NonNull FrameGenerationConfig config, boolean dllImported) {
    if (container == null) return;
    boolean armed = config.enabled && dllImported;
    container.putExtra(EXTRA_ARMED, armed);
    container.putExtra(EXTRA_MULTIPLIER, config.multiplier);
    container.putExtra(
        EXTRA_FLOW_SCALE, String.format(Locale.US, "%.2f", config.flowScale));
    container.putExtra(EXTRA_PERFORMANCE_MODE, config.performanceMode);
    container.saveData();
  }

  public static boolean applyLaunchEnv(
      @NonNull Context context,
      @Nullable Container container,
      @NonNull FrameGenerationConfig config,
      @NonNull EnvVars envVars) {
    envVars.remove(ENV_CONFIG);
    envVars.remove(ENV_PROCESS);

    if (!isSupported(container)) {
      return false;
    }

    boolean dllImported = LosslessDllManager.hasImportedDll(context);
    persistSettings(container, config, dllImported);
    boolean armed = config.enabled && dllImported;

    writeConfig(context, container, config, dllImported);
    if (!armed) {
      disableLayerInContainer(container);
      return false;
    }

    if (!ensureRuntimeInstalled(context, container)) {
      return false;
    }

    File configFile = getConfigFile(container);
    envVars.put(ENV_CONFIG, configFile.getAbsolutePath());
    envVars.put(ENV_PROCESS, PROCESS_EXE_IDENTIFIER);

    File layerDir = getLayerDir(container);
    String existingLayerPath = envVars.get("VK_LAYER_PATH");
    if (existingLayerPath == null || existingLayerPath.isEmpty()) {
      envVars.put("VK_LAYER_PATH", layerDir.getAbsolutePath());
    } else if (!existingLayerPath.contains(layerDir.getAbsolutePath())) {
      envVars.put("VK_LAYER_PATH", existingLayerPath + ":" + layerDir.getAbsolutePath());
    }
    return true;
  }

  public static boolean updateRuntimeConfig(
      @NonNull Context context,
      @Nullable Container container,
      @NonNull FrameGenerationConfig config) {
    if (!isSupported(container)) {
      return false;
    }
    boolean dllImported = LosslessDllManager.hasImportedDll(context);
    persistSettings(container, config, dllImported);
    return writeConfig(context, container, config, dllImported);
  }

  @NonNull
  public static String getStatus(
      @NonNull Context context, @Nullable Container container, @NonNull FrameGenerationConfig config) {
    if (!LosslessDllManager.hasImportedDll(context)) {
      return "Lossless.dll missing";
    }
    if (!isSupported(container)) {
      return "LSFG unavailable for this container";
    }
    boolean armed = config.enabled;
    File libFile = getRuntimeLibFile(container);
    File configFile = getConfigFile(container);
    if (armed) {
      return String.format(
          Locale.US,
          "lsfg-vk armed | %dx | flow %.2f%s",
          config.multiplier,
          config.flowScale,
          config.performanceMode ? " | perf" : "");
    }
    if (libFile.isFile() && configFile.isFile()) {
      return "lsfg-vk ready | disabled";
    }
    return "Lossless.dll imported | arms on launch";
  }

  private static boolean ensureRuntimeInstalled(
      @NonNull Context context, @NonNull Container container) {
    File rootDir = resolveRootDir(container);
    if (rootDir == null) {
      return false;
    }

    File libDir = getRuntimeLibFile(container).getParentFile();
    File layerDir = getLayerDir(container);
    File dllDir = getContainerDllFile(container).getParentFile();
    if ((libDir != null && !libDir.exists() && !libDir.mkdirs())
        || (layerDir != null && !layerDir.exists() && !layerDir.mkdirs())
        || (dllDir != null && !dllDir.exists() && !dllDir.mkdirs())) {
      return false;
    }

    File runtimeLib = getRuntimeLibFile(container);
    File manifest = getManifestFile(container);
    File versionFile = new File(layerDir, VERSION_FILENAME);
    String currentVersion = versionFile.isFile() ? FileUtils.readString(versionFile) : null;
    boolean runtimeCurrent =
        runtimeLib.isFile()
            && manifest.isFile()
            && RUNTIME_VERSION.equals(currentVersion != null ? currentVersion.trim() : "");

    if (!runtimeCurrent) {
      File packagedLib = new File(context.getApplicationInfo().nativeLibraryDir, LIB_FILENAME);
      if (!packagedLib.isFile() || !FileUtils.copy(packagedLib, runtimeLib)) {
        return false;
      }
      FileUtils.writeString(manifest, buildManifest());
      FileUtils.writeString(versionFile, RUNTIME_VERSION);
      FileUtils.chmod(runtimeLib, 0755);
      FileUtils.chmod(manifest, 0644);
      FileUtils.chmod(versionFile, 0644);
    }

    File importedDll = LosslessDllManager.getDllFile(context);
    File containerDll = getContainerDllFile(container);
    if (importedDll.isFile()) {
      if (!containerDll.isFile()
          || containerDll.length() != importedDll.length()
          || containerDll.lastModified() != importedDll.lastModified()) {
        if (!FileUtils.copy(importedDll, containerDll)) {
          return false;
        }
        //noinspection ResultOfMethodCallIgnored
        containerDll.setLastModified(importedDll.lastModified());
        FileUtils.chmod(containerDll, 0644);
      }
    } else if (containerDll.exists()) {
      //noinspection ResultOfMethodCallIgnored
      containerDll.delete();
    }

    return runtimeLib.isFile() && manifest.isFile();
  }

  private static boolean writeConfig(
      @NonNull Context context,
      @NonNull Container container,
      @NonNull FrameGenerationConfig config,
      boolean dllImported) {
    File configFile = getConfigFile(container);
    File parent = configFile.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
      return false;
    }

    String dllPath = null;
    if (dllImported) {
      dllPath = getContainerDllFile(container).getAbsolutePath();
    }
    boolean enabled = config.enabled && dllImported && config.multiplier >= 2;
    boolean ok = FileUtils.writeString(configFile, buildConfigToml(dllPath, enabled, config));
    if (ok && configFile.exists()) {
      FileUtils.chmod(configFile, 0644);
    }
    if (!enabled) {
      disableLayerInContainer(container);
    } else {
      File manifest = getManifestFile(container);
      if (!manifest.isFile()) {
        ensureRuntimeInstalled(context, container);
      }
    }
    return ok;
  }

  private static void disableLayerInContainer(@NonNull Container container) {
    File manifest = getManifestFile(container);
    if (manifest.exists()) {
      //noinspection ResultOfMethodCallIgnored
      manifest.delete();
    }
  }

  @Nullable
  private static File resolveRootDir(@NonNull Container container) {
    return container.getRootDir();
  }

  @NonNull
  private static File getRuntimeLibFile(@NonNull Container container) {
    return new File(resolveRootDir(container), LIB_RELATIVE_DIR + "/" + LIB_FILENAME);
  }

  @NonNull
  private static File getLayerDir(@NonNull Container container) {
    return new File(resolveRootDir(container), LAYER_RELATIVE_DIR);
  }

  @NonNull
  private static File getManifestFile(@NonNull Container container) {
    return new File(getLayerDir(container), MANIFEST_FILENAME);
  }

  @NonNull
  private static File getContainerDllFile(@NonNull Container container) {
    return new File(resolveRootDir(container), DLL_RELATIVE_DIR + "/" + DLL_FILENAME);
  }

  @NonNull
  private static File getConfigFile(@NonNull Container container) {
    return new File(resolveRootDir(container), CONFIG_RELATIVE_PATH);
  }

  @NonNull
  private static String buildManifest() {
    return "{\n"
        + "  \"file_format_version\": \"1.0.0\",\n"
        + "  \"layer\": {\n"
        + "    \"name\": \"VK_LAYER_LS_frame_generation\",\n"
        + "    \"type\": \"GLOBAL\",\n"
        + "    \"library_path\": \"../../../lib/"
        + LIB_FILENAME
        + "\",\n"
        + "    \"api_version\": \"1.4.313\",\n"
        + "    \"implementation_version\": \"1\",\n"
        + "    \"description\": \"Lossless Scaling frame generation layer\",\n"
        + "    \"functions\": {\n"
        + "      \"vkGetInstanceProcAddr\": \"layer_vkGetInstanceProcAddr\",\n"
        + "      \"vkGetDeviceProcAddr\": \"layer_vkGetDeviceProcAddr\"\n"
        + "    },\n"
        + "    \"disable_environment\": {\n"
        + "      \"DISABLE_LSFG\": \"1\"\n"
        + "    }\n"
        + "  }\n"
        + "}\n";
  }

  @NonNull
  private static String buildConfigToml(
      @Nullable String dllPath, boolean enabled, @NonNull FrameGenerationConfig config) {
    StringBuilder sb = new StringBuilder();
    sb.append("version = 1\n\n");
    sb.append("[global]\n");
    if (dllPath != null && !dllPath.isEmpty()) {
      sb.append("dll = ").append(toTomlString(dllPath)).append('\n');
    }
    sb.append("no_fp16 = false\n\n");
    if (dllPath != null && !dllPath.isEmpty()) {
      sb.append("[[game]]\n");
      sb.append("exe = ").append(toTomlString(PROCESS_EXE_IDENTIFIER)).append('\n');
      sb.append("multiplier = ").append(enabled ? config.multiplier : 1).append('\n');
      sb.append("flow_scale = ")
          .append(String.format(Locale.US, "%.2f", config.flowScale))
          .append('\n');
      sb.append("performance_mode = ").append(enabled && config.performanceMode).append('\n');
      sb.append("anti_artifacts = ").append(enabled && config.antiArtifacts).append('\n');
      sb.append("framegen_fp16 = ").append(enabled && config.framegenFp16).append('\n');
      sb.append("target_fps_cap = ")
          .append(enabled ? FrameGenerationConfig.clampTargetFpsCap(config.targetFpsCap) : 0)
          .append('\n');
      sb.append("pacing_ema_alpha = ")
          .append(String.format(Locale.US, "%.3f", FrameGenerationConfig.clampEmaAlpha(config.emaAlpha)))
          .append('\n');
      sb.append("pacing_outlier_ratio = ")
          .append(String.format(Locale.US, "%.2f", FrameGenerationConfig.clampOutlierRatio(config.outlierRatio)))
          .append('\n');
      sb.append("pacing_vsync_slack_ms = ")
          .append(String.format(Locale.US, "%.2f", FrameGenerationConfig.clampVsyncSlackMs(config.vsyncSlackMs)))
          .append('\n');
      sb.append("pacing_queue_depth = ")
          .append(FrameGenerationConfig.clampQueueDepth(config.queueDepth))
          .append('\n');
      sb.append("hdr_mode = false\n");
      sb.append("experimental_present_mode = ").append(toTomlString("fifo")).append('\n');
    }
    return sb.toString();
  }

  @NonNull
  private static String toTomlString(@NonNull String value) {
    return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
  }
}
