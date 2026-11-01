package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.block.ElectronicBlockEntity;
import com.grahambartley.hardwired.power.PowerGrids;
import com.grahambartley.hardwired.wire.WireEngine;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class ElectronicsDriver {

  public static final int BEAT_TICKS = 20;

  private ElectronicsDriver() {}

  public static void wake(ServerWorld world, BlockPos pos) {
    BlockState state = world.getBlockState(pos);
    if (state.getBlock() instanceof Electronic device
        && world.getBlockEntity(pos) instanceof ElectronicBlockEntity entity
        && !(entity.status() == DeviceStatus.RUNNING
            && device.demand(world, pos, state) == DeviceStatus.RUNNING
            && power(world).hasPower(device.wiredAt(world, pos, state)))) {
      pulse(world, pos);
    }
  }

  public static void pulse(ServerWorld world, BlockPos pos) {
    BlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof Electronic device)
        || !(world.getBlockEntity(pos) instanceof ElectronicBlockEntity entity)) {
      return;
    }
    DeviceStatus demand = device.demand(world, pos, state);
    BlockPos wired = device.wiredAt(world, pos, state);
    long now = world.getTime();
    PowerGrids power = power(world);
    DeviceStatus next =
        ElectronicsRule.next(demand, power.hasPower(wired), power.stopReason(wired));
    if (state.getBlock() instanceof WireDevice wiredDevice
        && entity.noteLoad(wiredDevice.load(world, pos, state))) {
      power.changed(wired);
    }
    entity.setStatus(next);
    device.show(world, pos, state, next);
    if (next == DeviceStatus.RUNNING && ElectronicsRule.beatDue(entity.beatAt(), now)) {
      entity.noteBeat(now);
      device.beat(world, pos);
    }
    if (device.keepsChecking(next)) {
      world.scheduleBlockTick(pos, state.getBlock(), BEAT_TICKS);
    }
  }

  public static void show(World world, BlockPos pos, BlockState state, BlockState next) {
    if (state != next) {
      world.setBlockState(pos, next, Block.NOTIFY_ALL);
    }
  }

  public static void tell(World world, BlockPos pos, PlayerEntity player) {
    if (world.getBlockEntity(pos) instanceof ElectronicBlockEntity entity) {
      player.sendMessage(Text.translatable(entity.status().translationKey()), true);
    }
  }

  private static PowerGrids power(ServerWorld world) {
    return WireEngine.of(world).power();
  }
}
