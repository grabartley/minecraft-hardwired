package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.power.SolarLogic;
import com.grahambartley.hardwired.power.SourceKind;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
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

public final class SolarPanelBlock extends SourceBlock {

  public static final BooleanProperty LIT = Properties.LIT;

  private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 9.5, 16);

  public SolarPanelBlock(Settings settings) {
    super(settings);
    setDefaultState(getDefaultState().with(LIT, false));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    super.appendProperties(builder);
    builder.add(LIT);
  }

  @Override
  public SourceKind sourceKind() {
    return SourceKind.SOLAR;
  }

  @Override
  public boolean isFuelled(ServerWorld world, BlockPos pos, BlockState state) {
    return state.get(LIT);
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new SourceBlockEntity(ModBlockEntities.SOLAR_PANEL, pos, state);
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
}
