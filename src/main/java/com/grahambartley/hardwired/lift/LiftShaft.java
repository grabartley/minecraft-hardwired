package com.grahambartley.hardwired.lift;

import com.grahambartley.hardwired.block.ElectronicBlockEntity;
import com.grahambartley.hardwired.block.LiftBlock;
import com.grahambartley.hardwired.block.LiftFloorBlock;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.wire.WireEngine;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public record LiftShaft(ServerWorld world, BlockPos head) {

  private static final int CAR_HEIGHT = 2;
  private static final float GATE_VOLUME = 0.7F;
  private static final float GATE_PITCH = 1.3F;

  public static Optional<LiftShaft> forGate(ServerWorld world, BlockPos gate) {
    BlockState state = world.getBlockState(gate);
    if (!(state.getBlock() instanceof LiftFloorBlock)) {
      return Optional.empty();
    }
    BlockPos column = LiftFloorBlock.column(gate, state);
    return devicesOn(world, List.of(gate, gate.up()))
        .filter(pos -> world.getBlockState(pos).getBlock() instanceof LiftBlock)
        .filter(pos -> pos.getX() == column.getX() && pos.getZ() == column.getZ())
        .filter(pos -> pos.getY() > gate.getY() + 1)
        .min(Comparator.comparingInt(BlockPos::getY))
        .map(pos -> new LiftShaft(world, pos));
  }

  private static Stream<BlockPos> devicesOn(ServerWorld world, Collection<BlockPos> points) {
    WireEngine engine = WireEngine.of(world);
    return points.stream()
        .flatMap(pos -> engine.allOnNetwork(pos, ElectronicBlockEntity.class).stream())
        .map(ElectronicBlockEntity::getPos)
        .distinct();
  }

  public boolean standing() {
    return world.getBlockState(head).getBlock() instanceof LiftBlock;
  }

  public List<BlockPos> gates() {
    return devicesOn(world, List.of(head))
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
    BlockPos column = LiftFloorBlock.column(lower, state);
    boolean onColumn = column.getX() == head.getX() && column.getZ() == head.getZ();
    return onColumn && lower.getY() + 1 < head.getY() ? Optional.of(lower) : Optional.empty();
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
    BlockPos pos = new BlockPos(head.getX(), y, head.getZ());
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
            head.getX() + 1,
            head.getY() + 1,
            head.getZ() + 1);
    return world
        .getEntitiesByClass(LiftCarEntity.class, column, car -> head.equals(car.head()))
        .stream()
        .min(Comparator.comparing(LiftCarEntity::getUuid));
  }

  public void showGates(List<BlockPos> gates, double level, boolean open) {
    for (BlockPos gate : gates) {
      BlockState state = world.getBlockState(gate);
      boolean wanted = open && gate.getY() == level;
      if (state.getBlock() instanceof LiftFloorBlock && state.get(LiftFloorBlock.OPEN) != wanted) {
        world.setBlockState(
            gate,
            state.with(LiftFloorBlock.OPEN, wanted),
            Block.NOTIFY_LISTENERS | Block.REDRAW_ON_MAIN_THREAD);
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
