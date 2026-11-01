package com.grahambartley.hardwired.lift;

import com.grahambartley.hardwired.block.ElectronicBlockEntity;
import com.grahambartley.hardwired.block.LiftBlock;
import com.grahambartley.hardwired.block.LiftFloorBlock;
import com.grahambartley.hardwired.block.LiftFloorBlockEntity;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.registry.ModEntities;
import com.grahambartley.hardwired.wire.WireEngine;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public record LiftShaft(ServerWorld world, BlockPos head, int span) {

  private static final int CAR_HEIGHT = 2;
  private static final float GATE_VOLUME = 0.7F;
  private static final float GATE_PITCH = 1.3F;

  public LiftShaft(ServerWorld world, BlockPos head) {
    this(world, head, LiftBlock.spanAt(world, head));
  }

  public static Optional<LiftShaft> forGate(ServerWorld world, BlockPos gate) {
    BlockState state = world.getBlockState(gate);
    if (!(state.getBlock() instanceof LiftFloorBlock)) {
      return Optional.empty();
    }
    LiftFloorBlock floor = (LiftFloorBlock) state.getBlock();
    List<BlockPos> columns = floor.columns(gate, state);
    return devicesOn(world, floor.cells(gate, state))
        .map(pos -> headAt(world, pos))
        .flatMap(Optional::stream)
        .map(pos -> new LiftShaft(world, pos))
        .filter(shaft -> columns.stream().allMatch(shaft::holds))
        .filter(shaft -> shaft.head().getY() > gate.getY() + 1)
        .min(Comparator.comparingInt(shaft -> shaft.head().getY()));
  }

  private static Optional<BlockPos> headAt(ServerWorld world, BlockPos pos) {
    BlockState state = world.getBlockState(pos);
    return state.getBlock() instanceof LiftBlock lift
        ? Optional.of(lift.head(pos, state))
        : Optional.empty();
  }

  private boolean holds(BlockPos column) {
    return LiftSafety.inColumn(column.getX(), column.getZ(), head.getX(), head.getZ(), span);
  }

  private static Stream<BlockPos> devicesOn(ServerWorld world, Collection<BlockPos> points) {
    WireEngine engine = WireEngine.of(world);
    return points.stream()
        .flatMap(pos -> engine.allOnNetwork(pos, ElectronicBlockEntity.class).stream())
        .map(ElectronicBlockEntity::getPos)
        .distinct();
  }

  private List<BlockPos> cells() {
    return span > 1 ? LiftCorner.cells(head) : List.of(head);
  }

  public EntityType<LiftCarEntity> carType() {
    return world.getBlockState(head).getBlock() instanceof LiftBlock lift
        ? lift.carType()
        : ModEntities.LIFT_CAR;
  }

  public boolean standing() {
    return world.getBlockState(head).getBlock() instanceof LiftBlock;
  }

  public List<BlockPos> gates() {
    return devicesOn(world, cells())
        .map(this::lowerGate)
        .flatMap(Optional::stream)
        .distinct()
        .sorted(Comparator.comparingInt(BlockPos::getY))
        .toList();
  }

  private Optional<BlockPos> lowerGate(BlockPos pos) {
    BlockState state = world.getBlockState(pos);
    return state.getBlock() instanceof LiftFloorBlock
        ? gateAt(LiftFloorBlock.lower(pos, state))
        : Optional.empty();
  }

  public Optional<BlockPos> gateAt(BlockPos lower) {
    BlockState state = world.getBlockState(lower);
    if (!(state.getBlock() instanceof LiftFloorBlock)) {
      return Optional.empty();
    }
    boolean served =
        ((LiftFloorBlock) state.getBlock()).columns(lower, state).stream().allMatch(this::holds);
    return served && lower.getY() + 1 < head.getY() ? Optional.of(lower) : Optional.empty();
  }

  public Optional<Direction> gateFacing(int level) {
    return gates().stream()
        .filter(gate -> gate.getY() == level)
        .findFirst()
        .map(gate -> world.getBlockState(gate).get(LiftFloorBlock.FACING));
  }

  public static List<Integer> levels(List<BlockPos> gates) {
    return LiftFloors.ordered(gates.stream().map(BlockPos::getY).toList());
  }

  public boolean pathClear(double fromLevel, double toLevel) {
    int bottom = (int) Math.floor(Math.min(fromLevel, toLevel));
    int top = (int) Math.ceil(Math.max(fromLevel, toLevel)) + CAR_HEIGHT - 1;
    for (int y = bottom; y <= top; y++) {
      if (!rowClear(y)) {
        return false;
      }
    }
    return true;
  }

  private boolean rowClear(int y) {
    return cells().stream().map(cell -> cell.withY(y)).allMatch(this::cellClear);
  }

  private boolean cellClear(BlockPos pos) {
    BlockState state = world.getBlockState(pos);
    return state.getCollisionShape(world, pos).isEmpty()
        && !state.getFluidState().isIn(FluidTags.LAVA);
  }

  public double dropBelow(double level) {
    int y = (int) Math.floor(level) - 1;
    while (y > world.getBottomY() && rowClear(y)) {
      y--;
    }
    return level - (y + 1);
  }

  public Optional<LiftCarEntity> car() {
    Box column =
        new Box(
            head.getX(),
            world.getBottomY(),
            head.getZ(),
            head.getX() + span,
            head.getY() + 1,
            head.getZ() + span);
    return world
        .getEntitiesByClass(LiftCarEntity.class, column, car -> head.equals(car.head()))
        .stream()
        .min(Comparator.comparing(LiftCarEntity::getUuid));
  }

  public void showGates(List<BlockPos> gates, double level, boolean open) {
    for (BlockPos gate : gates) {
      BlockState state = world.getBlockState(gate);
      boolean wanted = open && gate.getY() == level;
      if (state.getBlock() instanceof LiftFloorBlock floor
          && state.get(LiftFloorBlock.OPEN) != wanted) {
        for (BlockPos cell : floor.cells(gate, state)) {
          setOpen(cell, wanted);
        }
        world.playSound(
            null,
            gate,
            wanted ? SoundEvents.BLOCK_IRON_DOOR_OPEN : SoundEvents.BLOCK_IRON_DOOR_CLOSE,
            SoundCategory.BLOCKS,
            GATE_VOLUME,
            GATE_PITCH);
      }
    }
  }

  private void setOpen(BlockPos cell, boolean open) {
    BlockState state = world.getBlockState(cell);
    if (state.getBlock() instanceof LiftFloorBlock) {
      world.setBlockState(
          cell,
          state.with(LiftFloorBlock.OPEN, open),
          Block.NOTIFY_LISTENERS | Block.REDRAW_ON_MAIN_THREAD);
    }
  }

  public void showDial(List<BlockPos> gates, LiftDial dial) {
    for (BlockPos gate : gates) {
      if (world.getBlockEntity(gate.up()) instanceof LiftFloorBlockEntity upper) {
        upper.setDial(dial);
      }
    }
  }

  public void mirror(List<BlockPos> gates, DeviceStatus status) {
    DeviceStatus shown = status.isStopped() ? status : DeviceStatus.READY;
    for (BlockPos gate : gates) {
      setStatus(gate, shown);
      setStatus(gate.up(), shown);
    }
  }

  private void setStatus(BlockPos pos, DeviceStatus status) {
    if (world.getBlockEntity(pos) instanceof ElectronicBlockEntity entity) {
      entity.setStatus(status);
    }
  }
}
