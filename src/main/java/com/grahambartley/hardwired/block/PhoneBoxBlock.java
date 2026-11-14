package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.Electronic;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.device.PhoneDevice;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.phone.PhoneActions;
import com.grahambartley.hardwired.phone.PhoneDirectory;
import com.grahambartley.hardwired.phone.Switchboard;
import com.grahambartley.hardwired.registry.ModSounds;
import com.grahambartley.hardwired.wire.WireEngine;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public final class PhoneBoxBlock extends HorizontalFacingBlock
    implements PhoneDevice, BlockEntityProvider, WireDevice, Electronic {

  public static final EnumProperty<PhoneBoxPart> PART = EnumProperty.of("part", PhoneBoxPart.class);
  public static final EnumProperty<PhoneBoxPaint> PAINT =
      EnumProperty.of("paint", PhoneBoxPaint.class);
  public static final BooleanProperty OPEN = Properties.OPEN;
  public static final BooleanProperty LIT = Properties.LIT;
  private static final int LAMP_LIGHT = 12;

  public PhoneBoxBlock(Settings settings) {
    super(settings);
    setDefaultState(
        getStateManager()
            .getDefaultState()
            .with(FACING, Direction.NORTH)
            .with(PART, PhoneBoxPart.LOWER)
            .with(PAINT, PhoneBoxPaint.OAK)
            .with(OPEN, false)
            .with(LIT, false)
            .with(LIFTED, false)
            .with(RINGING, false));
  }

  public static int light(BlockState state) {
    return state.get(LIT) && state.get(PART) == PhoneBoxPart.UPPER ? LAMP_LIGHT : 0;
  }

  public static BlockPos floorOf(BlockPos pos, BlockState state) {
    return pos.down(state.get(PART).height());
  }

  @Override
  protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
    return MapCodec.unit(this);
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(FACING, PART, PAINT, OPEN, LIT, LIFTED, RINGING);
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return state.get(PART) == PhoneBoxPart.LOWER ? new PhoneBoxBlockEntity(pos, state) : null;
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    BlockState state =
        getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    return WireDevice.fits(context, state, cells(context.getBlockPos(), state)) ? state : null;
  }

  @Override
  public void onPlaced(
      World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    for (PhoneBoxPart part : PhoneBoxPart.values()) {
      if (part != PhoneBoxPart.LOWER) {
        world.setBlockState(pos.up(part.height()), state.with(PART, part), NOTIFY_ALL);
      }
    }
    if (world instanceof ServerWorld server && placer instanceof ServerPlayerEntity player) {
      Text name = stack.get(DataComponentTypes.CUSTOM_NAME);
      PhoneDirectory.of(server)
          .placedBy(pos.asLong(), name == null ? "" : name.getString(), player.getUuid());
    }
  }

  @Override
  protected void onStateReplaced(
      BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
    if (!state.isOf(newState.getBlock())) {
      BlockPos floor = floorOf(pos, state);
      WireDevice.clearOthers(world, pos, this, cells(pos, state));
      if (world instanceof ServerWorld server && pos.equals(floor)) {
        Switchboard.of(server).removed(floor.asLong());
        PhoneDirectory.of(server).remove(floor.asLong());
      }
    }
    super.onStateReplaced(state, world, pos, newState, moved);
  }

  @Override
  protected VoxelShape getOutlineShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return PhoneBoxShapes.shape(state.get(PART), state.get(FACING), state.get(OPEN));
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
    if (!(stack.getItem() instanceof DyeItem dye)) {
      return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    PhoneBoxPaint paint = PhoneBoxPaint.of(dye.getColor());
    if (state.get(PAINT) == paint) {
      return ItemActionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }
    if (!world.isClient) {
      setAll(world, pos, state, cell -> cell.with(PAINT, paint));
      world.playSound(null, pos, SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS, 1F, 1F);
      stack.decrementUnlessCreative(1, player);
    }
    return ItemActionResult.success(world.isClient);
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    BlockPos floor = floorOf(pos, state);
    boolean inside = PhoneBoxReach.inside(player.getPos(), floor);
    Vec3d local = hit.getPos().subtract(Vec3d.of(pos));
    double depth = PhoneBoxShapes.depthFromDoor(local.x, local.z, state.get(FACING));
    if (world instanceof ServerWorld server && player instanceof ServerPlayerEntity serverPlayer) {
      if (PhoneBoxReach.opensDoor(inside, depth)) {
        swing(server, pos, state);
      } else {
        PhoneActions.use(serverPlayer, floor);
      }
    }
    return ActionResult.SUCCESS;
  }

  private void swing(ServerWorld world, BlockPos pos, BlockState state) {
    BlockPos floor = floorOf(pos, state);
    boolean open = !state.get(OPEN);
    setAll(world, pos, state, cell -> cell.with(OPEN, open));
    world.playSound(
        null,
        floor.up(),
        open ? ModSounds.PHONE_BOX_DOOR_OPEN : ModSounds.PHONE_BOX_DOOR_CLOSE,
        SoundCategory.BLOCKS,
        1F,
        1F);
    if (world.getBlockEntity(floor) instanceof PhoneBoxBlockEntity entity) {
      entity.swing(world.getTime());
    }
    ElectronicsDriver.pulse(world, floor);
  }

  private void setAll(
      World world, BlockPos pos, BlockState state, UnaryOperator<BlockState> change) {
    for (BlockPos cell : cells(pos, state)) {
      BlockState current = world.getBlockState(cell);
      if (current.isOf(this)) {
        world.setBlockState(cell, change.apply(current), NOTIFY_ALL);
      }
    }
  }

  @Override
  protected void onBlockAdded(
      BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
    if (!oldState.isOf(this) && state.get(PART) == PhoneBoxPart.LOWER) {
      world.scheduleBlockTick(pos, this, 1);
      if (world instanceof ServerWorld server) {
        PhoneDirectory.of(server)
            .add(pos.asLong(), cells(pos, state).stream().map(BlockPos::asLong).toList(), false);
      }
    }
  }

  @Override
  protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    ElectronicsDriver.pulse(world, pos);
  }

  @Override
  public void onWireChange(ServerWorld world, BlockPos pos, BlockState state) {
    PhoneDirectory.of(world).markStale();
    ElectronicsDriver.wake(world, floorOf(pos, state));
  }

  @Override
  public boolean usableBy(ServerPlayerEntity player, BlockPos phone, BlockState state) {
    return PhoneBoxReach.inside(player.getPos(), phone);
  }

  @Override
  public List<BlockPos> cells(BlockPos pos, BlockState state) {
    BlockPos floor = floorOf(pos, state);
    return IntStream.range(0, PhoneBoxPart.values().length).mapToObj(floor::up).toList();
  }

  @Override
  public DeviceStatus demand(ServerWorld world, BlockPos pos, BlockState state) {
    return PhoneDirectory.readiness(world, pos);
  }

  @Override
  public void show(ServerWorld world, BlockPos pos, BlockState state, DeviceStatus status) {
    boolean lit = !state.get(OPEN) && WireEngine.of(world).power().hasPower(pos);
    if (state.get(LIT) != lit) {
      setAll(world, pos, state, cell -> cell.with(LIT, lit));
    }
  }

  @Override
  public boolean keepsChecking(DeviceStatus status) {
    return true;
  }

  @Override
  public int ratedLoad(PowerSettings settings) {
    return settings.loads().telephoneLoad();
  }

  @Override
  public int load(ServerWorld world, BlockPos pos, BlockState state) {
    return loadWhenWired(world, pos, state);
  }
}
