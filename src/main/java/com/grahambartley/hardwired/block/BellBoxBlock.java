package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.device.AnimatedDevice;
import com.grahambartley.hardwired.phone.RingCadence;
import com.grahambartley.hardwired.phone.Switchboard;
import com.grahambartley.hardwired.registry.ModSounds;
import java.util.Set;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public final class BellBoxBlock extends MountedDeviceBlock
    implements BlockEntityProvider, AnimatedDevice {

  private static final float BLOCKS_PER_VOLUME = 16F;

  public BellBoxBlock(Settings settings, double[] floorBox) {
    super(settings, floorBox);
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new AnimatedDeviceBlockEntity(pos, state);
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    return Mounting.placement(getDefaultState(), Set.of(BlockFace.WALL), context);
  }

  @Override
  public boolean isAnimating(BlockState state) {
    return state.get(POWERED);
  }

  @Override
  protected void onBlockAdded(
      BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
    if (!world.isClient && !oldState.isOf(this)) {
      world.scheduleBlockTick(pos, this, 1);
    }
  }

  @Override
  protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    boolean ringing = Switchboard.of(world).bellRings(pos);
    if (ringing != state.get(POWERED)) {
      world.setBlockState(pos, state.with(POWERED, ringing), NOTIFY_ALL);
    }
    float strike = ringing ? RingCadence.gongPitch(world.getTime()) : 0F;
    if (strike > 0F) {
      float volume = Hardwired.SERVER_CONFIG.phone().bellBoxHearing() / BLOCKS_PER_VOLUME;
      world.playSound(null, pos, ModSounds.TELEPHONE_BELL, SoundCategory.BLOCKS, volume, strike);
    }
    world.scheduleBlockTick(pos, this, 1);
  }

  @Override
  public int ratedLoad(PowerSettings settings) {
    return settings.loads().bellBoxLoad();
  }

  @Override
  public int load(ServerWorld world, BlockPos pos, BlockState state) {
    return loadWhenWired(world, pos, state);
  }
}
