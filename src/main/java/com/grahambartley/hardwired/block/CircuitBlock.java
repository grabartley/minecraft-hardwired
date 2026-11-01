package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.circuit.Design;
import com.grahambartley.hardwired.circuit.Evaluator;
import com.grahambartley.hardwired.circuit.Side;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.registry.ModComponents;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireEngine;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public final class CircuitBlock extends Block implements BlockEntityProvider, WireDevice {

  public static final EnumProperty<CircuitPort> PORT_NORTH =
      EnumProperty.of("port_north", CircuitPort.class);
  public static final EnumProperty<CircuitPort> PORT_EAST =
      EnumProperty.of("port_east", CircuitPort.class);
  public static final EnumProperty<CircuitPort> PORT_SOUTH =
      EnumProperty.of("port_south", CircuitPort.class);
  public static final EnumProperty<CircuitPort> PORT_WEST =
      EnumProperty.of("port_west", CircuitPort.class);

  private static final Map<Direction, EnumProperty<CircuitPort>> PORTS =
      Map.of(
          Direction.NORTH, PORT_NORTH,
          Direction.EAST, PORT_EAST,
          Direction.SOUTH, PORT_SOUTH,
          Direction.WEST, PORT_WEST);

  public CircuitBlock(Settings settings) {
    super(settings);
    BlockState state = getStateManager().getDefaultState();
    for (EnumProperty<CircuitPort> port : PORTS.values()) {
      state = state.with(port, CircuitPort.UNUSED);
    }
    setDefaultState(state);
  }

  public static EnumProperty<CircuitPort> port(Direction side) {
    return PORTS.get(side);
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(PORT_NORTH, PORT_EAST, PORT_SOUTH, PORT_WEST);
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    Design design = context.getStack().getOrDefault(ModComponents.CIRCUIT_DESIGN, Design.EMPTY);
    int turns = CircuitSides.turnsFor(context.getHorizontalPlayerFacing());
    BlockState state = getDefaultState();
    for (Side side : Side.values()) {
      state =
          state.with(
              port(CircuitSides.direction(side.turned(turns))), CircuitPort.of(design.mode(side)));
    }
    return state;
  }

  @Override
  public void onPlaced(
      World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    if (world.getBlockEntity(pos) instanceof CircuitBlockEntity circuit) {
      circuit.setTurns(placer == null ? 0 : CircuitSides.turnsFor(placer.getHorizontalFacing()));
    }
    if (world instanceof ServerWorld server) {
      scheduleStep(server, pos);
    }
  }

  private void scheduleStep(ServerWorld world, BlockPos pos) {
    world.scheduleBlockTick(pos, this, Evaluator.delayToNextStep(world.getTime()));
  }

  @Override
  protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    if (world.getBlockEntity(pos) instanceof CircuitBlockEntity circuit && !circuit.step(world)) {
      scheduleStep(world, pos);
    }
  }

  @Override
  public int ratedLoad(PowerSettings settings) {
    return settings.logicLoad();
  }

  @Override
  public int load(ServerWorld world, BlockPos pos, BlockState state) {
    return WireEngine.of(world).isOn(pos) ? ratedLoad(Hardwired.SERVER_CONFIG.power()) : 0;
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new CircuitBlockEntity(pos, state);
  }

  @Override
  public boolean connectsFrom(BlockState state, Direction side) {
    EnumProperty<CircuitPort> port = port(side);
    return port != null && state.get(port) != CircuitPort.UNUSED;
  }

  @Override
  public boolean isDrivingFrom(
      ServerWorld world, BlockPos pos, BlockState state, Hue color, Direction side) {
    return connectsFrom(state, side)
        && world.getBlockEntity(pos) instanceof CircuitBlockEntity circuit
        && circuit.drives(side);
  }

  @Override
  public void onWireChange(ServerWorld world, BlockPos pos, BlockState state) {
    scheduleStep(world, pos);
  }

  @Override
  public void appendTooltip(
      ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
    Design design = stack.get(ModComponents.CIRCUIT_DESIGN);
    if (design != null) {
      tooltip.add(
          Text.translatable("block.hardwired.circuit_block.parts", design.parts().size())
              .formatted(Formatting.GRAY));
    }
  }
}
