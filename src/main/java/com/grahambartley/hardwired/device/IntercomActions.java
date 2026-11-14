package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.ElectronicBlockEntity;
import com.grahambartley.hardwired.block.IntercomBlock;
import com.grahambartley.hardwired.network.IntercomSayPayload;
import com.grahambartley.hardwired.power.PowerGrids;
import com.grahambartley.hardwired.wire.WireEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class IntercomActions {

  private static final DeviceChat CHAT = new DeviceChat();

  private IntercomActions() {}

  public static void forget(ServerPlayerEntity player) {
    CHAT.forget(player);
  }

  public static DeviceStatus readiness(ServerWorld world, BlockPos pos) {
    PowerGrids power = WireEngine.of(world).power();
    return power.hasPower(pos) ? DeviceStatus.READY : power.stopReason(pos);
  }

  public static void open(ServerPlayerEntity player, BlockPos pos) {
    ServerWorld world = player.getServerWorld();
    ElectronicsDriver.pulse(world, pos);
    if (readiness(world, pos).isStopped()) {
      ElectronicsDriver.tell(world, pos, player);
    } else {
      DeviceActions.open(player, pos);
    }
  }

  public static void say(ServerPlayerEntity player, IntercomSayPayload said) {
    ServerWorld world = player.getServerWorld();
    BlockPos pos = said.pos();
    if (!DeviceActions.canUse(player, pos)
        || !(world.getBlockState(pos).getBlock() instanceof IntercomBlock)) {
      return;
    }
    Optional<String> line = CHAT.accept(player, said.line());
    if (line.isEmpty()) {
      return;
    }
    if (readiness(world, pos).isStopped()) {
      ElectronicsDriver.pulse(world, pos);
      ElectronicsDriver.tell(world, pos, player);
      return;
    }
    broadcast(
        world,
        pos,
        Text.translatable(
            "chat.hardwired.intercom", player.getDisplayName(), Text.literal(line.get())));
  }

  private static void broadcast(ServerWorld world, BlockPos pos, Text message) {
    List<BlockPos> intercoms = intercomsWith(world, pos);
    int range = Hardwired.SERVER_CONFIG.device().intercomRangeBlocks();
    world.getServer().sendMessage(message);
    for (ServerPlayerEntity listener : world.getPlayers()) {
      if (DeviceChat.reads(listener) && IntercomLine.reaches(listener.getPos(), intercoms, range)) {
        listener.sendMessage(message);
      }
    }
    for (BlockPos intercom : intercoms) {
      BlockState state = world.getBlockState(intercom);
      if (state.getBlock() instanceof IntercomBlock block) {
        block.ring(world, intercom, state);
      }
    }
  }

  private static List<BlockPos> intercomsWith(ServerWorld world, BlockPos pos) {
    List<BlockPos> intercoms = new ArrayList<>(List.of(pos));
    WireEngine.of(world).allOnNetwork(pos, ElectronicBlockEntity.class).stream()
        .map(ElectronicBlockEntity::getPos)
        .filter(other -> !other.equals(pos))
        .filter(other -> world.getBlockState(other).getBlock() instanceof IntercomBlock)
        .forEach(intercoms::add);
    return intercoms;
  }
}
