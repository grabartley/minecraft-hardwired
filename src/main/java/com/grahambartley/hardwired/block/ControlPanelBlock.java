package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.network.OpenDevicePayload;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireEngine;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class ControlPanelBlock extends MountedDeviceBlock implements BlockEntityProvider {

  public ControlPanelBlock(Settings settings, double[] floorBox) {
    super(settings, floorBox);
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new ControlPanelBlockEntity(pos, state);
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    if (player instanceof ServerPlayerEntity serverPlayer) {
      ServerPlayNetworking.send(serverPlayer, new OpenDevicePayload(pos));
    }
    return ActionResult.SUCCESS;
  }

  @Override
  public boolean isDriving(ServerWorld world, BlockPos pos, BlockState state, Hue color) {
    return false;
  }

  @Override
  public void onWireChange(ServerWorld world, BlockPos pos, BlockState state) {
    WireEngine engine = WireEngine.of(world);
    if (engine.lookAt(pos) == null
        && world.getBlockEntity(pos) instanceof ControlPanelBlockEntity panel) {
      engine.applyLook(pos, panel.look());
    }
  }

  @Override
  protected void onStateReplaced(
      BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
    super.onStateReplaced(state, world, pos, newState, moved);
    if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld serverWorld) {
      WireEngine.of(serverWorld)
          .releaseLook(pos, other -> other.getBlock() instanceof ControlPanelBlock);
    }
  }
}
