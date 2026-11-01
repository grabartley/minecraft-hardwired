package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.radio.MastTier;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightningRodBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public final class RadioMastBlock extends Block implements WireDevice {

  public static final IntProperty TIER = IntProperty.of("tier", MastTier.FOOT, MastTier.SLIM);
  public static final BooleanProperty SEAT = BooleanProperty.of("seat");
  private static final int FULL = 16;
  private static final VoxelShape[] SHAPES = shapes();

  public RadioMastBlock(Settings settings) {
    super(settings);
    setDefaultState(
        getStateManager()
            .getDefaultState()
            .with(TIER, MastTier.FOOT)
            .with(SEAT, false)
            .with(CopperCare.AGE, 0)
            .with(CopperCare.WAXED, false));
  }

  private static VoxelShape[] shapes() {
    VoxelShape[] shapes = new VoxelShape[MastTier.SLIM + 1];
    for (int tier = MastTier.FOOT; tier <= MastTier.SLIM; tier++) {
      int inset = MastTier.insetPixels(tier);
      shapes[tier] = Block.createCuboidShape(inset, 0, inset, FULL - inset, FULL, FULL - inset);
    }
    return shapes;
  }

  public static boolean isRod(BlockState state) {
    return state.isOf(Blocks.LIGHTNING_ROD) && state.get(LightningRodBlock.FACING) == Direction.UP;
  }

  private static int tierOver(BlockState below) {
    return below.getBlock() instanceof RadioMastBlock
        ? MastTier.aboveMast(below.get(TIER))
        : MastTier.FOOT;
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(TIER, SEAT, CopperCare.AGE, CopperCare.WAXED);
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    World world = context.getWorld();
    BlockPos pos = context.getBlockPos();
    return getDefaultState()
        .with(TIER, tierOver(world.getBlockState(pos.down())))
        .with(SEAT, isRod(world.getBlockState(pos.up())));
  }

  @Override
  protected BlockState getStateForNeighborUpdate(
      BlockState state,
      Direction direction,
      BlockState neighbor,
      WorldAccess world,
      BlockPos pos,
      BlockPos neighborPos) {
    return switch (direction) {
      case DOWN -> state.with(TIER, tierOver(neighbor));
      case UP -> state.with(SEAT, isRod(neighbor));
      default -> state;
    };
  }

  @Override
  protected VoxelShape getOutlineShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return SHAPES[state.get(TIER)];
  }

  @Override
  protected boolean hasRandomTicks(BlockState state) {
    return CopperCare.canAge(state);
  }

  @Override
  protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    if (random.nextFloat()
        < CopperCare.ageChance(Hardwired.SERVER_CONFIG.lamp().ageRateMultiplierPercent())) {
      world.setBlockState(pos, state.with(CopperCare.AGE, state.get(CopperCare.AGE) + 1));
    }
  }

  @Override
  protected ItemActionResult onUseWithItem(
      ItemStack stack,
      BlockState state,
      World world,
      BlockPos pos,
      PlayerEntity player,
      Hand hand,
      BlockHitResult hit) {
    return CopperCare.use(
        stack, state, world, pos, player, hand, next -> world.setBlockState(pos, next));
  }
}
