package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.block.AbstractLampBlock;
import com.grahambartley.hardwired.block.MasterSwitchBlock;
import com.grahambartley.hardwired.block.MountedDeviceBlock;
import com.grahambartley.hardwired.block.RedstoneCellBlockEntity;
import com.grahambartley.hardwired.block.RedstoneGeneratorBlockEntity;
import com.grahambartley.hardwired.block.SourceBlockEntity;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.power.GridRule;
import com.grahambartley.hardwired.power.StartOrder;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class PowerGridGameTest implements FabricGameTest {

  private static final String BENCH = "hardwired:bench";
  private static final int FLOOR_Y = -1;
  private static final int SETTLE = 10;
  private static final int HOLD = GridRule.MIN_DARK_TICKS + SETTLE;
  private static final int FULL = 15;
  private static final BlockPos CELL = at(1, 1);
  private static final BlockPos SECOND_CELL = at(2, 1);
  private static final BlockPos SWITCH = at(3, 1);
  private static final BlockPos LAMP = at(5, 1);

  private static BlockPos at(int x, int z) {
    return new BlockPos(x, FLOOR_Y + 1, z);
  }

  private static WireFace floor(TestContext context, int x, int z) {
    return new WireFace(context.getAbsolutePos(new BlockPos(x, FLOOR_Y, z)), Direction.UP);
  }

  private static void layRun(TestContext context, int fromX, int toX, int z, Hue hue) {
    WireEngine engine = WireEngine.of(context.getWorld());
    for (int x = fromX; x <= toX; x++) {
      engine.lay(new WireCell(floor(context, x, z), hue));
    }
  }

  private static BlockState floorSwitch(boolean on) {
    return ModBlocks.WALL_SWITCH
        .getDefaultState()
        .with(WallMountedBlock.FACE, BlockFace.FLOOR)
        .with(MountedDeviceBlock.POWERED, on);
  }

  private static RedstoneCellBlockEntity cell(TestContext context, BlockPos pos, int charge) {
    context.setBlockState(pos, ModBlocks.REDSTONE_CELL);
    RedstoneCellBlockEntity cell = context.getBlockEntity(pos);
    cell.setCharge(charge);
    return cell;
  }

  private static void lampRun(TestContext context) {
    context.setBlockState(SWITCH, floorSwitch(true));
    context.setBlockState(LAMP, ModBlocks.LIGHT_STRIP);
    layRun(context, 1, 5, 1, Hue.PLAIN);
  }

  private static void loads(TestContext context, int count) {
    for (int z = 3; z < 3 + count; z++) {
      context.setBlockState(
          at(5, z),
          ModBlocks.ALARM.getDefaultState().with(WallMountedBlock.FACE, BlockFace.FLOOR));
      WireEngine.of(context.getWorld()).lay(new WireCell(floor(context, 5, z), Hue.PLAIN));
    }
    if (count > 0) {
      WireEngine.of(context.getWorld()).lay(new WireCell(floor(context, 5, 2), Hue.PLAIN));
    }
  }

  @GameTest(templateName = BENCH)
  public void aLampAndSwitchWithNoSourceStayDark(TestContext context) {
    lampRun(context);
    context.waitAndRun(
        SETTLE,
        () -> {
          context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, 0);
          context.assertTrue(
              WireEngine.of(context.getWorld()).isOn(context.getAbsolutePos(LAMP)),
              "the switch still turns its network on");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void aFilledCellLightsTheLamp(TestContext context) {
    lampRun(context);
    cell(context, CELL, 100);
    context.waitAndRun(
        SETTLE,
        () -> {
          context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, FULL);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void anEmptyCellGivesNoPower(TestContext context) {
    lampRun(context);
    cell(context, CELL, 0);
    context.waitAndRun(
        SETTLE,
        () -> {
          context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, 0);
          context.assertTrue(
              WireEngine.of(context.getWorld())
                      .power()
                      .stopReason(context.getAbsolutePos(LAMP))
                  == DeviceStatus.OUT_OF_FUEL,
              "the reason must be fuel");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void twoCellsAddUpWhereOneOverloads(TestContext context) {
    lampRun(context);
    cell(context, CELL, 100);
    loads(context, 2);
    context.waitAndRun(
        SETTLE,
        () -> {
          context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, 0);
          context.assertTrue(
              WireEngine.of(context.getWorld())
                      .power()
                      .stopReason(context.getAbsolutePos(LAMP))
                  == DeviceStatus.OVERLOADED,
              "17 load must overload one cell");
          cell(context, SECOND_CELL, 100);
          context.setBlockState(SECOND_CELL.up(), Blocks.AIR);
        });
    context.waitAndRun(
        SETTLE + HOLD + SETTLE,
        () -> {
          context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, FULL);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 120)
  public void anOverloadComesBackBySelfOnceLoadDropsAndHoldsTwoSeconds(TestContext context) {
    lampRun(context);
    cell(context, CELL, 100);
    context.waitAndRun(
        SETTLE,
        () -> {
          context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, FULL);
          loads(context, 2);
        });
    context.waitAndRun(
        SETTLE + SETTLE,
        () -> {
          context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, 0);
          context.setBlockState(at(5, 3), Blocks.AIR);
          context.setBlockState(at(5, 4), Blocks.AIR);
        });
    context.waitAndRun(
        SETTLE + SETTLE + SETTLE,
        () -> context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, 0));
    context.waitAndRun(
        SETTLE + SETTLE + SETTLE + HOLD,
        () -> {
          context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, FULL);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void twoColoursTouchingOneDeviceShareSupplyButNotSignal(TestContext context) {
    context.setBlockState(LAMP, ModBlocks.LIGHT_STRIP);
    layRun(context, 5, 5, 1, Hue.PLAIN);
    context.setBlockState(
        at(5, 2), ModBlocks.ALARM.getDefaultState().with(WallMountedBlock.FACE, BlockFace.FLOOR));
    layRun(context, 5, 5, 2, Hue.PLAIN);
    WireEngine engine = WireEngine.of(context.getWorld());
    engine.lay(new WireCell(floor(context, 5, 2), Hue.BLUE));
    layRun(context, 1, 5, 3, Hue.BLUE);
    cell(context, at(1, 3), 100);
    context.waitAndRun(
        SETTLE,
        () -> {
          context.assertTrue(
              engine.power().hasPower(context.getAbsolutePos(LAMP)),
              "the blue source must reach plain wire through the shared Alarm");
          context.assertFalse(
              engine.isOn(context.getAbsolutePos(at(1, 3)), Hue.BLUE), "signal must not cross");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void aVanillaPistonRunsOnlyWithPower(TestContext context) {
    BlockPos piston = at(5, 1);
    context.setBlockState(SWITCH, floorSwitch(true));
    context.setBlockState(piston, Blocks.PISTON.getDefaultState().with(Properties.FACING, Direction.UP));
    layRun(context, 1, 5, 1, Hue.PLAIN);
    context.waitAndRun(
        SETTLE,
        () -> {
          context.expectBlockProperty(piston, Properties.EXTENDED, false);
          cell(context, CELL, 100);
        });
    context.waitAndRun(
        SETTLE + SETTLE,
        () -> {
          context.expectBlockProperty(piston, Properties.EXTENDED, true);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void aSourceSwitchTakesItOutOfTheGrid(TestContext context) {
    lampRun(context);
    RedstoneCellBlockEntity cell = cell(context, CELL, 100);
    context.waitAndRun(
        SETTLE,
        () -> {
          context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, FULL);
          cell.toggleSwitch(context.getWorld());
        });
    context.waitAndRun(
        SETTLE + SETTLE,
        () -> {
          context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, 0);
          context.assertTrue(
              WireEngine.of(context.getWorld())
                      .power()
                      .stopReason(context.getAbsolutePos(LAMP))
                  == DeviceStatus.SOURCES_OFF,
              "the reason must be its switch");
          context.complete();
        });
  }

  private static void masterAt(TestContext context, BlockPos pos, boolean closed) {
    BlockState state =
        ModBlocks.MASTER_SWITCH
            .getDefaultState()
            .with(MasterSwitchBlock.FACING, Direction.SOUTH)
            .with(MasterSwitchBlock.CLOSED, closed);
    context.setBlockState(pos, state);
    context.setBlockState(pos.up(), state.with(MasterSwitchBlock.HALF, DoubleBlockHalf.UPPER));
    context.setBlockState(pos.north(), Blocks.STONE);
    context.setBlockState(pos.north().up(), Blocks.STONE);
  }

  @GameTest(templateName = BENCH, tickLimit = 100)
  public void aMasterSwitchHoldsSourcesOffUntilItCloses(TestContext context) {
    lampRun(context);
    cell(context, CELL, 100);
    BlockPos master = at(4, 2);
    masterAt(context, master, false);
    WireEngine engine = WireEngine.of(context.getWorld());
    engine.lay(new WireCell(floor(context, 4, 2), Hue.PLAIN));
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, at(4, 4));
    context.waitAndRun(
        SETTLE,
        () -> {
          context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, 0);
          player.hold(ItemStack.EMPTY, ItemStack.EMPTY);
          player.useOn(new WireFace(context.getAbsolutePos(master), Direction.SOUTH));
          context.expectBlockProperty(master, MasterSwitchBlock.CLOSED, true);
        });
    context.waitAndRun(
        SETTLE + StartOrder.START_TICKS / 2L,
        () -> context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, 0));
    context.waitAndRun(
        SETTLE + StartOrder.START_TICKS / 2L + StartOrder.START_TICKS + SETTLE,
        () -> {
          context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, FULL);
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 100)
  public void anOpenSourceSwitchStaysOffWhenTheMasterCloses(TestContext context) {
    lampRun(context);
    RedstoneCellBlockEntity cell = cell(context, CELL, 100);
    cell.toggleSwitch(context.getWorld());
    masterAt(context, at(4, 2), true);
    WireEngine.of(context.getWorld()).lay(new WireCell(floor(context, 4, 2), Hue.PLAIN));
    context.waitAndRun(
        SETTLE,
        () -> {
          context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, 0);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void aFuelFreeDynamoRunsWithoutRedstone(TestContext context) {
    lampRun(context);
    context.setBlockState(CELL, ModBlocks.LODESTONE_DYNAMO);
    context.waitAndRun(
        SETTLE,
        () -> {
          context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, FULL);
          SourceBlockEntity dynamo = context.getBlockEntity(CELL);
          context.assertTrue(dynamo.isRunning(), "the dynamo must be running");
          context.assertTrue(dynamo.load() == 1 && dynamo.supply() == 24, "wrong totals");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void aHopperFeedsAGeneratorAndLosesNothing(TestContext context) {
    BlockPos generator = at(3, 3);
    context.setBlockState(generator, ModBlocks.REDSTONE_GENERATOR);
    context.setBlockState(
        generator.up(), Blocks.HOPPER.getDefaultState().with(Properties.HOPPER_FACING, Direction.DOWN));
    HopperBlockEntity hopper = context.getBlockEntity(generator.up());
    hopper.setStack(0, new ItemStack(Items.REDSTONE, 2));
    hopper.setStack(1, new ItemStack(Items.COAL, 1));
    context.waitAndRun(
        40,
        () -> {
          RedstoneGeneratorBlockEntity engine = context.getBlockEntity(generator);
          context.assertTrue(engine.charge() == 20, "two dust must give 20, got " + engine.charge());
          context.assertTrue(hopper.getStack(0).isEmpty(), "dust must go in");
          context.assertTrue(hopper.getStack(1).isOf(Items.COAL), "coal must stay in the hopper");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void sealedWireSharesPowerOnlyWithItsOwner(TestContext context) {
    WireEngine engine = WireEngine.of(context.getWorld());
    UUID owner = UUID.randomUUID();
    context.setBlockState(LAMP, ModBlocks.LIGHT_STRIP);
    engine.lay(new WireCell(floor(context, 5, 1), Hue.PLAIN, owner));
    engine.lay(new WireCell(floor(context, 5, 1), Hue.BLUE));
    for (int x = 1; x <= 4; x++) {
      engine.lay(new WireCell(floor(context, x, 1), Hue.BLUE));
    }
    cell(context, CELL, 100);
    context.waitAndRun(
        SETTLE,
        () -> {
          WireCell sealed = new WireCell(floor(context, 5, 1), Hue.PLAIN, owner);
          WireCell open = new WireCell(floor(context, 5, 1), Hue.BLUE);
          context.assertFalse(
              engine.grid().networkOf(sealed).gridState().givesPower(),
              "open wire must not power a stranger's seal");
          context.assertTrue(
              engine.grid().networkOf(open).gridState().givesPower(),
              "the open wire keeps its own power");
          context.complete();
        });
  }
}
