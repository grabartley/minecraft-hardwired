package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.config.SyncedServerConfig;
import com.grahambartley.hardwired.network.CallVoicePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class CallTalk {

  private final BlockPos phone;
  @Nullable private VoiceCapture capture;
  @Nullable private MicProblem problem;

  public CallTalk(BlockPos phone) {
    this.phone = phone;
  }

  public static boolean allowed() {
    return SyncedServerConfig.get().phone().voiceCalls();
  }

  public void press(MinecraftClient client, boolean connected) {
    if (capture == null && connected && allowed()) {
      problem = null;
      capture =
          VoiceCapture.start(
              frame -> client.execute(() -> send(frame)),
              failure -> client.execute(() -> fail(failure)));
    }
  }

  public void letGo() {
    if (capture != null) {
      capture.stop();
      capture = null;
    }
  }

  public void tick(MinecraftClient client, boolean connected) {
    if (!connected || !allowed() || !client.isWindowFocused()) {
      letGo();
    }
  }

  public boolean micOpen() {
    return capture != null;
  }

  @Nullable
  public MicProblem problem() {
    return problem;
  }

  private void fail(MicProblem failure) {
    problem = failure;
    letGo();
  }

  private void send(byte[] frame) {
    if (capture != null) {
      ClientPlayNetworking.send(new CallVoicePayload(phone, frame));
    }
  }
}
