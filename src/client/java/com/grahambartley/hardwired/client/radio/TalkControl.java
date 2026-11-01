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

  private final BlockPos pos;
  private final BroadcastDeskBlockEntity desk;
  private final TalkState state = new TalkState();
  @Nullable private VoiceCapture capture;
  @Nullable private MicProblem problem;

  public TalkControl(BlockPos pos, BroadcastDeskBlockEntity desk) {
    this.pos = pos;
    this.desk = desk;
  }

  public void press() {
    if (state.press(available())) {
      problem = null;
      send(DeskAction.TAKE_MIC);
    }
  }

  public void letGo() {
    if (state.release()) {
      giveUpMic();
    }
  }

  public void tick(MinecraftClient client) {
    boolean inReach = client.player.canInteractWithBlockAt(pos, DeviceActions.REACH_MARGIN);
    if (state.mustDrop(client.isWindowFocused(), available(), inReach)) {
      drop();
    }
    if (state.askAgain(desk.mic().speaker() == null)) {
      send(DeskAction.TAKE_MIC);
    }
    switch (state.capture(capture != null, mine(client))) {
      case START ->
          capture =
              VoiceCapture.start(
                  frame -> client.execute(() -> send(frame)),
                  failure -> client.execute(() -> fail(failure)));
      case STOP -> closeCapture();
      case KEEP -> {}
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
    drop();
  }

  private void drop() {
    if (state.drop()) {
      giveUpMic();
    }
  }

  private void giveUpMic() {
    closeCapture();
    send(DeskAction.RELEASE_MIC);
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
