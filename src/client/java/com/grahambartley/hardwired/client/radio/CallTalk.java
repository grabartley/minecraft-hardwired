package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.config.SyncedServerConfig;
import com.grahambartley.hardwired.network.CallVoicePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class CallTalk {

  private final BlockPos phone;
  private final MicSession mic = new MicSession();

  public CallTalk(BlockPos phone) {
    this.phone = phone;
  }

  public static boolean allowed() {
    return SyncedServerConfig.get().phone().voiceCalls();
  }

  public void press(MinecraftClient client, boolean connected) {
    if (connected && allowed()) {
      mic.open(
          client, frame -> ClientPlayNetworking.send(new CallVoicePayload(phone, frame)), () -> {});
    }
  }

  public void letGo() {
    mic.close();
  }

  public void tick(MinecraftClient client, boolean connected) {
    if (!connected || !allowed() || !client.isWindowFocused()) {
      letGo();
    }
  }

  public boolean micOpen() {
    return mic.isOpen();
  }

  @Nullable
  public MicProblem problem() {
    return mic.problem();
  }
}
