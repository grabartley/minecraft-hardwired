package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.AlarmBlock;
import com.grahambartley.hardwired.block.RedstoneCellBlock;
import com.grahambartley.hardwired.block.RedstoneCellBlockEntity;
import com.grahambartley.hardwired.block.SolarPanelBlock;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.registry.ModComponents;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.AfterBatch;
import net.minecraft.test.BeforeBatch;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameRules;

public class PowerGameTest implements FabricGameTest {

  private static final String BENCH = "hardwired:bench";
  private static final String DAY_BATCH = "solar-day";
  private static final String NIGHT_BATCH = "solar-night";
  private static final int FLOOR_Y = -1;
  private static final int NOON = 6000;
  private static final int MIDNIGHT = 18000;
  private static final int FAST_SOLAR = 600;
  private static final int SETTLE_TICKS = 4;
  private static final int SOLAR_TICKS = 100;
  private static final int LIGHTNING_TICKS = 12;
  private static final BlockPos CELL = at(1, 1);
  private static final BlockPos FAR_END = at(6, 1);
  private static final Identifier CHARGED_UP = id("charged_up");
  private static final Identifier SUN_POWER = id("sun_power");
  private static final Identifier CAUGHT_LIGHTNING = id("caught_lightning");

  private static HardwiredConfig savedConfig;

  private static Identifier id(String path) {
    return Identifier.of(Hardwired.MOD_ID, path);
  }

  private static BlockPos at(int x, int z) {
    return new BlockPos(x, FLOOR_Y + 1, z);
  }

  private static WireFace floor(TestContext context, int x, int z) {
    return new WireFace(context.getAbsolutePos(new BlockPos(x, FLOOR_Y, z)), Direction.UP);
  }

  private static void layRun(TestContext context, int fromX, int toX) {
    WireEngine engine = WireEngine.of(context.getWorld());
    for (int x = fromX; x <= toX; x++) {
      engine.lay(new WireCell(floor(context, x, 1), Hue.PLAIN));
    }
  }

  private static RedstoneCellBlockEntity cell(TestContext context) {
    context.setBlockState(CELL, ModBlocks.REDSTONE_CELL);
    return context.getBlockEntity(CELL);
  }

  private static RedstoneCellBlockEntity wiredCell(TestContext context) {
    RedstoneCellBlockEntity cell = cell(context);
    layRun(context, 1, 6);
    return cell;
  }

  private static ConnectedTestPlayer playerBeside(TestContext context, int x) {
    return ConnectedTestPlayer.joinAt(context, at(x, 3));
  }

  private static int count(ConnectedTestPlayer player) {
    return player.player().getMainHandStack().getCount();
  }

