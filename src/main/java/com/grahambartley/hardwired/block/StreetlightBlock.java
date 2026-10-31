package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.advancement.ModCriteria;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public final class StreetlightBlock extends AbstractLampBlock {

  public static final int HEAD = 2;
  public static final IntProperty PART = IntProperty.of("part", 0, HEAD);
  private static final VoxelShape POST_SHAPE = Block.createCuboidShape(5, 0, 5, 11, 16, 11);
  private static final VoxelShape HEAD_SHAPE = Block.createCuboidShape(5, 0, 5, 11, 12, 11);

  public StreetlightBlock(Settings settings) {
    super(settings);
    setDefaultState(getDefaultState().with(PART, 0));
  }

  public static boolean isHeadGlowing(BlockState state, BlockView world, BlockPos pos) {
    return state.get(PART) == HEAD && isGlowing(state, world, pos);
  }

  public static int headLightLevel(BlockState state) {
    return state.get(PART) == HEAD ? lightLevel(state) : 0;
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    super.appendProperties(builder);
    builder.add(PART);
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return state.get(PART) == 0 ? super.createBlockEntity(pos, state) : null;
  }

  @Override
  public BlockPos lampPos(BlockPos pos, BlockState state) {
    return pos.down(state.get(PART));
  }

  @Override
  protected List<BlockPos> parts(BlockPos pos, BlockState state) {
    BlockPos base = lampPos(pos, state);
    return List.of(base, base.up(), base.up(HEAD));
  }

  @Override
  protected void update(World world, BlockPos pos, BlockState state) {
    BlockPos base = lampPos(pos, state);
    for (int part = 0; part <= HEAD; part++) {
      world.setBlockState(base.up(part), state.with(PART, part), NOTIFY_ALL);
    }
  }

  @Override
  protected String litEvent(World world) {
    return DuskClock.isDark(world.getTimeOfDay())
        ? ModCriteria.STREETLIGHT_LIT
        : super.litEvent(world);
  }

  @Override
  protected boolean hasRandomTicks(BlockState state) {
    return state.get(PART) == 0 && super.hasRandomTicks(state);
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    BlockPos pos = context.getBlockPos();
    World world = context.getWorld();
    boolean fits =
        pos.getY() + HEAD < world.getTopY()
            && world.getBlockState(pos.up()).canReplace(context)
            && world.getBlockState(pos.up(HEAD)).canReplace(context);
    return fits ? getDefaultState() : null;
  }

  @Override
  public void onPlaced(
      World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    update(world, pos, state);
  }

  @Override
  protected BlockState getStateForNeighborUpdate(
      BlockState state,
      Direction direction,
      BlockState neighborState,
      WorldAccess world,
      BlockPos pos,
      BlockPos neighborPos) {
    int part = state.get(PART);
    boolean linked =
        direction == Direction.UP ? part < HEAD : direction == Direction.DOWN && part > 0;
    boolean partMissing =
        linked
            && !(neighborState.isOf(this)
                && neighborState.get(PART) == part + direction.getOffsetY());
    if (partMissing && part == 0) {
      world.breakBlock(pos, true);
    }
    return partMissing ? Blocks.AIR.getDefaultState() : state;
  }

  @Override
  protected VoxelShape getOutlineShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return state.get(PART) == HEAD ? HEAD_SHAPE : POST_SHAPE;
  }
}
