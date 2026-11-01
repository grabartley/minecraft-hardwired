package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.grahambartley.hardwired.radio.AdpcmCodec;
import com.grahambartley.hardwired.radio.MicHealth;
import com.grahambartley.hardwired.radio.VoiceFormat;
import java.util.function.Consumer;

public final class VoiceCapture {

  private final CaptureSource source;
  private final Consumer<byte[]> frames;
  private final Consumer<MicProblem> problems;
  private volatile boolean running = true;

  private VoiceCapture(
      CaptureSource source, Consumer<byte[]> frames, Consumer<MicProblem> problems) {
    this.source = source;
    this.frames = frames;
    this.problems = problems;
  }

  public static VoiceCapture start(Consumer<byte[]> frames, Consumer<MicProblem> problems) {
    return start(new MicrophoneSource(ClientSettingsStore.radio().microphone()), frames, problems);
  }

  static VoiceCapture start(
      CaptureSource source, Consumer<byte[]> frames, Consumer<MicProblem> problems) {
    VoiceCapture capture = new VoiceCapture(source, frames, problems);
    Thread thread = new Thread(capture::run, "Hardwired Microphone");
    thread.setDaemon(true);
    thread.start();
    return capture;
  }

  public void stop() {
    running = false;
    source.close();
  }

  private void run() {
    try {
      source.open();
      AdpcmCodec encoder = new AdpcmCodec();
      MicHealth health = new MicHealth();
      short[] frame = new short[VoiceFormat.FRAME_SAMPLES];
      while (running) {
        source.read(frame);
        if (!running) {
          return;
        }
        if (health.refused(frame)) {
          problems.accept(MicProblem.REFUSED);
          return;
        }
        frames.accept(encoder.encode(frame));
      }
    } catch (MicFailure failure) {
      if (running) {
        problems.accept(failure.problem());
      }
    } finally {
      source.close();
    }
  }
}
