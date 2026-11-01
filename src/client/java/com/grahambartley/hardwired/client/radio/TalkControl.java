package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.block.BroadcastDeskBlockEntity;
import com.grahambartley.hardwired.config.SyncedServerConfig;
import com.grahambartley.hardwired.device.DeviceActions;
import com.grahambartley.hardwired.network.DeskActionPayload;
import com.grahambartley.hardwired.network.VoiceFramePayload;
import com.grahambartley.hardwired.radio.DeskAction;
import com.grahambartley.hardwired.radio.DeskMic;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class TalkControl {

  private static final int ASK_AGAIN_TICKS = 10;

  private final BlockPos pos;
  private final BroadcastDeskBlockEntity desk;
  private boolean held;
  private boolean focused = true;
  private int ticksSinceAsked;
  @Nullable private VoiceCapture capture;
  @Nullable private MicProblem problem;

  public TalkControl(BlockPos pos, BroadcastDeskBlockEntity desk) {
    this.pos = pos;
    this.desk = desk;
  }

  public void press() {
    if (!held && available()) {
      held = true;
      problem = null;
      ticksSinceAsked = 0;
      send(DeskAction.TAKE_MIC);
    }
  }

  public void letGo() {
    if (held) {
      held = false;
      closeCapture();
      send(DeskAction.RELEASE_MIC);
    }
  }

  public void tick(MinecraftClient client) {
    boolean nowFocused = client.isWindowFocused();
    boolean lostFocus = focused && !nowFocused;
    focused = nowFocused;
    if (lostFocus
        || !available()
        || !client.player.canInteractWithBlockAt(pos, DeviceActions.REACH_MARGIN)) {
      letGo();
    }
    if (held && desk.mic().speaker() == null && ++ticksSinceAsked >= ASK_AGAIN_TICKS) {
      ticksSinceAsked = 0;
      send(DeskAction.TAKE_MIC);
    }
    if (capture != null && !mine(client)) {
      closeCapture();
    } else if (held && capture == null && mine(client)) {
      capture =
          VoiceCapture.start(
              frame -> client.execute(() -> send(frame)),
              failure -> client.execute(() -> fail(failure)));
    }
  }

  public boolean micOpen() {
    return capture != null;
  }

  public boolean available() {
    return DeskMic.mayBeLive(desk.status(), SyncedServerConfig.get().radio().voiceBroadcasts());
  }

  public boolean taken(MinecraftClient client) {
    return desk.mic().speaker() != null && !mine(client);
  }

  @Nullable
  public MicProblem problem() {
    return problem;
  }

  private boolean mine(MinecraftClient client) {
    UUID speaker = desk.mic().speaker();
    return speaker != null && speaker.equals(client.player.getUuid());
  }

  private void fail(MicProblem failure) {
    problem = failure;
    letGo();
  }

  private void closeCapture() {
    if (capture != null) {
      capture.stop();
      capture = null;
    }
  }

  private void send(byte[] frame) {
    if (capture != null) {
      ClientPlayNetworking.send(new VoiceFramePayload(pos, frame));
    }
  }

  private void send(DeskAction action) {
    ClientPlayNetworking.send(new DeskActionPayload(pos, action.ordinal(), 0));
  }
}
