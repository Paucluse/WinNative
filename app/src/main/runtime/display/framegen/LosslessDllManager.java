package com.winlator.cmod.runtime.display.framegen;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class LosslessDllManager {
  public static final String PREF_DLL_DISPLAY_NAME = "superframe_lossless_dll_display_name";
  private static final String DLL_DIRECTORY = "framegen/lossless";
  private static final String DLL_FILE_NAME = "Lossless.dll";

  public static final class ImportResult {
    public final boolean success;
    public final String message;
    public final String displayName;

    public ImportResult(boolean success, @NonNull String message, @Nullable String displayName) {
      this.success = success;
      this.message = message;
      this.displayName = displayName;
    }
  }

  private LosslessDllManager() {}

  public static File getDllFile(Context context) {
    return new File(new File(context.getFilesDir(), DLL_DIRECTORY), DLL_FILE_NAME);
  }

  public static boolean hasImportedDll(Context context) {
    return getDllFile(context).isFile();
  }

  @NonNull
  public static String getImportedDllDisplayName(Context context, SharedPreferences preferences) {
    String stored = preferences.getString(PREF_DLL_DISPLAY_NAME, DLL_FILE_NAME);
    return stored != null && !stored.trim().isEmpty() ? stored : DLL_FILE_NAME;
  }

  public static void syncNativeConfig(Context context) {
    File dllFile = getDllFile(context);
    FrameGenerationBridge.configureUserLosslessDll(
        dllFile.isFile() ? dllFile.getAbsolutePath() : null);
  }

  @NonNull
  public static ImportResult importFromUri(
      Context context, SharedPreferences preferences, Uri uri) {
    String displayName = resolveDisplayName(context, uri);
    if (!DLL_FILE_NAME.equalsIgnoreCase(displayName)) {
      return new ImportResult(
          false,
          "Selected file is \"" + displayName + "\", expected \"" + DLL_FILE_NAME + "\".",
          displayName);
    }

    File dllFile = getDllFile(context);
    File parent = dllFile.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
      return new ImportResult(false, "Failed to create Lossless.dll storage directory.", null);
    }

    try (InputStream input = context.getContentResolver().openInputStream(uri)) {
      if (input == null) {
        return new ImportResult(false, "Couldn't open the selected Lossless.dll file.", null);
      }
      try (OutputStream output = java.nio.file.Files.newOutputStream(dllFile.toPath())) {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
          output.write(buffer, 0, read);
        }
      }
    } catch (IOException e) {
      return new ImportResult(false, "Couldn't copy Lossless.dll: " + e.getMessage(), null);
    }

    preferences.edit().putString(PREF_DLL_DISPLAY_NAME, displayName).apply();
    syncNativeConfig(context);
    return new ImportResult(true, "Lossless.dll imported.", displayName);
  }

  public static void clearImportedDll(Context context, SharedPreferences preferences) {
    File dllFile = getDllFile(context);
    if (dllFile.isFile()) {
      //noinspection ResultOfMethodCallIgnored
      dllFile.delete();
    }
    preferences.edit().remove(PREF_DLL_DISPLAY_NAME).apply();
    syncNativeConfig(context);
  }

  public static void tryPersistReadPermission(Context context, Uri uri) {
    try {
      context
          .getContentResolver()
          .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } catch (SecurityException ignored) {
    }
  }

  @NonNull
  private static String resolveDisplayName(Context context, Uri uri) {
    Cursor cursor =
        context
            .getContentResolver()
            .query(uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null);
    if (cursor == null) return DLL_FILE_NAME;
    try {
      if (cursor.moveToFirst()) {
        String value = cursor.getString(0);
        if (value != null && !value.trim().isEmpty()) return value;
      }
    } finally {
      cursor.close();
    }
    return DLL_FILE_NAME;
  }
}
