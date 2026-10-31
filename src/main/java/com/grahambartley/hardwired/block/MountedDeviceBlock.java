package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.wire.Hue;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public abstract class MountedDeviceBlock extends WallMountedBlock implements WireDevice {

  public static final BooleanProperty POWERED = Properties.POWERED;

  private final double[] floorBox;

  protected MountedDeviceBlock(Settings settings, double[] floorBox) {
    super(settings);
    this.floorBox = floorBox;
    setDefaultState(
        getStateManager()
            .getDefaultState()
            .with(FACE, BlockFace.WALL)
            .with(FACING, Direction.NORTH)
            .with(POWERED, false));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(FACE, FACING, POWERED);
  }

  @Override
  protected VoxelShape getOutlineShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return MountShapes.shape(floorBox, state.get(FACE), state.get(FACING));
  }

  @Override
  protected VoxelShape getCollisionShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return VoxelShapes.empty();
  }

  @Override
  protected MapCodec<? extends WallMountedBlock> getCodec() {
    return MapCodec.unit(this);
  }

  @Override
  public boolean isDriving(ServerWorld world, BlockPos pos, BlockState state, Hue color) {
    return state.get(POWERED);
  }
}
