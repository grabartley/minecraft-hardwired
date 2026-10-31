package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.device.MainPanelActions;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireEngine;
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

public final class MainPanelBlock extends MountedDeviceBlock implements BlockEntityProvider {

  public MainPanelBlock(Settings settings, double[] floorBox) {
    super(settings, floorBox);
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new MainPanelBlockEntity(pos, state);
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    if (player instanceof ServerPlayerEntity serverPlayer) {
      MainPanelActions.open(serverPlayer, pos);
    }
    return ActionResult.SUCCESS;
  }

  @Override
  public boolean isDriving(ServerWorld world, BlockPos pos, BlockState state, Hue color) {
    return false;
  }

  @Override
  public void onWireChange(ServerWorld world, BlockPos pos, BlockState state) {
    boolean live = WireEngine.of(world).isPowered(pos);
    if (state.get(POWERED) != live) {
      world.setBlockState(pos, state.with(POWERED, live), NOTIFY_ALL);
    }
  }
}
