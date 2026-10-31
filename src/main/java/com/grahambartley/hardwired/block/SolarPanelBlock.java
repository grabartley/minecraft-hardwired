package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.power.ChargeNetwork;
import com.grahambartley.hardwired.power.SolarLogic;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

public final class SolarPanelBlock extends Block implements WireDevice {

  public static final BooleanProperty LIT = Properties.LIT;

  private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 6, 16);

  public SolarPanelBlock(Settings settings) {
    super(settings);
    setDefaultState(getStateManager().getDefaultState().with(LIT, false));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(LIT);
  }

  @Override
  protected VoxelShape getOutlineShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return SHAPE;
  }

  @Override
  protected void onBlockAdded(
      BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
    if (!oldState.isOf(this)) {
      world.scheduleBlockTick(pos, this, SolarLogic.CHECK_TICKS);
    }
  }

  @Override
  protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    boolean lit = inDaylight(world, pos);
    if (state.get(LIT) != lit) {
      world.setBlockState(pos, state.with(LIT, lit), NOTIFY_ALL);
    }
    if (lit) {
      generate(world, pos);
    }
    world.scheduleBlockTick(pos, this, SolarLogic.CHECK_TICKS);
  }

  private static boolean inDaylight(ServerWorld world, BlockPos pos) {
    int power =
        SolarLogic.daylight(
            world.getLightLevel(LightType.SKY, pos),
            world.getAmbientDarkness(),
            world.getSkyAngleRadians(1F));
    return world.getDimension().hasSkyLight() && SolarLogic.isDay(power);
  }

  private static void generate(ServerWorld world, BlockPos pos) {
    int perMinute = Hardwired.SERVER_CONFIG.power().solarChargePerMinute();
    int earned = SolarLogic.earned(perMinute, world.getTime(), SolarLogic.CHECK_TICKS);
    if (earned > 0 && ChargeNetwork.fill(world, pos, earned) > 0) {
      ModCriteria.DEVICE.triggerNear(world, pos, ModCriteria.SOLAR_CHARGED);
    }
  }
}
