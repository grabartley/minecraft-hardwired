package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.block.AbstractLampBlock;
import com.grahambartley.hardwired.block.ControllerBlockEntity;
import com.grahambartley.hardwired.block.MasterSwitchBlock;
import com.grahambartley.hardwired.block.MasterSwitchBlockEntity;
import com.grahambartley.hardwired.block.MountedDeviceBlock;
import com.grahambartley.hardwired.block.RedstoneCellBlockEntity;
import com.grahambartley.hardwired.block.SourceBlockEntity;
import com.grahambartley.hardwired.controller.ControllerProgram;
import com.grahambartley.hardwired.controller.ControllerWiring;
import com.grahambartley.hardwired.controller.Timeline;
import com.grahambartley.hardwired.controller.TimelineStep;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.DeviceStops;
import com.grahambartley.hardwired.device.MainPanelActions;
import com.grahambartley.hardwired.device.PanelAction;
import com.grahambartley.hardwired.network.MainPanelActionPayload;
import com.grahambartley.hardwired.power.GridRule;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.LampLook;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class PowerStopGameTest implements FabricGameTest {

  private static final String BENCH = "hardwired:bench";
  private static final int FLOOR_Y = -1;
  private static final int SETTLE = 10;
  private static final int HOLD = GridRule.MIN_DARK_TICKS + SETTLE;
  private static final int FULL = 15;
  private static final int LONG_WAIT = 20;
  private static final BlockPos CELL = at(1, 1);
  private static final BlockPos SWITCH = at(3, 1);
  private static final BlockPos LAMP = at(5, 1);
  private static final ControllerProgram DOOR =
      new ControllerProgram(
          new ControllerWiring(Hue.PLAIN, Hue.PLAIN, List.of(Optional.of(Hue.RED))),
          new Timeline(List.of(new TimelineStep(1, LONG_WAIT), new TimelineStep(0, LONG_WAIT)), 0));

  private static BlockPos at(int x, int z) {
    return new BlockPos(x, FLOOR_Y + 1, z);
  }

  private static WireFace floor(TestContext context, int x, int z) {
    return new WireFace(context.getAbsolutePos(new BlockPos(x, FLOOR_Y, z)), Direction.UP);
  }

  private static WireEngine engine(TestContext context) {
    return WireEngine.of(context.getWorld());
  }

  private static void layRun(TestContext context, int fromX, int toX, int z, Hue hue) {
    for (int x = fromX; x <= toX; x++) {
      engine(context).lay(new WireCell(floor(context, x, z), hue));
    }
  }

  private static BlockState floorMounted(net.minecraft.block.Block block, boolean on) {
    return block
        .getDefaultState()
        .with(WallMountedBlock.FACE, BlockFace.FLOOR)
        .with(MountedDeviceBlock.POWERED, on);
  }

  private static void cell(TestContext context, BlockPos pos, int charge) {
    context.setBlockState(pos, ModBlocks.REDSTONE_CELL);
    RedstoneCellBlockEntity cell = context.getBlockEntity(pos);
    cell.setCharge(charge);
  }

  private static DeviceStatus statusAt(TestContext context, BlockPos pos) {
    return DeviceStops.at(context.getWorld(), context.getAbsolutePos(pos));
  }

  @GameTest(templateName = BENCH, tickLimit = 120)
  public void aDarkControllerHoldsItsStepThenCarriesOn(TestContext context) {
    context.setBlockState(SWITCH, floorMounted(ModBlocks.WALL_SWITCH, true));
    context.setBlockState(LAMP, ModBlocks.CONTROLLER);
    ControllerBlockEntity controller = context.getBlockEntity(LAMP);
    controller.setProgram(DOOR);
    layRun(context, 1, 5, 1, Hue.PLAIN);
    context.waitAndRun(
        SETTLE,
        () -> {
          context.assertTrue(controller.run().position() == 0, "no power, so no step is taken");
          cell(context, CELL, 100);
        });
    context.waitAndRun(
        3L * SETTLE,
        () -> {
          context.assertTrue(controller.run().position() > 0, "power returned, so it carries on");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void aMainPanelWithNoPowerWorksNothing(TestContext context) {
    context.setBlockState(SWITCH, floorMounted(ModBlocks.WALL_SWITCH, false));
    context.setBlockState(LAMP, floorMounted(ModBlocks.MAIN_PANEL, false));
    layRun(context, 3, 5, 1, Hue.PLAIN);
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, at(4, 3));
    context.waitAndRun(
        SETTLE,
        () -> {
          boolean worked =
              MainPanelActions.perform(
                  player.player(),
                  new MainPanelActionPayload(
                      context.getAbsolutePos(LAMP),
                      context.getAbsolutePos(SWITCH),
                      PanelAction.TOGGLE,
                      LampLook.DEFAULT));
          context.assertFalse(worked, "a dark panel must refuse its actions");
          context.expectBlockProperty(SWITCH, MountedDeviceBlock.POWERED, false);
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 100)
  public void aLampSaysWhyItStopped(TestContext context) {
    context.setBlockState(SWITCH, floorMounted(ModBlocks.WALL_SWITCH, false));
    context.setBlockState(LAMP, ModBlocks.LIGHT_STRIP);
    layRun(context, 3, 5, 1, Hue.PLAIN);
    context.waitAndRun(
        SETTLE,
        () -> {
          context.assertTrue(
              statusAt(context, LAMP) == DeviceStatus.NO_SOURCE, "no source on its wire");
          context.setBlockState(at(2, 1), ModBlocks.LODESTONE_DYNAMO);
          engine(context).lay(new WireCell(floor(context, 2, 1), Hue.PLAIN));
        });
    context.waitAndRun(
        3L * SETTLE,
        () -> {
          context.assertTrue(
              statusAt(context, LAMP) == DeviceStatus.WIRE_OFF, "powered, but its wire is off");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 100)
  public void aSourceSaysWhenItsSwitchIsOpen(TestContext context) {
    cell(context, CELL, 100);
    engine(context).lay(new WireCell(floor(context, 1, 1), Hue.PLAIN));
    context.waitAndRun(
        SETTLE,
        () -> {
          SourceBlockEntity source = context.getBlockEntity(CELL);
          source.toggleSwitch(context.getWorld());
          context.assertTrue(
              statusAt(context, CELL) == DeviceStatus.SOURCES_OFF, "its own switch is open");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 100)
  public void aSourceLosingItsLastWireStops(TestContext context) {
    cell(context, CELL, 100);
    WireCell only = new WireCell(floor(context, 1, 1), Hue.PLAIN);
    engine(context).lay(only);
    SourceBlockEntity source = context.getBlockEntity(CELL);
    context.waitAndRun(
        SETTLE,
        () -> {
          context.assertTrue(source.isRunning(), "a wired cell with charge runs");
          engine(context).cut(only);
        });
    context.waitAndRun(
        3L * SETTLE,
        () -> {
          context.assertFalse(source.isRunning(), "with no wire left it must stop");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 100)
  public void twoThrowsInOneTickGiveOneResult(TestContext context) {
    BlockPos master = at(4, 2);
    BlockState state =
        ModBlocks.MASTER_SWITCH
            .getDefaultState()
            .with(MasterSwitchBlock.FACING, Direction.SOUTH)
            .with(MasterSwitchBlock.CLOSED, true);
    context.setBlockState(master, state);
    context.setBlockState(master.up(), state.with(MasterSwitchBlock.HALF, DoubleBlockHalf.UPPER));
    context.setBlockState(master.north(), Blocks.STONE);
    context.setBlockState(master.north().up(), Blocks.STONE);
    context.waitAndRun(
        SETTLE,
        () -> {
          MasterSwitchBlockEntity entity = context.getBlockEntity(master);
          BlockPos absolute = context.getAbsolutePos(master);
          entity.requestThrow(context.getWorld(), context.getWorld().getBlockState(absolute));
          entity.requestThrow(context.getWorld(), context.getWorld().getBlockState(absolute));
          context.expectBlockProperty(master, MasterSwitchBlock.CLOSED, false);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 120)
  public void aDeviceOnASealedAndAnOpenNetworkRunsIfEitherGivesPower(TestContext context) {
    UUID owner = UUID.randomUUID();
    context.setBlockState(LAMP, ModBlocks.LIGHT_STRIP);
    context.setBlockState(SWITCH, floorMounted(ModBlocks.WALL_SWITCH, true));
    for (int x = 3; x <= 5; x++) {
      engine(context).lay(new WireCell(floor(context, x, 1), Hue.RED, owner));
    }
    context.setBlockState(at(5, 3), ModBlocks.LODESTONE_DYNAMO);
    engine(context).lay(new WireCell(floor(context, 5, 1), Hue.BLUE));
    engine(context).lay(new WireCell(floor(context, 5, 2), Hue.BLUE));
    engine(context).lay(new WireCell(floor(context, 5, 3), Hue.BLUE));
    context.waitAndRun(
        HOLD,
        () -> {
          context.assertFalse(
              engine(context).grid().networksTouching(context.getAbsolutePos(LAMP)).isEmpty(),
              "both networks touch the lamp");
          context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, FULL);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 100)
  public void wireOnAnUnusedCircuitSideGivesNoPowerOrLoad(TestContext context) {
    BlockPos circuit = at(2, 1);
    context.setBlockState(circuit, ModBlocks.CIRCUIT_BLOCK);
    context.setBlockState(SWITCH, floorMounted(ModBlocks.WALL_SWITCH, true));
    context.setBlockState(at(5, 1), ModBlocks.LODESTONE_DYNAMO);
    layRun(context, 2, 5, 1, Hue.PLAIN);
    context.waitAndRun(
        HOLD,
        () -> {
          BlockPos absolute = context.getAbsolutePos(circuit);
          WireEngine engine = engine(context);
          context.assertTrue(engine.power().networksAt(absolute).isEmpty(), "no port, no network");
          context.assertFalse(
              engine.power().hasPower(absolute), "wire on its bottom gives no power");
          context.assertTrue(
              engine.power().networksAt(context.getAbsolutePos(at(5, 1))).stream()
                  .allMatch(network -> network.gridLoad() == 0),
              "and it adds no load");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 160)
  public void lampsStepDownOnADarkGrid(TestContext context) {
    cell(context, CELL, 100);
    context.setBlockState(SWITCH, floorMounted(ModBlocks.WALL_SWITCH, true));
    context.setBlockState(LAMP, ModBlocks.LIGHT_STRIP);
    layRun(context, 1, 5, 1, Hue.PLAIN);
    context.waitAndRun(
        HOLD,
        () -> {
          context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, FULL);
          SourceBlockEntity source = context.getBlockEntity(CELL);
          source.toggleSwitch(context.getWorld());
        });
    context.waitAndRun(
        HOLD + 3L,
        () -> {
          int level = context.getBlockState(LAMP).get(AbstractLampBlock.LEVEL);
          context.assertTrue(level > 0 && level < FULL, "a lamp dims first, level " + level);
        });
    context.waitAndRun(
        HOLD + 2L * SETTLE,
        () -> {
          context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, 0);
          context.complete();
        });
  }
}
