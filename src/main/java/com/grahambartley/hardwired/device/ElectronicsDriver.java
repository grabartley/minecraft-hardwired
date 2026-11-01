package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.block.ElectronicBlockEntity;
import com.grahambartley.hardwired.power.ChargeNetwork;
import com.grahambartley.hardwired.wire.WireEngine;
import java.util.List;
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
            && device.demand(world, pos, state) == DeviceStatus.RUNNING)) {
      pulse(world, pos);
    }
  }

  public static void pulse(ServerWorld world, BlockPos pos) {
    BlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof Electronic device)
        || !(world.getBlockEntity(pos) instanceof ElectronicBlockEntity entity)) {
      return;
    }
    DeviceStatus before = entity.status();
    DeviceStatus demand = device.demand(world, pos, state);
    BlockPos wired = device.wiredAt(world, pos, state);
    long now = world.getTime();
    boolean wanted = demand == DeviceStatus.RUNNING;
    boolean covered = wanted && ElectronicsRule.beatPaid(entity.paidAt(), now);
    boolean drained =
        wanted && !covered && ChargeNetwork.tryDrain(world, wired, device.chargePerSecond());
    boolean paid = covered || drained;
    boolean unpaid = wanted && !paid;
    boolean hasCell = unpaid && !ChargeNetwork.storesFor(world, wired).isEmpty();
    DeviceStatus next = ElectronicsRule.next(demand, paid, hasCell);
    entity.setStatus(next);
    device.show(world, pos, state, next);
    if (drained) {
      entity.notePaid(now);
      device.beat(world, pos);
    }
    if (device.keepsChecking(next)) {
      world.scheduleBlockTick(pos, state.getBlock(), BEAT_TICKS);
    }
    if (ElectronicsRule.lightsOut(before, next, () -> networkStatuses(world, wired))) {
      ModCriteria.DEVICE.triggerNear(world, pos, ModCriteria.LIGHTS_OUT);
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

  private static List<DeviceStatus> networkStatuses(ServerWorld world, BlockPos pos) {
    return WireEngine.of(world).allOnNetwork(pos, ElectronicBlockEntity.class).stream()
        .map(ElectronicBlockEntity::status)
        .toList();
  }
}
