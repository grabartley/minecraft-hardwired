package com.grahambartley.hardwired.radio;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.block.BroadcastDeskBlockEntity;
import com.grahambartley.hardwired.block.SoundingBlockEntity;
import com.grahambartley.hardwired.config.RadioSettings;
import com.grahambartley.hardwired.device.DeviceActions;
import com.grahambartley.hardwired.network.VoiceFramePayload;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class DeskVoice {

  private DeskVoice() {}

  public static void take(ServerPlayerEntity player, BroadcastDeskBlockEntity desk) {
    if (DeskMic.mayBeLive(desk.status(), voiceBroadcasts())) {
      desk.takeMic(player.getUuid(), Util.getMeasuringTimeMs());
    }
  }

  public static void release(ServerPlayerEntity player, BroadcastDeskBlockEntity desk) {
    desk.releaseMic(player.getUuid());
  }

  public static void frame(ServerPlayerEntity player, VoiceFramePayload frame) {
    BlockPos panel = frame.desk();
    ServerWorld world = player.getServerWorld();
    if (!DeviceActions.canUse(player, panel)
        || !(world.getBlockEntity(panel) instanceof BroadcastDeskBlockEntity desk)
        || !DeskMic.mayBeLive(desk.status(), voiceBroadcasts())
        || !desk.mic().accepts(player.getUuid(), frame.adpcm().length, Util.getMeasuringTimeMs())) {
      return;
    }
    if (!StationRegistry.of(world).audience().tunedTo(panel).isEmpty()) {
      ModCriteria.DEVICE.trigger(player, ModCriteria.LIVE_MIC);
    }
    for (ServerPlayerEntity listener : listeners(world, panel, player)) {
      if (ServerPlayNetworking.canSend(listener, VoiceFramePayload.ID)) {
        ServerPlayNetworking.send(listener, frame);
      }
    }
  }

  public static List<ServerPlayerEntity> listeners(
      ServerWorld world, BlockPos panel, ServerPlayerEntity speaker) {
    RadioSettings settings = Hardwired.SERVER_CONFIG.radio();
    Set<SoundingBlockEntity> tuned = StationRegistry.of(world).audience().tunedTo(panel);
    if (tuned.isEmpty()) {
      return List.of();
    }
    List<VoiceAudience.Device> devices =
        tuned.stream()
            .map(
                device -> {
                  Vec3d at = Vec3d.ofCenter(device.getPos());
                  return new VoiceAudience.Device(at.x, at.y, at.z, device.hearing(settings));
                })
            .toList();
    return world.getPlayers(
        listener ->
            listener != speaker
                && VoiceAudience.hears(listener.getX(), listener.getY(), listener.getZ(), devices));
  }

  public static void beat(ServerWorld world, BroadcastDeskBlockEntity desk) {
    UUID speaker = desk.mic().speaker();
    if (speaker == null) {
      return;
    }
    ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(speaker);
    if (player == null
        || player.getServerWorld() != world
        || !DeskMic.mayBeLive(desk.status(), voiceBroadcasts())
        || desk.mic().gaveUp(Util.getMeasuringTimeMs())
        || !DeskActions.mayWork(player, desk.getPos())) {
      desk.clearMic();
    }
  }

  private static boolean voiceBroadcasts() {
    return Hardwired.SERVER_CONFIG.radio().voiceBroadcasts();
  }
}
