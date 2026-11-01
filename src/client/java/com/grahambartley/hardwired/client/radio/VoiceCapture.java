package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.grahambartley.hardwired.radio.AdpcmCodec;
import com.grahambartley.hardwired.radio.MicHealth;
import com.grahambartley.hardwired.radio.VoiceFormat;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class VoiceCapture {

  private static final Supplier<CaptureSource> MICROPHONE =
      () -> new MicrophoneSource(ClientSettingsStore.radio().microphone());
  private static Supplier<CaptureSource> sources = MICROPHONE;

  private final Consumer<byte[]> frames;
  private final Consumer<MicProblem> problems;
  private volatile boolean running = true;

  private VoiceCapture(Consumer<byte[]> frames, Consumer<MicProblem> problems) {
    this.frames = frames;
    this.problems = problems;
  }

  public static void useSource(Supplier<CaptureSource> next) {
    sources = next;
  }

  public static VoiceCapture start(Consumer<byte[]> frames, Consumer<MicProblem> problems) {
    VoiceCapture capture = new VoiceCapture(frames, problems);
    CaptureSource source = sources.get();
    Thread thread = new Thread(() -> capture.run(source), "Hardwired Microphone");
    thread.setDaemon(true);
    thread.start();
    return capture;
  }

  public void stop() {
    running = false;
  }

  private void run(CaptureSource source) {
    try {
      source.open();
      AdpcmCodec encoder = new AdpcmCodec();
      MicHealth health = new MicHealth();
      short[] frame = new short[VoiceFormat.FRAME_SAMPLES];
      while (running) {
        source.read(frame);
        if (health.refused(frame)) {
          problems.accept(MicProblem.REFUSED);
          return;
        }
        if (running) {
          frames.accept(encoder.encode(frame));
        }
      }
    } catch (MicFailure failure) {
      problems.accept(failure.problem());
    } finally {
      source.close();
    }
  }
}
