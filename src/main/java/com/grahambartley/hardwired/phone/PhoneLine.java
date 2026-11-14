package com.grahambartley.hardwired.phone;

import com.grahambartley.hardwired.block.ExchangeBlock;
import com.grahambartley.hardwired.block.TelephoneBlock;
import com.grahambartley.hardwired.device.DeviceActions;
import com.grahambartley.hardwired.power.PowerGrids;
import com.grahambartley.hardwired.registry.ModSounds;
import com.grahambartley.hardwired.wire.WireEngine;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;

final class PhoneLine {

  private final ServerWorld world;

  PhoneLine(ServerWorld world) {
    this.world = world;
  }

  ServerPlayerEntity player(UUID id) {
    return id == null ? null : world.getServer().getPlayerManager().getPlayer(id);
  }

  Optional<LineStatus> fault(long phone, UUID id) {
    BlockPos pos = BlockPos.fromLong(phone);
    ServerPlayerEntity player = player(id);
    if (player == null
        || player.getServerWorld() != world
        || !player.isAlive()
        || !player.canInteractWithBlockAt(pos, DeviceActions.REACH_MARGIN)) {
      return Optional.of(LineStatus.LEFT);
    }
    boolean live = WireEngine.of(world).isLoaded(pos) && answerable(phone);
    return live ? Optional.empty() : Optional.of(LineStatus.LINE_DEAD);
  }

  boolean answerable(long phone) {
    BlockPos pos = BlockPos.fromLong(phone);
    boolean standing =
        !WireEngine.of(world).isLoaded(pos)
            || world.getBlockState(pos).getBlock() instanceof TelephoneBlock;
    return standing
        && power().hasPower(pos)
        && PhoneDirectory.of(world).entry(phone).filter(PhoneEntry::hasNumber).isPresent();
  }

  boolean exchangesLive(Set<Long> exchanges) {
    PhoneDirectory directory = PhoneDirectory.of(world);
    return exchanges.stream()
        .allMatch(
            exchange ->
                directory
                    .entry(exchange)
                    .filter(PhoneEntry::hasNumber)
                    .filter(
                        entry ->
                            entry.cells().stream()
                                .anyMatch(cell -> power().hasPower(BlockPos.fromLong(cell))))
                    .isPresent());
  }

  void ring(Call call, long now) {
    long ticks = call.ringingFor(now);
    boolean ringing = RingCadence.ringing(ticks, call.isPartyLine());
    boolean stroke = RingCadence.strokeStarting(ticks, call.isPartyLine()) > 0;
    for (long phone : call.called()) {
      BlockPos pos = BlockPos.fromLong(phone);
      if (!WireEngine.of(world).isLoaded(pos)) {
        continue;
      }
      setLook(pos, false, ringing);
      if (stroke) {
        world.playSound(null, pos, ModSounds.TELEPHONE_BELL, SoundCategory.BLOCKS, 1F, 1F);
      }
    }
  }

  void show(long phone) {
    Switchboard board = Switchboard.of(world);
    BlockPos pos = BlockPos.fromLong(phone);
    if (WireEngine.of(world).isLoaded(pos)) {
      setLook(pos, board.isLifted(phone), false);
    }
  }

  void tell(Call call, LineStatus status) {
    for (Map.Entry<Long, UUID> member : call.members().entrySet()) {
      ServerPlayerEntity player = player(member.getValue());
      if (player != null) {
        PhoneViews.update(player, member.getKey(), status);
      }
    }
  }

  void tell(long phone, UUID id, LineStatus status) {
    ServerPlayerEntity player = player(id);
    if (player != null) {
      PhoneViews.update(player, phone, status);
    }
  }

  void click(Call call) {
    for (long phone : call.members().keySet()) {
      world.playSound(
          null, BlockPos.fromLong(phone), ModSounds.TELEPHONE_RELAY, SoundCategory.BLOCKS, 1F, 1F);
    }
  }

  void lightExchange(long exchange, int lines) {
    BlockPos anchor = BlockPos.fromLong(exchange);
    BlockState state = world.getBlockState(anchor);
    if (!WireEngine.of(world).isLoaded(anchor)
        || !(state.getBlock() instanceof ExchangeBlock block)) {
      return;
    }
    for (BlockPos cell : block.cells(anchor, state)) {
      BlockState part = world.getBlockState(cell);
      if (part.isOf(block) && part.get(ExchangeBlock.LINES) != lines) {
        world.setBlockState(cell, part.with(ExchangeBlock.LINES, lines), Block.NOTIFY_LISTENERS);
      }
    }
  }

  private void setLook(BlockPos pos, boolean lifted, boolean ringing) {
    BlockState state = world.getBlockState(pos);
    if (state.getBlock() instanceof TelephoneBlock) {
      BlockState next =
          state.with(TelephoneBlock.LIFTED, lifted).with(TelephoneBlock.RINGING, ringing);
      if (next != state) {
        world.setBlockState(pos, next, Block.NOTIFY_LISTENERS);
      }
    }
  }

  private PowerGrids power() {
    return WireEngine.of(world).power();
  }
}
