package com.grahambartley.hardwired.device;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;

public interface PhoneDevice {

  BooleanProperty LIFTED = BooleanProperty.of("lifted");
  BooleanProperty RINGING = BooleanProperty.of("ringing");

  static boolean usableAt(ServerWorld world, BlockPos phone, ServerPlayerEntity player) {
    BlockState state = world.getBlockState(phone);
    return !(state.getBlock() instanceof PhoneDevice device)
        || device.usableBy(player, phone, state);
  }

  default boolean usableBy(ServerPlayerEntity player, BlockPos phone, BlockState state) {
    return true;
  }
}
