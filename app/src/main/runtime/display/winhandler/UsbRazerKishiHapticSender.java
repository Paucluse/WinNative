package com.winlator.cmod.runtime.display.winhandler;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

final class UsbRazerKishiHapticSender {
  private static final String TAG = "UsbRazerKishiHaptic";
  private static final int QUEUE_CAPACITY = 24;
  private static final int TRANSFER_TIMEOUT_MS = 100;

  private final UsbDeviceConnection connection;
  private final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY, false);
  private volatile boolean running;
  private Thread worker;
  private UsbEndpoint endpoint;

  UsbRazerKishiHapticSender(UsbDeviceConnection connection) {
    this.connection = connection;
  }

  synchronized void start(UsbEndpoint endpoint) {
    if (this.running) {
      return;
    }

    this.endpoint = endpoint;
    this.running = true;
    this.queue.clear();
    this.worker =
        new Thread(
            () -> {
              while (this.running) {
                try {
                  byte[] frame = this.queue.take();
                  int result =
                      this.connection.bulkTransfer(
                          this.endpoint, frame, frame.length, TRANSFER_TIMEOUT_MS);
                  if (result != frame.length) {
                    Log.w(
                        TAG,
                        "Kishi bulkTransfer failed: result="
                            + result
                            + " expected="
                            + frame.length);
                  }
                } catch (InterruptedException ignored) {
                  break;
                }
              }

              this.queue.clear();
            },
            "UsbRazerKishiHaptic");
    this.worker.setDaemon(true);
    this.worker.start();
  }

  synchronized void stop() {
    this.running = false;
    if (this.worker != null) {
      this.worker.interrupt();
      this.worker = null;
    }
    this.queue.clear();
  }

  void enqueue(byte[] frame) {
    if (!this.running || frame == null || frame.length == 0) {
      return;
    }

    if (!this.queue.offer(frame)) {
      this.queue.poll();
      this.queue.offer(frame);
    }
  }
}