  @GameTest(templateName = BENCH)
  public void twoPlayersFillingOneCellInOneTickConserveDust(TestContext context) {
    RedstoneCellBlockEntity cell = cell(context);
    int dustCharge = Hardwired.SERVER_CONFIG.power().dustCharge();
    int capacity = cell.capacity();
    cell.setCharge(capacity - dustCharge - 1);
    int before = cell.charge();
    ConnectedTestPlayer first = playerBeside(context, 0);
    ConnectedTestPlayer second = playerBeside(context, 2);
    first.hold(new ItemStack(Items.REDSTONE, 3), ItemStack.EMPTY);
    second.hold(new ItemStack(Items.REDSTONE, 3), ItemStack.EMPTY);
    WireFace top = new WireFace(context.getAbsolutePos(CELL), Direction.UP);
    first.useOn(top);
    second.useOn(top);
    context.assertTrue(
        count(first) + count(second) == 5, "one dust must go, not " + count(first) + count(second));
    context.assertTrue(cell.charge() == before + dustCharge, "one dust must add its charge once");
    context.assertTrue(cell.charge() <= capacity, "a cell must never overcharge");
    first.leave();
    second.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void aFullCellRefusesRedstoneAndUsesNone(TestContext context) {
    RedstoneCellBlockEntity cell = cell(context);
    cell.setCharge(cell.capacity());
    ConnectedTestPlayer player = playerBeside(context, 0);
    player.hold(new ItemStack(Items.REDSTONE_BLOCK, 2), ItemStack.EMPTY);
    player.useOn(new WireFace(context.getAbsolutePos(CELL), Direction.UP));
    context.assertTrue(count(player) == 2, "a refused block must stay in the hand");
    context.assertTrue(cell.charge() == cell.capacity(), "charge must not move");
    context.assertFalse(player.hasEarned(CHARGED_UP), "a refused fill earns nothing");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void aRedstoneBlockIsWorthNineDustAndEarnsChargedUp(TestContext context) {
    RedstoneCellBlockEntity cell = cell(context);
    ConnectedTestPlayer player = playerBeside(context, 0);
    player.hold(new ItemStack(Items.REDSTONE_BLOCK, 1), ItemStack.EMPTY);
    player.useOn(new WireFace(context.getAbsolutePos(CELL), Direction.UP));
    context.assertTrue(
        cell.charge() == 9 * Hardwired.SERVER_CONFIG.power().dustCharge(),
        "wrong charge " + cell.charge());
    context.assertTrue(player.player().getMainHandStack().isEmpty(), "the block must be used");
    context.assertTrue(player.hasEarned(CHARGED_UP), "filling a cell earns Charged Up");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void theLevelFollowsTheCharge(TestContext context) {
    RedstoneCellBlockEntity cell = cell(context);
    int capacity = cell.capacity();
    context.expectBlockProperty(CELL, RedstoneCellBlock.LEVEL, 0);
    cell.setCharge(capacity);
    context.expectBlockProperty(CELL, RedstoneCellBlock.LEVEL, 4);
    cell.setCharge(capacity / 2);
    context.expectBlockProperty(CELL, RedstoneCellBlock.LEVEL, 2);
    cell.setCharge(1);
    context.expectBlockProperty(CELL, RedstoneCellBlock.LEVEL, 1);
    cell.setCharge(0);
    context.expectBlockProperty(CELL, RedstoneCellBlock.LEVEL, 0);
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void chargeDropsWithTheCellAndComesBackWhenPlaced(TestContext context) {
    RedstoneCellBlockEntity cell = cell(context);
    int charge = cell.capacity() / 2 + 7;
    cell.setCharge(charge);
    context.getWorld().breakBlock(context.getAbsolutePos(CELL), true);
    List<ItemEntity> drops =
        context
            .getWorld()
            .getEntitiesByClass(
                ItemEntity.class, new Box(context.getAbsolutePos(CELL)).expand(2), e -> true);
    context.assertTrue(drops.size() == 1, "one cell must drop, got " + drops.size());
    ItemStack dropped = drops.get(0).getStack();
    context.assertTrue(
        dropped.getOrDefault(ModComponents.CELL_CHARGE, 0) == charge, "the charge must drop");
    drops.get(0).discard();
    ConnectedTestPlayer player = playerBeside(context, 0);
    player.hold(dropped.copy(), ItemStack.EMPTY);
    player.useOn(floor(context, 1, 1));
    RedstoneCellBlockEntity placed = context.getBlockEntity(CELL);
    context.assertTrue(placed.charge() == charge, "placing must bring the charge back");
    context.expectBlockProperty(CELL, RedstoneCellBlock.LEVEL, 3);
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH, tickLimit = 120)
  public void chargeNeverRisesFromRunningOrRewiring(TestContext context) {
    RedstoneCellBlockEntity cell = wiredCell(context);
    cell.setCharge(50);
    context.setBlockState(
        at(6, 1),
        ModBlocks.WALL_SWITCH.getDefaultState().with(WallMountedBlock.FACE, BlockFace.FLOOR));
    context.setBlockState(
        at(4, 1), ModBlocks.ALARM.getDefaultState().with(WallMountedBlock.FACE, BlockFace.FLOOR));
    int[] last = {cell.charge()};
    context.runAtEveryTick(
        () -> {
          context.assertTrue(cell.charge() <= last[0], "charge rose to " + cell.charge());
          last[0] = cell.charge();
        });
    WireCell gap = new WireCell(floor(context, 3, 1), Hue.PLAIN);
    context.waitAndRun(SETTLE_TICKS, () -> context.useBlock(at(6, 1)));
    context.waitAndRun(30, () -> WireEngine.of(context.getWorld()).cut(gap));
    context.waitAndRun(50, () -> WireEngine.of(context.getWorld()).lay(gap));
    context.waitAndRun(70, () -> context.useBlock(at(6, 1)));
    context.waitAndRun(
        100,
        () -> {
          context.assertTrue(cell.charge() < 50, "the alarm must have used charge");
          context.expectBlockProperty(at(4, 1), AlarmBlock.POWERED, false);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void lightningOnAWiredRodFillsTheCellOnce(TestContext context) {
    RedstoneCellBlockEntity cell = wiredCell(context);
    ConnectedTestPlayer player = playerBeside(context, 1);
    context.setBlockState(FAR_END, Blocks.LIGHTNING_ROD);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.spawnEntity(EntityType.LIGHTNING_BOLT, FAR_END.up());
          context.waitAndRun(
              LIGHTNING_TICKS * 4,
              () -> {
                context.assertTrue(
                    cell.charge() == Hardwired.SERVER_CONFIG.power().lightningCharge(),
                    "one strike must fill once, got " + cell.charge());
                context.assertTrue(player.hasEarned(CAUGHT_LIGHTNING), "a strike must earn it");
                player.leave();
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 40)
  public void lightningOnARodWithNoWireChargesNothing(TestContext context) {
    RedstoneCellBlockEntity cell = cell(context);
    context.setBlockState(FAR_END, Blocks.LIGHTNING_ROD);
    context.spawnEntity(EntityType.LIGHTNING_BOLT, FAR_END.up());
    context.waitAndRun(
        LIGHTNING_TICKS,
        () -> {
          context.assertTrue(cell.charge() == 0, "an unwired rod must not reach the cell");
          context.complete();
        });
  }

  @BeforeBatch(batchId = DAY_BATCH)
  public void pinNoon(ServerWorld world) {
    pinTime(world, NOON);
  }

  @BeforeBatch(batchId = NIGHT_BATCH)
  public void pinMidnight(ServerWorld world) {
    pinTime(world, MIDNIGHT);
  }

  private void pinTime(ServerWorld world, int time) {
    world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, world.getServer());
    world.setTimeOfDay(time);
    savedConfig = Hardwired.SERVER_CONFIG;
    Hardwired.SERVER_CONFIG =
        savedConfig.withPower(savedConfig.power().withChargePerLoadMinute(FAST_SOLAR));
  }

  @AfterBatch(batchId = DAY_BATCH)
  public void endDay(ServerWorld world) {
    releaseTime(world);
  }

  @AfterBatch(batchId = NIGHT_BATCH)
  public void endNight(ServerWorld world) {
    releaseTime(world);
  }

  private void releaseTime(ServerWorld world) {
    world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(true, world.getServer());
    Hardwired.SERVER_CONFIG = savedConfig;
  }

  private static void assertSolar(TestContext context, RedstoneCellBlockEntity cell, boolean day) {
    BlockState panel = context.getBlockState(FAR_END);
    context.assertTrue(panel.get(SolarPanelBlock.LIT) == day, "panel lit should be " + day);
    context.assertTrue((cell.charge() > 0) == day, "cell charge was " + cell.charge());
  }

  @GameTest(templateName = BENCH, batchId = DAY_BATCH, skyAccess = true, tickLimit = 160)
  public void aSolarPanelFillsCellsByDay(TestContext context) {
    RedstoneCellBlockEntity cell = wiredCell(context);
    ConnectedTestPlayer player = playerBeside(context, 5);
    context.setBlockState(FAR_END, ModBlocks.SOLAR_PANEL);
    context.waitAndRun(
        SOLAR_TICKS,
        () -> {
          assertSolar(context, cell, true);
          context.assertTrue(player.hasEarned(SUN_POWER), "a panel filling a cell earns it");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = NIGHT_BATCH, skyAccess = true, tickLimit = 160)
  public void aSolarPanelRestsAtNight(TestContext context) {
    RedstoneCellBlockEntity cell = wiredCell(context);
    context.setBlockState(FAR_END, ModBlocks.SOLAR_PANEL);
    context.waitAndRun(
        SOLAR_TICKS,
        () -> {
          assertSolar(context, cell, false);
          context.complete();
        });
  }
}
