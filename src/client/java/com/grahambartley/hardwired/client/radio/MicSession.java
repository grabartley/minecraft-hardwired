package com.grahambartley.hardwired.client.radio;

import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

public final class MicSession {

  @Nullable private VoiceCapture capture;
  @Nullable private MicProblem problem;

  public void open(MinecraftClient client, Consumer<byte[]> send, Runnable onFailure) {
    if (capture != null) {
      return;
    }
    problem = null;
    capture =
        VoiceCapture.start(
            frame ->
                client.execute(
                    () -> {
                      if (capture != null) {
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
    if (capture != null) {
      capture.stop();
      capture = null;
    }
  }

  public boolean isOpen() {
    return capture != null;
  }

  @Nullable
  public MicProblem problem() {
    return problem;
  }

  public void clearProblem() {
    problem = null;
  }
}
