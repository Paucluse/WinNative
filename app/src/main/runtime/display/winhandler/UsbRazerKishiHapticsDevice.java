package com.winlator.cmod.runtime.display.winhandler;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;
import java.util.Locale;

final class UsbRazerKishiHapticsDevice implements UsbRumbleDevice {
  private static final String TAG = "UsbRazerKishiHaptic";
  private static final int RAZER_VID = 0x1532;
  private static final int UNIVERSAL_XINPUT_PID = 0x0037;
  private static final int KISHI_XINPUT_PLUS_PID = 0x0719;
  private static final int KISHI_ULTRA_HID_PID = 0x071A;
  private static final int KISHI_V3_HID_PID = 0x0721;
  private static final int KISHI_V3_PRO_HID_PID = 0x0724;
  private static final int HAPTIC_INTERFACE_INDEX = 3;
  private static final int FRAME_SIZE = 64;
  private static final int HEADER_SIZE = 10;
  private static final int PAYLOAD_SIZE = 48;
  private static final int CHECKSUM_INDEX = HEADER_SIZE + PAYLOAD_SIZE;
  private static final int SAMPLE_COUNT = PAYLOAD_SIZE / 2;
  private static final int FRAME_DURATION_MS = 6;
  private static final int USB_TYPE_CLASS = 0x20;
  private static final int USB_RECIP_INTERFACE = 0x01;
  private static final int HID_SET_REPORT = 0x09;
  private static final int HID_REPORT_TYPE_FEATURE = 0x03;
  private static final int CONTROL_TIMEOUT_MS = 1000;
  private static final byte[] FRAME_HEADER = {
    (byte) 0x55, (byte) 0xAA, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) PAYLOAD_SIZE, (byte) 0xFE,
    (byte) 0x79
  };

  private final UsbDevice device;
  private final UsbDeviceConnection connection;
  private UsbInterface hapticInterface;
  private UsbEndpoint hapticEndpoint;
  private UsbRazerKishiHapticSender sender;
  private boolean started;

  UsbRazerKishiHapticsDevice(UsbDevice device, UsbDeviceConnection connection) {
    this.device = device;
    this.connection = connection;
  }

  static boolean canClaimDevice(UsbDevice device) {
    if (device == null || device.getVendorId() != RAZER_VID) {
      return false;
    }

    int productId = device.getProductId();
    if (productId == KISHI_XINPUT_PLUS_PID
        || productId == KISHI_ULTRA_HID_PID
        || productId == KISHI_V3_HID_PID
        || productId == KISHI_V3_PRO_HID_PID) {
      return true;
    }

    if (productId != UNIVERSAL_XINPUT_PID) {
      return false;
    }

    String productName = device.getProductName();
    if (productName == null) {
      return false;
    }
    String lowerName = productName.toLowerCase(Locale.US);
    return lowerName.contains("kishi") || lowerName.contains("ultra");
  }

  @Override
  public boolean start() {
    if (this.started) {
      return true;
    }

    detectHapticEndpoint();
    if (this.hapticInterface == null || this.hapticEndpoint == null) {
      Log.w(TAG, "No Kishi haptic endpoint found for " + this.device.getProductName());
      return false;
    }

    if (!this.connection.claimInterface(this.hapticInterface, true)) {
      Log.w(TAG, "Failed to claim Kishi haptic interface id=" + this.hapticInterface.getId());
      return false;
    }

    if (!sendHapticStateControl(true)) {
      this.connection.releaseInterface(this.hapticInterface);
      return false;
    }

    sendHapticIntensity((byte) 0x64);
    this.sender = new UsbRazerKishiHapticSender(this.connection);
    this.sender.start(this.hapticEndpoint);
    this.started = true;
    Log.i(
        TAG,
        "Started Kishi haptics backend for vid=0x"
            + Integer.toHexString(this.device.getVendorId())
            + " pid=0x"
            + Integer.toHexString(this.device.getProductId()));
    return true;
  }

  @Override
  public void rumble(int strong, int weak, int durationMs) {
    if (!this.started || this.sender == null) {
      return;
    }

    int frameCount = Math.max(1, durationMs / FRAME_DURATION_MS);
    short leftSample = toSignedSample(strong);
    short rightSample = toSignedSample(weak);
    for (int i = 0; i < frameCount; i++) {
      this.sender.enqueue(buildFrame(leftSample, rightSample));
    }

    if (strong == 0 && weak == 0) {
      this.sender.enqueue(buildFrame((short) 0, (short) 0));
    }
  }

  @Override
  public void stop() {
    if (!this.started) {
      return;
    }

    if (this.sender != null) {
      this.sender.enqueue(buildFrame((short) 0, (short) 0));
      this.sender.stop();
      this.sender = null;
    }

    sendHapticStateControl(false);
    try {
      this.connection.releaseInterface(this.hapticInterface);
    } catch (Exception ignored) {
    }
    try {
      this.connection.close();
    } catch (Exception ignored) {
    }

    this.started = false;
  }

  private void detectHapticEndpoint() {
    this.hapticInterface = null;
    this.hapticEndpoint = null;

    if (this.device.getInterfaceCount() > HAPTIC_INTERFACE_INDEX) {
      UsbInterface preferred = this.device.getInterface(HAPTIC_INTERFACE_INDEX);
      UsbEndpoint endpoint = findInterruptOutEndpoint(preferred);
      if (endpoint != null) {
        this.hapticInterface = preferred;
        this.hapticEndpoint = endpoint;
        return;
      }
    }

    for (int i = 0; i < this.device.getInterfaceCount(); i++) {
      UsbInterface candidate = this.device.getInterface(i);
      UsbEndpoint endpoint = findInterruptOutEndpoint(candidate);
      if (endpoint != null) {
        this.hapticInterface = candidate;
        this.hapticEndpoint = endpoint;
        return;
      }
    }
  }

  private UsbEndpoint findInterruptOutEndpoint(UsbInterface usbInterface) {
    if (usbInterface == null) {
      return null;
    }

    for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
      UsbEndpoint endpoint = usbInterface.getEndpoint(i);
      if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_INT
          && endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
        return endpoint;
      }
    }

    return null;
  }

  private boolean sendHapticStateControl(boolean enable) {
    byte state = enable ? (byte) 0x01 : (byte) 0x00;
    return sendControlCommand(new byte[] {0x00, 0x01, state})
        && sendControlCommand(new byte[] {0x00, 0x02, state});
  }

  private void sendHapticIntensity(byte intensity) {
    sendControlCommand(new byte[] {0x00, 0x01, intensity});
    sendControlCommand(new byte[] {0x00, 0x02, intensity});
  }

  private boolean sendControlCommand(byte[] data) {
    if (this.hapticInterface == null) {
      return false;
    }

    int requestType = UsbConstants.USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_INTERFACE;
    int value = (HID_REPORT_TYPE_FEATURE << 8);
    int result =
        this.connection.controlTransfer(
            requestType,
            HID_SET_REPORT,
            value,
            this.hapticInterface.getId(),
            data,
            data.length,
            CONTROL_TIMEOUT_MS);
    return result == data.length;
  }

  private byte[] buildFrame(short leftSample, short rightSample) {
    byte[] frame = new byte[FRAME_SIZE];
    System.arraycopy(FRAME_HEADER, 0, frame, 0, HEADER_SIZE);

    for (int i = 0; i < SAMPLE_COUNT; i++) {
      short sample = (i % 2 == 0) ? leftSample : rightSample;
      int offset = HEADER_SIZE + (i * 2);
      frame[offset] = (byte) (sample & 0xFF);
      frame[offset + 1] = (byte) ((sample >> 8) & 0xFF);
    }

    byte checksum = 0;
    for (int i = 2; i < CHECKSUM_INDEX; i++) {
      checksum ^= frame[i];
    }
    frame[CHECKSUM_INDEX] = checksum;
    return frame;
  }

  private short toSignedSample(int intensity) {
    if (intensity <= 0) {
      return 0;
    }

    int amplitude = Math.min(32767, Math.max(512, (int) ((intensity / 65535.0f) * 32767.0f)));
    return (short) amplitude;
  }
}
