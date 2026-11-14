package com.grahambartley.hardwired.client.radio;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

public final class MicSession {

  interface Mic {
    Runnable start(Consumer<byte[]> frames, Consumer<MicProblem> problems);
  }

  private final Mic mic;
  private final Executor client;
  @Nullable private Runnable stop;
  @Nullable private MicProblem problem;

  public MicSession() {
    this(
        (frames, problems) -> VoiceCapture.start(frames, problems)::stop,
        task -> MinecraftClient.getInstance().execute(task));
  }

  MicSession(Mic mic, Executor client) {
    this.mic = mic;
    this.client = client;
  }

  public void open(Consumer<byte[]> send, Runnable onFailure) {
    if (stop != null) {
      return;
    }
    problem = null;
    stop =
        mic.start(
            frame ->
                client.execute(
                    () -> {
                      if (stop != null) {
                        send.accept(frame);
                      }
                    }),
            failure ->
                client.execute(
                    () -> {
                      problem = failure;
                      close();
                      onFailure.run();
                    }));
  }

  public void close() {
    if (stop != null) {
      stop.run();
      stop = null;
    }
  }

  public boolean isOpen() {
    return stop != null;
  }

  @Nullable
  public MicProblem problem() {
    return problem;
  }

  public void clearProblem() {
    problem = null;
  }
}
