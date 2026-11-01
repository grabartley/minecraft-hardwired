package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.block.CircuitBlock;
import com.grahambartley.hardwired.block.CircuitBlockEntity;
import com.grahambartley.hardwired.block.CircuitPort;
import com.grahambartley.hardwired.circuit.Design;
import com.grahambartley.hardwired.circuit.Line;
import com.grahambartley.hardwired.circuit.Part;
import com.grahambartley.hardwired.circuit.PartType;
import com.grahambartley.hardwired.circuit.PortMode;
import com.grahambartley.hardwired.circuit.Side;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.registry.ModComponents;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.TouchPoint;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import com.grahambartley.hardwired.wire.WireRules;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class CircuitGameTest implements FabricGameTest {

  private static final String BENCH = "hardwired:bench";
  private static final int FLOOR_Y = -1;
  private static final int SETTLE_TICKS = 6;
  private static final float YAW_NORTH = 180.0F;
  private static final float YAW_EAST = -90.0F;
  private static final float YAW_SOUTH = 0.0F;
  private static final float YAW_WEST = 90.0F;
  private static final BlockPos SWITCH = at(1, 1);
  private static final BlockPos CIRCUIT = at(4, 1);
  private static final BlockPos STANDING = at(2, 3);

  private static final int PART_NODE = Design.partNode(0);
  private static final List<PortMode> WEST_IN_EAST_OUT =
      List.of(PortMode.UNUSED, PortMode.OUTPUT, PortMode.UNUSED, PortMode.INPUT);
  private static final Design INVERTER =
      new Design(
          WEST_IN_EAST_OUT,
          List.of(Part.of(PartType.NOT, 3, 3)),
          List.of(
              new Line(Design.sideNode(Side.WEST), PART_NODE, 0),
              new Line(PART_NODE, Design.sideNode(Side.EAST), 0)));
  private static final Design LATCH =
      new Design(
          WEST_IN_EAST_OUT,
          List.of(Part.of(PartType.SR_LATCH, 3, 3)),
          List.of(
              new Line(Design.sideNode(Side.WEST), PART_NODE, 0),
              new Line(PART_NODE, Design.sideNode(Side.EAST), 0)));
  private static final Design ALWAYS_ON =
      new Design(
          List.of(PortMode.UNUSED, PortMode.OUTPUT, PortMode.UNUSED, PortMode.UNUSED),
          List.of(Part.of(PartType.NOT, 3, 3)),
          List.of(new Line(PART_NODE, Design.sideNode(Side.EAST), 0)));
  private static final Design CLOCK =
      new Design(
          List.of(PortMode.UNUSED, PortMode.OUTPUT, PortMode.UNUSED, PortMode.UNUSED),
          List.of(Part.of(PartType.NOT, 3, 3), new Part(PartType.TIMER, 4, 3, 1)),
          List.of(
              new Line(Design.partNode(1), PART_NODE, 0),
              new Line(PART_NODE, Design.partNode(1), 0),
              new Line(Design.partNode(1), Design.sideNode(Side.EAST), 0)));

  private static BlockPos at(int x, int z) {
    return new BlockPos(x, FLOOR_Y + 1, z);
  }

  private static void lay(TestContext context, int x, int z, Hue hue) {
    WireEngine.of(context.getWorld())
        .lay(
            new WireCell(
                new WireFace(context.getAbsolutePos(new BlockPos(x, FLOOR_Y, z)), Direction.UP),
                hue));
  }

  private static ItemStack circuitItem(Design design) {
    ItemStack stack = new ItemStack(ModBlocks.CIRCUIT_BLOCK);
    stack.set(ModComponents.CIRCUIT_DESIGN, design);
    return stack;
  }

  private static void place(
      TestContext context,
      ConnectedTestPlayer player,
      Design design,
      float yaw,
      BlockPos relative) {
    player.player().setYaw(yaw);
    player.player().setHeadYaw(yaw);
    player.hold(circuitItem(design), ItemStack.EMPTY);
    player.useOn(new WireFace(context.getAbsolutePos(relative.down()), Direction.UP));
  }

  private static void flipSwitch(TestContext context) {
    context.useBlock(SWITCH);
  }

  private static void buildSwitchAndInput(TestContext context) {
    context.setBlockState(
        SWITCH,
        ModBlocks.WALL_SWITCH.getDefaultState().with(WallMountedBlock.FACE, BlockFace.FLOOR));
    for (int x = 1; x <= 3; x++) {
      lay(context, x, 1, Hue.PLAIN);
    }
  }

  private static void layOutput(TestContext context) {
    for (int x = 5; x <= 7; x++) {
      lay(context, x, 1, Hue.RED);
    }
  }

  private static boolean outputOn(TestContext context) {
    return WireEngine.of(context.getWorld()).isOn(context.getAbsolutePos(at(6, 1)), Hue.RED);
  }

  private static List<ItemEntity> drops(TestContext context, BlockPos pos, Item item) {
    return context
        .getWorld()
        .getEntitiesByClass(
            ItemEntity.class,
            new Box(context.getAbsolutePos(pos)).expand(3),
            entity -> entity.getStack().isOf(item));
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void aPlacedInverterFollowsItsInputWire(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    buildSwitchAndInput(context);
    layOutput(context);
    place(context, player, INVERTER, YAW_NORTH, CIRCUIT);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.assertTrue(outputOn(context), "no input, so the inverter must drive its output");
          flipSwitch(context);
          context.waitAndRun(
              SETTLE_TICKS,
              () -> {
                context.assertFalse(outputOn(context), "the input must turn the output off");
                flipSwitch(context);
                context.waitAndRun(
                    SETTLE_TICKS,
                    () -> {
                      context.assertTrue(outputOn(context), "the output must come back");
                      player.leave();
                      context.complete();
                    });
              });
        });
  }

  @GameTest(templateName = BENCH)
  public void placingTurnsTheDesignByThePlacersFacing(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    Design design =
        new Design(List.of(PortMode.INPUT, PortMode.OUTPUT, PortMode.UNUSED, PortMode.UNUSED));
    float[] yaws = {YAW_NORTH, YAW_EAST, YAW_SOUTH, YAW_WEST};
    Direction[] northPorts = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    for (int index = 0; index < yaws.length; index++) {
      BlockPos spot = at(4 + index, 6);
      place(context, player, design, yaws[index], spot);
      BlockState state = context.getBlockState(spot);
      context.assertTrue(
          state.get(CircuitBlock.port(northPorts[index])) == CircuitPort.INPUT,
          "the design's north must land on " + northPorts[index]);
      context.assertTrue(
          state.get(CircuitBlock.port(northPorts[index].rotateYClockwise())) == CircuitPort.OUTPUT,
          "the design's east must stay clockwise of its north");
      CircuitBlockEntity circuit = context.getBlockEntity(spot);
      context.assertTrue(circuit.turns() == index, "the turns must be stored");
      context.assertTrue(circuit.design().equals(design), "the design must be on the block");
    }
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void aTurnedBlockDrivesItsTurnedSide(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    place(context, player, ALWAYS_ON, YAW_EAST, CIRCUIT);
    for (int z = 2; z <= 3; z++) {
      lay(context, 4, z, Hue.RED);
    }
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.assertTrue(
              WireEngine.of(context.getWorld()).isOn(context.getAbsolutePos(at(4, 3)), Hue.RED),
              "design east placed facing east must drive the south side");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void wireOnAnUnusedSideNeitherFeedsNorIsDriven(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    place(context, player, ALWAYS_ON, YAW_NORTH, CIRCUIT);
    for (int z = 2; z <= 3; z++) {
      lay(context, 4, z, Hue.BLUE);
    }
    layOutput(context);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          WireEngine engine = WireEngine.of(context.getWorld());
          context.assertTrue(outputOn(context), "the output side must be driven");
          context.assertFalse(
              engine.isOn(context.getAbsolutePos(at(4, 3)), Hue.BLUE),
              "wire on an unused side must not be driven");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void onlySidesInUseConnectAndTopAndBottomNever(TestContext context) {
    context.setBlockState(
        CIRCUIT,
        ModBlocks.CIRCUIT_BLOCK
            .getDefaultState()
            .with(CircuitBlock.PORT_EAST, CircuitPort.OUTPUT)
            .with(CircuitBlock.PORT_WEST, CircuitPort.INPUT));
    BlockPos pos = context.getAbsolutePos(CIRCUIT);
    BlockState state = context.getBlockState(CIRCUIT);
    WireDevice device = (WireDevice) state.getBlock();
    for (Direction side : Direction.values()) {
      boolean inUse = side == Direction.EAST || side == Direction.WEST;
      context.assertTrue(device.connectsFrom(state, side) == inUse, side + " connection");
      context.assertTrue(
          WireRules.isOutput(state, new TouchPoint(pos, side.getOpposite())) == inUse,
          side + " wire view mark");
    }
    context.complete();
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void everyWireOnASideJoinsItsPort(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    buildSwitchAndInput(context);
    place(context, player, INVERTER, YAW_NORTH, CIRCUIT);
    WireEngine engine = WireEngine.of(context.getWorld());
    engine.lay(
        new WireCell(new WireFace(context.getAbsolutePos(CIRCUIT), Direction.WEST), Hue.BLUE));
    layOutput(context);
    engine.lay(
        new WireCell(new WireFace(context.getAbsolutePos(CIRCUIT), Direction.EAST), Hue.YELLOW));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          BlockPos circuit = context.getAbsolutePos(CIRCUIT);
          context.assertTrue(engine.isOn(circuit, Hue.RED), "floor wire is driven");
          context.assertTrue(engine.isOn(circuit, Hue.YELLOW), "side wire is driven too");
          flipSwitch(context);
          context.waitAndRun(
              SETTLE_TICKS,
              () -> {
                context.assertFalse(
                    engine.isOn(circuit, Hue.RED), "one wire on the input side is enough");
                player.leave();
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void oneNetworkOnTwoSidesFollowsTheDrivingSide(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    place(context, player, ALWAYS_ON, YAW_NORTH, CIRCUIT);
    lay(context, 5, 1, Hue.RED);
    lay(context, 5, 0, Hue.RED);
    lay(context, 4, 0, Hue.RED);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.assertTrue(
              WireEngine.of(context.getWorld()).isOn(context.getAbsolutePos(at(5, 0)), Hue.RED),
              "the output side must hold the network on");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 60)
  public void aSettledBlockStopsTickingAndAClockKeepsGoing(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    place(context, player, INVERTER, YAW_NORTH, CIRCUIT);
    BlockPos clock = at(6, 6);
    place(context, player, CLOCK, YAW_NORTH, clock);
    WireEngine.of(context.getWorld())
        .lay(
            new WireCell(
                new WireFace(context.getAbsolutePos(clock.down()), Direction.UP), Hue.PLAIN));
    ServerWorld world = context.getWorld();
    context.waitAndRun(
        SETTLE_TICKS * 2,
        () -> {
          context.assertFalse(
              world
                  .getBlockTickScheduler()
                  .isQueued(context.getAbsolutePos(CIRCUIT), ModBlocks.CIRCUIT_BLOCK),
              "a settled block must not stay scheduled");
          context.assertTrue(
              world
                  .getBlockTickScheduler()
                  .isQueued(context.getAbsolutePos(clock), ModBlocks.CIRCUIT_BLOCK),
              "a running clock must stay scheduled");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void aLoopThroughOutsideWireHoldsOnLikeAnyLoop(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    Design buffer =
        new Design(
            WEST_IN_EAST_OUT,
            List.of(),
            List.of(new Line(Design.sideNode(Side.WEST), Design.sideNode(Side.EAST), 0)));
    buildSwitchAndInput(context);
    place(context, player, buffer, YAW_NORTH, CIRCUIT);
    WireEngine engine = WireEngine.of(context.getWorld());
    engine.lay(
        new WireCell(new WireFace(context.getAbsolutePos(CIRCUIT), Direction.WEST), Hue.PLAIN));
    engine.lay(
        new WireCell(new WireFace(context.getAbsolutePos(CIRCUIT), Direction.EAST), Hue.PLAIN));
    lay(context, 5, 1, Hue.PLAIN);
    lay(context, 5, 0, Hue.PLAIN);
    lay(context, 4, 0, Hue.PLAIN);
    lay(context, 3, 0, Hue.PLAIN);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          flipSwitch(context);
          context.waitAndRun(
              SETTLE_TICKS,
              () -> {
                flipSwitch(context);
                context.waitAndRun(
                    SETTLE_TICKS,
                    () -> {
                      context.assertTrue(
                          WireEngine.of(context.getWorld())
                              .isOn(context.getAbsolutePos(at(5, 0)), Hue.PLAIN),
                          "a loop through outside wire is memory");
                      player.leave();
                      context.complete();
                    });
              });
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 60)
  public void aLatchKeepsItsStateThroughAReload(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    buildSwitchAndInput(context);
    layOutput(context);
    place(context, player, LATCH, YAW_NORTH, CIRCUIT);
    flipSwitch(context);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          flipSwitch(context);
          context.waitAndRun(
              SETTLE_TICKS,
              () -> {
                CircuitBlockEntity circuit = context.getBlockEntity(CIRCUIT);
                var registries = context.getWorld().getRegistryManager();
                NbtCompound saved = circuit.createNbt(registries);
                CircuitBlockEntity reloaded =
                    new CircuitBlockEntity(circuit.getPos(), circuit.getCachedState());
                reloaded.read(saved, registries);
                context.assertTrue(reloaded.design().equals(LATCH), "the design must be saved");
                context.assertTrue(
                    reloaded.drives(Direction.EAST), "the output must still be driven");
                context.assertTrue(
                    java.util.Arrays.equals(reloaded.memory(), circuit.memory()),
                    "the runtime state must be saved");
                context.assertTrue(reloaded.memory()[0] == 1, "the latch must stay set");
                player.leave();
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH)
  public void aBrokenCircuitBlockDropsItsDesignOnly(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    place(context, player, INVERTER, YAW_WEST, CIRCUIT);
    context.getWorld().breakBlock(context.getAbsolutePos(CIRCUIT), true);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          List<ItemEntity> drops = drops(context, CIRCUIT, ModBlocks.CIRCUIT_BLOCK.asItem());
          context.assertTrue(drops.size() == 1, "one Circuit Block must drop");
          ItemStack dropped = drops.get(0).getStack();
          context.assertTrue(
              INVERTER.equals(dropped.get(ModComponents.CIRCUIT_DESIGN)),
              "the drop must carry the design");
          context.assertTrue(
              ItemStack.areItemsAndComponentsEqual(dropped, circuitItem(INVERTER)),
              "the drop must stack with a freshly made block");
          player.leave();
          context.complete();
        });
  }
}
