package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.block.SourceBlockEntity;
import com.grahambartley.hardwired.power.PowerGrids;
import com.grahambartley.hardwired.power.PowerSource;
import com.grahambartley.hardwired.power.SourceState;
import com.grahambartley.hardwired.wire.WireEngine;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class DeviceStops {

  private DeviceStops() {}

  public static DeviceStatus at(ServerWorld world, BlockPos pos) {
    PowerGrids power = WireEngine.of(world).power();
    BlockState state = world.getBlockState(pos);
    DeviceStatus gridReason = power.stopReason(pos);
    if (state.getBlock() instanceof PowerSource source
        && world.getBlockEntity(pos) instanceof SourceBlockEntity entity) {
      SourceState own =
          SourceState.of(
              entity.isClosed(),
              source.isFuelled(world, pos, state),
              source.sourceKind().burnsFuel());
      return StopRule.ofSource(own, !power.networksAt(pos).isEmpty(), gridReason);
    }
    WireDevice device = state.getBlock() instanceof WireDevice wired ? wired : null;
    List<BlockPos> cells = device != null ? device.cells(pos, state) : List.of(pos);
    return StopRule.ofDevice(
        cells.stream().anyMatch(power::hasPower),
        cells.stream().anyMatch(power::isOn),
        device != null && device.needsWireOn(),
        gridReason);
  }

  public static boolean tell(ServerWorld world, BlockPos pos, PlayerEntity player) {
    DeviceStatus status = at(world, pos);
    if (status.isStopped()) {
      player.sendMessage(Text.translatable(status.translationKey()), true);
    }
    return status.isPowerStop();
  }
}
