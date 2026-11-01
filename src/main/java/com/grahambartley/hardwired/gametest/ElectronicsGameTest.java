package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.AlarmBlock;
import com.grahambartley.hardwired.block.ElectronicBlockEntity;
import com.grahambartley.hardwired.block.IntercomBlock;
import com.grahambartley.hardwired.block.RedstoneCellBlockEntity;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.IntercomActions;
import com.grahambartley.hardwired.network.IntercomSayPayload;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.AfterBatch;
import net.minecraft.test.BeforeBatch;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ElectronicsGameTest implements FabricGameTest {

  private static final String BENCH = "hardwired:bench";
  private static final String RANGE_BATCH = "intercom-range";
  private static final int FLOOR_Y = -1;
  private static final int SETTLE_TICKS = 4;
  private static final int BEAT_TICKS = 24;
  private static final int SHORT_RANGE = 2;
  private static final BlockPos CELL = at(1, 1);
  private static final BlockPos SWITCH = at(7, 1);
  private static final BlockPos ALARM = at(4, 1);
  private static final Identifier LIGHTS_OUT = Identifier.of(Hardwired.MOD_ID, "lights_out");

  private static HardwiredConfig savedConfig;

  private static BlockPos at(int x, int z) {
    return new BlockPos(x, FLOOR_Y + 1, z);
  }

  private static WireCell wire(TestContext context, int x) {
    return new WireCell(
        new WireFace(context.getAbsolutePos(new BlockPos(x, FLOOR_Y, 1)), Direction.UP), Hue.PLAIN);
  }

  private static RedstoneCellBlockEntity wiredCell(TestContext context, int charge) {
    context.setBlockState(CELL, ModBlocks.REDSTONE_CELL);
    WireEngine engine = WireEngine.of(context.getWorld());
    for (int x = 1; x <= 7; x++) {
      engine.lay(wire(context, x));
    }
    RedstoneCellBlockEntity cell = context.getBlockEntity(CELL);
    cell.setCharge(charge);
    return cell;
  }

  private static void placeFloor(
      TestContext context, BlockPos pos, net.minecraft.block.Block block) {
    context.setBlockState(
        pos, block.getDefaultState().with(WallMountedBlock.FACE, BlockFace.FLOOR));
  }

  private static DeviceStatus status(TestContext context, BlockPos pos) {
    ElectronicBlockEntity entity = context.getBlockEntity(pos);
    return entity.status();
  }

  private static void expectStatus(TestContext context, BlockPos pos, DeviceStatus expected) {
    DeviceStatus actual = status(context, pos);
    context.assertTrue(actual == expected, "expected " + expected + " but was " + actual);
  }

  private static RedstoneCellBlockEntity buildAlarm(TestContext context, int charge) {
    RedstoneCellBlockEntity cell = wiredCell(context, charge);
    placeFloor(context, SWITCH, ModBlocks.WALL_SWITCH);
    placeFloor(context, ALARM, ModBlocks.ALARM);
    return cell;
  }

  private static ConnectedTestPlayer playerAt(TestContext context, int x) {
    return ConnectedTestPlayer.joinAt(context, at(x, 3));
  }

  @GameTest(templateName = BENCH, tickLimit = 240)
  public void anAlarmStopsOutOfChargeAndRestartsWhenCharged(TestContext context) {
    RedstoneCellBlockEntity cell = buildAlarm(context, 1);
    ConnectedTestPlayer player = playerAt(context, 4);
    context.waitAndRun(SETTLE_TICKS, () -> context.useBlock(SWITCH));
    context.waitAndRun(
        SETTLE_TICKS * 4L,
        () -> {
          context.expectBlockProperty(ALARM, AlarmBlock.POWERED, true);
          expectStatus(context, ALARM, DeviceStatus.RUNNING);
        });
    context.waitAndRun(
        SETTLE_TICKS + 5L * BEAT_TICKS,
        () -> {
          context.expectBlockProperty(ALARM, AlarmBlock.POWERED, false);
          expectStatus(context, ALARM, DeviceStatus.OUT_OF_FUEL);
          context.assertTrue(cell.charge() == 0, "the charge must be used up");
          context.assertTrue(player.hasEarned(LIGHTS_OUT), "the last stop must earn Lights Out");
          cell.fill(50);
        });
    context.waitAndRun(
        SETTLE_TICKS + 9L * BEAT_TICKS,
        () -> {
          context.expectBlockProperty(ALARM, AlarmBlock.POWERED, true);
          expectStatus(context, ALARM, DeviceStatus.RUNNING);
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 100)
  public void anAlarmOnAnOffWireUsesNoCharge(TestContext context) {
    RedstoneCellBlockEntity cell = buildAlarm(context, 50);
    context.waitAndRun(
        2L * BEAT_TICKS,
        () -> {
          context.expectBlockProperty(ALARM, AlarmBlock.POWERED, false);
          expectStatus(context, ALARM, DeviceStatus.WIRE_OFF);
          context.assertTrue(cell.charge() == 50, "an idle alarm must use nothing");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 60)
  public void anAlarmWithNoCellSaysSo(TestContext context) {
    WireEngine engine = WireEngine.of(context.getWorld());
    for (int x = 4; x <= 7; x++) {
      engine.lay(wire(context, x));
    }
    placeFloor(context, SWITCH, ModBlocks.WALL_SWITCH);
    placeFloor(context, ALARM, ModBlocks.ALARM);
    context.waitAndRun(SETTLE_TICKS, () -> context.useBlock(SWITCH));
    context.waitAndRun(
        SETTLE_TICKS * 3L,
        () -> {
          expectStatus(context, ALARM, DeviceStatus.NO_SOURCE);
          context.complete();
        });
  }

  private static RedstoneCellBlockEntity buildIntercoms(TestContext context, int charge) {
    RedstoneCellBlockEntity cell = wiredCell(context, charge);
    context.setBlockState(at(2, 1), ModBlocks.INTERCOM);
    context.setBlockState(at(7, 1), ModBlocks.INTERCOM);
    return cell;
  }

  private static void say(TestContext context, ConnectedTestPlayer player, String line) {
    IntercomActions.say(
        player.player(), new IntercomSayPayload(context.getAbsolutePos(at(2, 1)), line));
  }

  private static String lastLine(ConnectedTestPlayer player) {
    List<Text> lines = player.intercomLines();
    return lines.isEmpty() ? "" : lines.get(lines.size() - 1).getString();
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void aLineReachesPlayersAndRingsEveryIntercomOnce(TestContext context) {
    RedstoneCellBlockEntity cell = buildIntercoms(context, 10);
    ConnectedTestPlayer speaker = playerAt(context, 2);
    ConnectedTestPlayer listener = playerAt(context, 7);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          say(context, speaker, "§chello\u0007");
          context.assertTrue(lastLine(listener).contains("hello"), lastLine(listener));
          context.assertFalse(lastLine(listener).contains("§"), "codes must be stripped");
          context.assertTrue(lastLine(speaker).contains("hello"), "the speaker hears it too");
          context.expectBlockProperty(at(2, 1), IntercomBlock.ACTIVE, true);
          context.expectBlockProperty(at(7, 1), IntercomBlock.ACTIVE, true);
        });
    context.waitAndRun(
        3L * BEAT_TICKS,
        () -> {
          context.expectBlockProperty(at(2, 1), IntercomBlock.ACTIVE, false);
          context.expectBlockProperty(at(7, 1), IntercomBlock.ACTIVE, false);
          speaker.leave();
          listener.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 60)
  public void chatRulesStopALine(TestContext context) {
    TestPower.keepFed(context);
    RedstoneCellBlockEntity cell = buildIntercoms(context, 10);
    ConnectedTestPlayer hidden = playerAt(context, 2);
    ConnectedTestPlayer deaf = playerAt(context, 3);
    ConnectedTestPlayer open = playerAt(context, 4);
    hidden.setChatVisibility(ChatVisibility.HIDDEN);
    deaf.setChatVisibility(ChatVisibility.SYSTEM);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          say(context, hidden, "muted");
          context.assertTrue(open.intercomLines().isEmpty(), "a hidden player must not send");
          context.assertTrue(cell.charge() == 10, "a refused line must cost nothing");
          say(context, open, "hello");
          context.assertTrue(deaf.intercomLines().isEmpty(), "chat off must not receive a line");
          context.assertTrue(open.intercomLines().size() == 1, "the speaker must hear it");
          say(context, open, "again");
          context.assertTrue(
              open.intercomLines().size() == 1, "a second line must be rate limited");
          say(context, deaf, "   ");
          hidden.leave();
          deaf.leave();
          open.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 60)
  public void anIntercomWithNoChargeSendsNothingAndSaysWhy(TestContext context) {
    buildIntercoms(context, 0);
    ConnectedTestPlayer player = playerAt(context, 2);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          say(context, player, "hello");
          context.assertTrue(player.intercomLines().isEmpty(), "no charge means no line");
          expectStatus(context, at(2, 1), DeviceStatus.OUT_OF_FUEL);
          RedstoneCellBlockEntity cell = context.getBlockEntity(CELL);
          cell.fill(5);
        });
    context.waitAndRun(
        SETTLE_TICKS + BEAT_TICKS,
        () -> {
          expectStatus(context, at(2, 1), DeviceStatus.READY);
          player.leave();
          context.complete();
        });
  }

  @BeforeBatch(batchId = RANGE_BATCH)
  public void shortenRange(ServerWorld world) {
    savedConfig = Hardwired.SERVER_CONFIG;
    Hardwired.SERVER_CONFIG =
        savedConfig.withDevice(savedConfig.device().withIntercomRangeBlocks(SHORT_RANGE));
  }

  @AfterBatch(batchId = RANGE_BATCH)
  public void restoreRange(ServerWorld world) {
    Hardwired.SERVER_CONFIG = savedConfig;
  }

  @GameTest(templateName = BENCH, batchId = RANGE_BATCH, tickLimit = 40)
  public void onlyPlayersInRangeOfAnIntercomHearALine(TestContext context) {
    buildIntercoms(context, 10);
    ConnectedTestPlayer speaker = ConnectedTestPlayer.joinAt(context, at(2, 2));
    ConnectedTestPlayer near = ConnectedTestPlayer.joinAt(context, at(7, 2));
    ConnectedTestPlayer between = ConnectedTestPlayer.joinAt(context, at(4, 6));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          say(context, speaker, "hello");
          context.assertTrue(near.intercomLines().size() == 1, "the far intercom must carry it");
          context.assertTrue(between.intercomLines().isEmpty(), "out of range must hear nothing");
          speaker.leave();
          near.leave();
          between.leave();
          context.complete();
        });
  }
}
