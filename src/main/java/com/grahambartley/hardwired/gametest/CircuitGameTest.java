package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.CircuitBlock;
import com.grahambartley.hardwired.block.CircuitBlockEntity;
import com.grahambartley.hardwired.block.CircuitBoardBlockEntity;
import com.grahambartley.hardwired.block.CircuitPort;
import com.grahambartley.hardwired.circuit.Design;
import com.grahambartley.hardwired.circuit.DesignText;
import com.grahambartley.hardwired.circuit.Line;
import com.grahambartley.hardwired.circuit.Part;
import com.grahambartley.hardwired.circuit.PartType;
import com.grahambartley.hardwired.circuit.PortMode;
import com.grahambartley.hardwired.circuit.Refusal;
import com.grahambartley.hardwired.circuit.Side;
import com.grahambartley.hardwired.device.CircuitAction;
import com.grahambartley.hardwired.device.CircuitActions;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.network.CircuitActionPayload;
import com.grahambartley.hardwired.network.CircuitResultPayload;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.registry.ModComponents;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.TouchPoint;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import com.grahambartley.hardwired.wire.WireRules;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class CircuitGameTest implements FabricGameTest {

  private static final String BENCH = "hardwired:bench";
  private static final int FLOOR_Y = -1;
  private static final int SETTLE_TICKS = 6;
  private static final int RATE_GAP = 5;
  private static final float YAW_NORTH = 180.0F;
  private static final float YAW_EAST = -90.0F;
  private static final float YAW_SOUTH = 0.0F;
  private static final float YAW_WEST = 90.0F;
  private static final BlockPos SWITCH = at(1, 1);
  private static final BlockPos CIRCUIT = at(4, 1);
  private static final BlockPos BOARD = at(4, 4);
  private static final BlockPos STANDING = at(2, 3);
  private static final Identifier LOGIC_GATE = Identifier.of(Hardwired.MOD_ID, "logic_gate");
  private static final Identifier POCKET_CIRCUIT =
      Identifier.of(Hardwired.MOD_ID, "pocket_circuit");
  private static final Identifier SHARE_THE_LOVE =
      Identifier.of(Hardwired.MOD_ID, "share_the_love");

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
    return WireEngine.of(context.getWorld()).isPowered(context.getAbsolutePos(at(6, 1)), Hue.RED);
  }

  private static CircuitBoardBlockEntity board(TestContext context) {
    context.setBlockState(BOARD, ModBlocks.CIRCUIT_BOARD);
    return context.getBlockEntity(BOARD);
  }

  private static void act(
      TestContext context, ConnectedTestPlayer player, CircuitAction action, String text) {
    CircuitActions.act(
        player.player(), CircuitActionPayload.of(context.getAbsolutePos(BOARD), action, text));
  }

  private static String lastMessage(ConnectedTestPlayer player) {
    List<CircuitResultPayload> results = player.circuitResults();
    return results.isEmpty() ? "" : results.get(results.size() - 1).message();
  }

  private static int count(ConnectedTestPlayer player, Item item) {
    PlayerInventory inventory = player.player().getInventory();
    return inventory.count(item);
  }

  private static void give(ConnectedTestPlayer player, Item item, int count) {
    player.player().getInventory().insertStack(new ItemStack(item, count));
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
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    place(context, player, ALWAYS_ON, YAW_EAST, CIRCUIT);
    for (int z = 2; z <= 3; z++) {
      lay(context, 4, z, Hue.RED);
    }
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.assertTrue(
              WireEngine.of(context.getWorld())
                  .isPowered(context.getAbsolutePos(at(4, 3)), Hue.RED),
              "design east placed facing east must drive the south side");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void wireOnAnUnusedSideNeitherFeedsNorIsDriven(TestContext context) {
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
              engine.isPowered(context.getAbsolutePos(at(4, 3)), Hue.BLUE),
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
          context.assertTrue(engine.isPowered(circuit, Hue.RED), "floor wire is driven");
          context.assertTrue(engine.isPowered(circuit, Hue.YELLOW), "side wire is driven too");
          flipSwitch(context);
          context.waitAndRun(
              SETTLE_TICKS,
              () -> {
                context.assertFalse(
                    engine.isPowered(circuit, Hue.RED), "one wire on the input side is enough");
                player.leave();
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void oneNetworkOnTwoSidesFollowsTheDrivingSide(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    place(context, player, ALWAYS_ON, YAW_NORTH, CIRCUIT);
    lay(context, 5, 1, Hue.RED);
    lay(context, 5, 0, Hue.RED);
    lay(context, 4, 0, Hue.RED);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.assertTrue(
              WireEngine.of(context.getWorld())
                  .isPowered(context.getAbsolutePos(at(5, 0)), Hue.RED),
              "the output side must hold the network on");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 60)
  public void aSettledBlockStopsTickingAndAClockKeepsGoing(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    place(context, player, INVERTER, YAW_NORTH, CIRCUIT);
    BlockPos clock = at(6, 6);
    place(context, player, CLOCK, YAW_NORTH, clock);
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
                              .isPowered(context.getAbsolutePos(at(5, 0)), Hue.PLAIN),
                          "a loop through outside wire is memory");
                      player.leave();
                      context.complete();
                    });
              });
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 60)
  public void aLatchKeepsItsStateThroughAReload(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    buildSwitchAndInput(context);
    layOutput(context);
    place(context, player, LATCH, YAW_EAST, CIRCUIT);
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
                context.assertTrue(reloaded.turns() == 1, "the turns must be saved");
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

  @GameTest(templateName = BENCH)
  public void aBrokenBoardDropsItsDesign(TestContext context) {
    CircuitBoardBlockEntity board = board(context);
    board.setDesign(INVERTER, false);
    context.getWorld().breakBlock(context.getAbsolutePos(BOARD), true);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          List<ItemEntity> drops = drops(context, BOARD, ModBlocks.CIRCUIT_BOARD.asItem());
          context.assertTrue(drops.size() == 1, "one board must drop");
          context.assertTrue(
              INVERTER.equals(drops.get(0).getStack().get(ModComponents.CIRCUIT_DESIGN)),
              "the drop must carry the design");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void makingSpendsOneBlankAndTheCostAndGivesOneBlock(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    List<Part> parts = new ArrayList<>();
    for (int x = 0; x < 5; x++) {
      parts.add(Part.of(PartType.AND, x, 0));
    }
    Design fiveParts = new Design(Design.EMPTY.sides(), parts, List.of());
    board(context).setDesign(fiveParts, false);
    Item blank = ModBlocks.BLANK_CIRCUIT_BLOCK.asItem();
    Item made = ModBlocks.CIRCUIT_BLOCK.asItem();
    give(player, blank, 3);
    give(player, Items.REDSTONE, 7);
    act(context, player, CircuitAction.MAKE, "");
    context.assertTrue(count(player, blank) == 2, "one blank must be spent");
    context.assertTrue(count(player, Items.REDSTONE) == 5, "two redstone must be spent");
    context.assertTrue(count(player, made) == 1, "one Circuit Block must be given");
    context.assertTrue(
        3 + 7 == count(player, blank) + count(player, Items.REDSTONE) + 3,
        "items must only be spent at the stated cost");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void makingWithoutTheCostChangesNothing(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    board(context).setDesign(INVERTER, false);
    Item blank = ModBlocks.BLANK_CIRCUIT_BLOCK.asItem();
    give(player, Items.REDSTONE, 4);
    act(context, player, CircuitAction.MAKE, "");
    context.assertTrue(
        lastMessage(player).equals(Refusal.NEEDS_BLANK.key()), "the blank is missing");
    context.assertTrue(count(player, Items.REDSTONE) == 4, "nothing must be spent");
    context.assertTrue(count(player, ModBlocks.CIRCUIT_BLOCK.asItem()) == 0, "nothing made");
    player.player().getInventory().clear();
    give(player, blank, 1);
    context.waitAndRun(
        RATE_GAP,
        () -> {
          act(context, player, CircuitAction.MAKE, "");
          context.assertTrue(
              lastMessage(player).equals(Refusal.NEEDS_REDSTONE.key()), "redstone is missing");
          context.assertTrue(count(player, blank) == 1, "the blank must be kept");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void anEmptyBoardMakesNothing(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    board(context);
    Item blank = ModBlocks.BLANK_CIRCUIT_BLOCK.asItem();
    give(player, blank, 1);
    give(player, Items.REDSTONE, 1);
    act(context, player, CircuitAction.MAKE, "");
    context.assertTrue(lastMessage(player).equals(Refusal.EMPTY.key()), "an empty board");
    context.assertTrue(count(player, blank) == 1, "nothing must be spent");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void savingMakingAndCopyingEarnTheCircuitBranch(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    CircuitBoardBlockEntity board = board(context);
    give(player, ModBlocks.BLANK_CIRCUIT_BLOCK.asItem(), 1);
    give(player, Items.REDSTONE, 1);
    act(context, player, CircuitAction.SAVE, DesignText.encode(INVERTER));
    context.assertTrue(board.design().equals(INVERTER), "the board must hold the design");
    context.assertTrue(player.hasEarned(LOGIC_GATE), "saving earns Logic Gate");
    context.waitAndRun(
        RATE_GAP,
        () -> {
          act(context, player, CircuitAction.MAKE, "");
          context.assertTrue(player.hasEarned(POCKET_CIRCUIT), "making earns Pocket Circuit");
          context.assertFalse(player.hasEarned(SHARE_THE_LOVE), "a first make is not a copy");
          board.setDesign(LATCH, false);
          player.hold(circuitItem(INVERTER), ItemStack.EMPTY);
          give(player, ModBlocks.BLANK_CIRCUIT_BLOCK.asItem(), 1);
          give(player, Items.REDSTONE, 1);
          context.waitAndRun(
              RATE_GAP,
              () -> {
                act(context, player, CircuitAction.LOAD, "");
                context.assertTrue(
                    board.design().equals(INVERTER), "load must read the held block");
                context.waitAndRun(
                    RATE_GAP,
                    () -> {
                      act(context, player, CircuitAction.MAKE, "");
                      context.assertTrue(
                          player.hasEarned(SHARE_THE_LOVE), "copying earns Share The Love");
                      player.leave();
                      context.complete();
                    });
              });
        });
  }

  @GameTest(templateName = BENCH)
  public void loadingWithoutACircuitBlockIsRefused(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    CircuitBoardBlockEntity board = board(context);
    board.setDesign(LATCH, false);
    act(context, player, CircuitAction.LOAD, "");
    context.assertTrue(
        lastMessage(player).equals(Refusal.NOT_HOLDING_CIRCUIT.key()), "nothing is held");
    context.assertTrue(board.design().equals(LATCH), "the board must keep its design");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void aHostilePasteIsRefusedAndChangesNothing(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    CircuitBoardBlockEntity board = board(context);
    board.setDesign(LATCH, false);
    act(context, player, CircuitAction.PASTE, "HWC1|uuuu|not:1:1:0|4:4:0");
    context.assertTrue(
        lastMessage(player).equals(Refusal.LOOP_WITHOUT_DELAY.key()), "a loop must be refused");
    context.assertTrue(board.design().equals(LATCH), "the board must keep its design");
    context.waitAndRun(
        RATE_GAP,
        () -> {
          act(context, player, CircuitAction.PASTE, "not a design at all");
          context.assertTrue(lastMessage(player).equals(Refusal.BAD_TEXT.key()), "bad text");
          context.assertTrue(board.design().equals(LATCH), "the board must keep its design");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void aPasteFromAnotherWorldLandsOnTheBoard(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    CircuitBoardBlockEntity board = board(context);
    act(context, player, CircuitAction.PASTE, DesignText.encode(CLOCK));
    context.assertTrue(board.design().equals(CLOCK), "a good paste must land");
    context.assertFalse(board.isCopy(), "a paste is not a copy of a held block");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void aSecondActionInOneTickIsTooFast(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    CircuitBoardBlockEntity board = board(context);
    act(context, player, CircuitAction.SAVE, DesignText.encode(INVERTER));
    act(context, player, CircuitAction.SAVE, DesignText.encode(LATCH));
    context.assertTrue(lastMessage(player).equals(Refusal.TOO_FAST.key()), "must be held back");
    context.assertTrue(board.design().equals(INVERTER), "the held back save must do nothing");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void twoPlayersSavingOneBoardLeaveTheLastSave(TestContext context) {
    ConnectedTestPlayer first = ConnectedTestPlayer.joinAt(context, STANDING);
    ConnectedTestPlayer second = ConnectedTestPlayer.joinAt(context, STANDING);
    CircuitBoardBlockEntity board = board(context);
    act(context, first, CircuitAction.SAVE, DesignText.encode(INVERTER));
    act(context, second, CircuitAction.SAVE, DesignText.encode(LATCH));
    context.assertTrue(board.design().equals(LATCH), "the last save must win");
    first.leave();
    second.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void actionsNeedReachAndBuildRights(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    CircuitBoardBlockEntity board = board(context);
    BlockPos target = context.getAbsolutePos(BOARD);
    player.player().refreshPositionAfterTeleport(Vec3d.ofCenter(target.add(40, 0, 0)));
    act(context, player, CircuitAction.SAVE, DesignText.encode(INVERTER));
    context.assertTrue(board.design().equals(Design.EMPTY), "out of reach");
    player.player().refreshPositionAfterTeleport(Vec3d.ofCenter(target.add(1, 1, 0)));
    player.player().getAbilities().allowModifyWorld = false;
    act(context, player, CircuitAction.SAVE, DesignText.encode(INVERTER));
    context.assertTrue(board.design().equals(Design.EMPTY), "cannot build");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void anActionOnAnotherBlockDoesNothing(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    context.setBlockState(BOARD, ModBlocks.CONTROLLER);
    CircuitActions.act(
        player.player(),
        new CircuitActionPayload(context.getAbsolutePos(BOARD), 99, "HWC1|uuuu||"));
    act(context, player, CircuitAction.SAVE, DesignText.encode(INVERTER));
    context.assertTrue(player.circuitResults().isEmpty(), "no reply for a block that is no board");
    player.leave();
    context.complete();
  }
}
