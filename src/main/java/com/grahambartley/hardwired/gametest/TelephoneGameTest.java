package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.ExchangeBlock;
import com.grahambartley.hardwired.block.ExchangeLayout;
import com.grahambartley.hardwired.block.ExchangePart;
import com.grahambartley.hardwired.block.RedstoneCellBlockEntity;
import com.grahambartley.hardwired.block.TelephoneBlock;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.network.CallVoicePayload;
import com.grahambartley.hardwired.network.ExchangeSharePayload;
import com.grahambartley.hardwired.network.PhoneActionPayload;
import com.grahambartley.hardwired.network.PhoneViewPayload;
import com.grahambartley.hardwired.phone.CallVoice;
import com.grahambartley.hardwired.phone.ExchangeActions;
import com.grahambartley.hardwired.phone.LineStatus;
import com.grahambartley.hardwired.phone.PhoneActions;
import com.grahambartley.hardwired.phone.PhoneDirectory;
import com.grahambartley.hardwired.phone.PhoneEntry;
import com.grahambartley.hardwired.phone.PhoneNumbering;
import com.grahambartley.hardwired.phone.Switchboard;
import com.grahambartley.hardwired.radio.AdpcmCodec;
import com.grahambartley.hardwired.radio.VoiceFormat;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.AfterBatch;
import net.minecraft.test.BeforeBatch;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

public class TelephoneGameTest implements FabricGameTest {

  private static final String BENCH = "hardwired:bench";
  private static final String VOICE_OFF_BATCH = "telephone-voice-off";
  private static final int FLOOR_Y = -1;
  private static final int SETTLE_TICKS = 25;
  private static final BlockPos CELL = at(1, 1);
  private static final BlockPos FIRST = at(2, 1);
  private static final BlockPos EXCHANGE = at(4, 1);
  private static final BlockPos SECOND = at(6, 1);
  private static final BlockPos THIRD = at(7, 1);
  private static final Identifier NUMBER_PLEASE = Identifier.of(Hardwired.MOD_ID, "number_please");
  private static final Identifier PARTY_LINE = Identifier.of(Hardwired.MOD_ID, "party_line");

  private static HardwiredConfig savedConfig;

  private static BlockPos at(int x, int z) {
    return new BlockPos(x, FLOOR_Y + 1, z);
  }

  private static RedstoneCellBlockEntity buildNetwork(TestContext context) {
    context.setBlockState(CELL, ModBlocks.REDSTONE_CELL);
    WireEngine engine = WireEngine.of(context.getWorld());
    for (int x = 1; x <= 7; x++) {
      engine.lay(
          new WireCell(
              new WireFace(context.getAbsolutePos(new BlockPos(x, FLOOR_Y, 1)), Direction.UP),
              Hue.PLAIN));
    }
    RedstoneCellBlockEntity cell = context.getBlockEntity(CELL);
    cell.setCharge(500);
    placePhone(context, FIRST);
    placeExchange(context);
    placePhone(context, SECOND);
    placePhone(context, THIRD);
    return cell;
  }

  private static void placePhone(TestContext context, BlockPos pos) {
    context.setBlockState(
        pos,
        ModBlocks.CANDLESTICK_TELEPHONE
            .getDefaultState()
            .with(TelephoneBlock.FACING, Direction.NORTH));
  }

  private static void placeExchange(TestContext context) {
    for (ExchangePart part : ExchangePart.values()) {
      context.setBlockState(
          ExchangeLayout.cell(part, Direction.SOUTH, EXCHANGE),
          ModBlocks.EXCHANGE
              .getDefaultState()
              .with(ExchangeBlock.FACING, Direction.SOUTH)
              .with(ExchangeBlock.PART, part));
    }
  }

  private static long abs(TestContext context, BlockPos pos) {
    return context.getAbsolutePos(pos).asLong();
  }

  private static int numberOf(TestContext context, BlockPos pos) {
    return PhoneDirectory.of(context.getWorld())
        .entry(abs(context, pos))
        .map(PhoneEntry::number)
        .orElse(PhoneNumbering.NONE);
  }

  private static LineStatus lastStatus(ConnectedTestPlayer player) {
    List<PhoneViewPayload> views = player.phoneViews();
    return views.isEmpty() ? null : LineStatus.byIndex(views.getLast().status());
  }

  private static void expectStatus(
      TestContext context, ConnectedTestPlayer player, LineStatus expected) {
    LineStatus actual = lastStatus(player);
    context.assertTrue(actual == expected, "expected " + expected + " but saw " + actual);
  }

  private static void pickUp(TestContext context, ConnectedTestPlayer player, BlockPos phone) {
    PhoneActions.use(player.player(), context.getAbsolutePos(phone));
  }

  private static void dial(
      TestContext context, ConnectedTestPlayer player, BlockPos phone, int number) {
    PhoneActions.act(
        player.player(), PhoneActionPayload.dial(context.getAbsolutePos(phone), number));
  }

  private static void leaveAll(ConnectedTestPlayer... players) {
    for (ConnectedTestPlayer player : players) {
      player.leave();
    }
  }

  @GameTest(templateName = BENCH, tickLimit = 60)
  public void phonesTakeNumbersUnderTheirExchangeInWiringOrder(TestContext context) {
    buildNetwork(context);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.assertTrue(numberOf(context, EXCHANGE) == 1, "the Exchange must take digit 1");
          context.assertTrue(numberOf(context, FIRST) == 100, "the first phone must be 100");
          context.assertTrue(
              numberOf(context, SECOND) != numberOf(context, THIRD),
              "two phones wired in one tick must get different numbers");
          context.assertTrue(numberOf(context, SECOND) == 101, "the second phone must be 101");
          context.assertTrue(numberOf(context, THIRD) == 102, "the third phone must be 102");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void cuttingTheExchangeOffLeavesAPhoneWithNoNumber(TestContext context) {
    buildNetwork(context);
    context.waitAndRun(SETTLE_TICKS, () -> context.removeBlock(EXCHANGE));
    context.waitAndRun(
        SETTLE_TICKS * 2L,
        () -> {
          context.assertTrue(
              numberOf(context, FIRST) == PhoneNumbering.NONE, "no Exchange must mean no number");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 120)
  public void aDialledPhoneRingsAndAnsweringConnectsBothSides(TestContext context) {
    buildNetwork(context);
    ConnectedTestPlayer caller = ConnectedTestPlayer.joinAt(context, at(2, 3));
    ConnectedTestPlayer answerer = ConnectedTestPlayer.joinAt(context, at(6, 3));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          pickUp(context, caller, FIRST);
          expectStatus(context, caller, LineStatus.DIAL_TONE);
          context.expectBlockProperty(FIRST, TelephoneBlock.LIFTED, true);
          dial(context, caller, FIRST, numberOf(context, SECOND));
          expectStatus(context, caller, LineStatus.RINGING_OUT);
        });
    context.waitAndRun(
        SETTLE_TICKS + 2L,
        () -> {
          context.expectBlockProperty(SECOND, TelephoneBlock.RINGING, true);
          pickUp(context, answerer, SECOND);
          expectStatus(context, caller, LineStatus.CONNECTED);
          expectStatus(context, answerer, LineStatus.CONNECTED);
          context.expectBlockProperty(SECOND, TelephoneBlock.RINGING, false);
          context.expectBlockProperty(SECOND, TelephoneBlock.LIFTED, true);
          context.assertTrue(caller.hasEarned(NUMBER_PLEASE), "the caller must earn the call");
          leaveAll(caller, answerer);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 100)
  public void aTypedLineReachesOnlyThePlayersOnTheCall(TestContext context) {
    buildNetwork(context);
    ConnectedTestPlayer caller = ConnectedTestPlayer.joinAt(context, at(2, 3));
    ConnectedTestPlayer answerer = ConnectedTestPlayer.joinAt(context, at(6, 3));
    ConnectedTestPlayer bystander = ConnectedTestPlayer.joinAt(context, at(7, 4));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          pickUp(context, caller, FIRST);
          dial(context, caller, FIRST, numberOf(context, SECOND));
          pickUp(context, answerer, SECOND);
          PhoneActions.act(
              caller.player(), PhoneActionPayload.say(context.getAbsolutePos(FIRST), "hello"));
        });
    context.waitAndRun(
        SETTLE_TICKS + 2L,
        () -> {
          context.assertTrue(caller.phoneLines().size() == 1, "the speaker must hear the line");
          context.assertTrue(answerer.phoneLines().size() == 1, "the far side must hear it");
          context.assertTrue(bystander.phoneLines().isEmpty(), "a bystander must hear nothing");
          leaveAll(caller, answerer, bystander);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 100)
  public void anUnknownNumberAndABusyLineSaySo(TestContext context) {
    buildNetwork(context);
    ConnectedTestPlayer caller = ConnectedTestPlayer.joinAt(context, at(2, 3));
    ConnectedTestPlayer other = ConnectedTestPlayer.joinAt(context, at(6, 3));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          pickUp(context, caller, FIRST);
          dial(context, caller, FIRST, 999);
          expectStatus(context, caller, LineStatus.NO_SUCH_NUMBER);
          pickUp(context, other, SECOND);
        });
    context.waitAndRun(
        SETTLE_TICKS + 22L,
        () -> {
          dial(context, caller, FIRST, numberOf(context, SECOND));
          expectStatus(context, caller, LineStatus.BUSY);
          leaveAll(caller, other);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 100)
  public void hangingUpEndsTheCallForTheOtherSide(TestContext context) {
    buildNetwork(context);
    ConnectedTestPlayer caller = ConnectedTestPlayer.joinAt(context, at(2, 3));
    ConnectedTestPlayer answerer = ConnectedTestPlayer.joinAt(context, at(6, 3));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          pickUp(context, caller, FIRST);
          dial(context, caller, FIRST, numberOf(context, SECOND));
          pickUp(context, answerer, SECOND);
          PhoneActions.act(
              caller.player(), PhoneActionPayload.hangUp(context.getAbsolutePos(FIRST)));
          expectStatus(context, answerer, LineStatus.HUNG_UP);
          context.expectBlockProperty(FIRST, TelephoneBlock.LIFTED, false);
          leaveAll(caller, answerer);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 100)
  public void aDarkGridEndsALiveCall(TestContext context) {
    RedstoneCellBlockEntity cell = buildNetwork(context);
    ConnectedTestPlayer caller = ConnectedTestPlayer.joinAt(context, at(2, 3));
    ConnectedTestPlayer answerer = ConnectedTestPlayer.joinAt(context, at(6, 3));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          pickUp(context, caller, FIRST);
          dial(context, caller, FIRST, numberOf(context, SECOND));
          pickUp(context, answerer, SECOND);
          cell.setCharge(0);
        });
    context.waitAndRun(
        SETTLE_TICKS * 3L,
        () -> {
          long first = abs(context, FIRST);
          context.assertTrue(
              Switchboard.of(context.getWorld()).callOf(first).isEmpty(),
              "a dark grid must end the call");
          leaveAll(caller, answerer);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 140)
  public void aSharedNumberRingsBothPhonesAndASecondPhoneJoins(TestContext context) {
    buildNetwork(context);
    ConnectedTestPlayer caller = ConnectedTestPlayer.joinAt(context, at(2, 3));
    ConnectedTestPlayer answerer = ConnectedTestPlayer.joinAt(context, at(6, 3));
    ConnectedTestPlayer joiner = ConnectedTestPlayer.joinAt(context, at(7, 3));
    context.waitAndRun(
        SETTLE_TICKS,
        () ->
            ExchangeActions.share(
                answerer.player(),
                new ExchangeSharePayload(
                    context.getAbsolutePos(EXCHANGE),
                    List.of(context.getAbsolutePos(SECOND), context.getAbsolutePos(THIRD)),
                    true)));
    context.waitAndRun(
        SETTLE_TICKS + 2L,
        () -> {
          context.assertTrue(
              numberOf(context, SECOND) == numberOf(context, THIRD),
              "both phones must share one number");
          pickUp(context, caller, FIRST);
          dial(context, caller, FIRST, numberOf(context, SECOND));
        });
    context.waitAndRun(
        SETTLE_TICKS + 4L,
        () -> {
          context.expectBlockProperty(SECOND, TelephoneBlock.RINGING, true);
          context.expectBlockProperty(THIRD, TelephoneBlock.RINGING, true);
          pickUp(context, answerer, SECOND);
          pickUp(context, joiner, THIRD);
          expectStatus(context, joiner, LineStatus.CONNECTED);
          context.assertTrue(joiner.hasEarned(PARTY_LINE), "joining must earn Party Line");
          context.assertTrue(
              Switchboard.of(context.getWorld())
                      .callOf(abs(context, FIRST))
                      .orElseThrow()
                      .members()
                      .size()
                  == 3,
              "three phones must be on the call");
          leaveAll(caller, answerer, joiner);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 100)
  public void aRingingPhoneThatIsTakenAwayEndsTheCall(TestContext context) {
    buildNetwork(context);
    ConnectedTestPlayer caller = ConnectedTestPlayer.joinAt(context, at(2, 3));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          pickUp(context, caller, FIRST);
          dial(context, caller, FIRST, numberOf(context, SECOND));
          context.setBlockState(SECOND, Blocks.AIR);
        });
    context.waitAndRun(
        SETTLE_TICKS + 3L,
        () -> {
          context.assertTrue(
              Switchboard.of(context.getWorld()).callOf(abs(context, FIRST)).isEmpty(),
              "a call with nothing left to ring must end");
          expectStatus(context, caller, LineStatus.LINE_DEAD);
          leaveAll(caller);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 140)
  public void aSealedLineKeepsItsNumbersAndRefusesASecondPhone(TestContext context) {
    buildNetwork(context);
    ConnectedTestPlayer caller = ConnectedTestPlayer.joinAt(context, at(2, 3));
    ConnectedTestPlayer answerer = ConnectedTestPlayer.joinAt(context, at(6, 3));
    ConnectedTestPlayer joiner = ConnectedTestPlayer.joinAt(context, at(7, 3));
    context.waitAndRun(
        SETTLE_TICKS,
        () ->
            ExchangeActions.share(
                answerer.player(),
                new ExchangeSharePayload(
                    context.getAbsolutePos(EXCHANGE),
                    List.of(context.getAbsolutePos(SECOND), context.getAbsolutePos(THIRD)),
                    true)));
    context.waitAndRun(
        SETTLE_TICKS + 2L,
        () -> {
          PhoneDirectory directory = PhoneDirectory.of(context.getWorld());
          WireEngine.of(context.getWorld())
              .reown(directory.systemOf(abs(context, FIRST)).orElseThrow(), new UUID(9, 9));
          directory.refresh();
          context.assertTrue(
              numberOf(context, SECOND) != PhoneNumbering.NONE
                  && numberOf(context, SECOND) == numberOf(context, THIRD),
              "sealing must keep the numbers phones already had");
          pickUp(context, caller, FIRST);
          dial(context, caller, FIRST, numberOf(context, SECOND));
        });
    context.waitAndRun(
        SETTLE_TICKS + 4L,
        () -> {
          pickUp(context, answerer, SECOND);
          pickUp(context, joiner, THIRD);
          expectStatus(context, joiner, LineStatus.BUSY);
          context.assertTrue(
              Switchboard.of(context.getWorld())
                      .callOf(abs(context, FIRST))
                      .orElseThrow()
                      .members()
                      .size()
                  == 2,
              "a sealed line must not take a second phone");
          leaveAll(caller, answerer, joiner);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 100)
  public void aVoiceReachesOnlyTheOtherPlayersOnTheCall(TestContext context) {
    buildNetwork(context);
    ConnectedTestPlayer caller = ConnectedTestPlayer.joinAt(context, at(2, 3));
    ConnectedTestPlayer answerer = ConnectedTestPlayer.joinAt(context, at(6, 3));
    ConnectedTestPlayer bystander = ConnectedTestPlayer.joinAt(context, at(7, 4));
    byte[] frame = new byte[AdpcmCodec.frameBytes(VoiceFormat.FRAME_SAMPLES)];
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          pickUp(context, caller, FIRST);
          dial(context, caller, FIRST, numberOf(context, SECOND));
          pickUp(context, answerer, SECOND);
          List<UUID> heard =
              CallVoice.audience(
                  caller.player(), new CallVoicePayload(context.getAbsolutePos(FIRST), frame));
          context.assertTrue(
              heard.equals(List.of(answerer.id())), "only the far side must hear, got " + heard);
          context.assertTrue(
              CallVoice.audience(
                      bystander.player(),
                      new CallVoicePayload(context.getAbsolutePos(THIRD), frame))
                  .isEmpty(),
              "a bystander must not be heard");
          context.assertTrue(
              CallVoice.audience(
                      answerer.player(), new CallVoicePayload(context.getAbsolutePos(FIRST), frame))
                  .isEmpty(),
              "nobody may speak through another player's phone");
          leaveAll(caller, answerer, bystander);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 100, batchId = VOICE_OFF_BATCH)
  public void turningVoiceCallsOffDropsEveryFrame(TestContext context) {
    buildNetwork(context);
    ConnectedTestPlayer caller = ConnectedTestPlayer.joinAt(context, at(2, 3));
    ConnectedTestPlayer answerer = ConnectedTestPlayer.joinAt(context, at(6, 3));
    byte[] frame = new byte[AdpcmCodec.frameBytes(VoiceFormat.FRAME_SAMPLES)];
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          pickUp(context, caller, FIRST);
          dial(context, caller, FIRST, numberOf(context, SECOND));
          pickUp(context, answerer, SECOND);
          context.assertTrue(
              CallVoice.audience(
                      caller.player(), new CallVoicePayload(context.getAbsolutePos(FIRST), frame))
                  .isEmpty(),
              "voice off must drop the frame");
          context.assertTrue(
              Switchboard.of(context.getWorld()).callOf(abs(context, FIRST)).isPresent(),
              "voice off must leave the call up");
          leaveAll(caller, answerer);
          context.complete();
        });
  }

  @BeforeBatch(batchId = VOICE_OFF_BATCH)
  public void turnVoiceCallsOff(ServerWorld world) {
    savedConfig = Hardwired.SERVER_CONFIG;
    Hardwired.SERVER_CONFIG = savedConfig.withPhone(savedConfig.phone().withVoiceCalls(false));
  }

  @AfterBatch(batchId = VOICE_OFF_BATCH)
  public void restoreVoiceCalls(ServerWorld world) {
    Hardwired.SERVER_CONFIG = savedConfig;
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void twoPlayersDiallingOneNumberInOneTickGiveOneCall(TestContext context) {
    buildNetwork(context);
    ConnectedTestPlayer first = ConnectedTestPlayer.joinAt(context, at(2, 3));
    ConnectedTestPlayer second = ConnectedTestPlayer.joinAt(context, at(7, 3));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          pickUp(context, first, FIRST);
          pickUp(context, second, THIRD);
          int target = numberOf(context, SECOND);
          dial(context, first, FIRST, target);
          dial(context, second, THIRD, target);
          expectStatus(context, first, LineStatus.RINGING_OUT);
          expectStatus(context, second, LineStatus.BUSY);
          leaveAll(first, second);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 60)
  public void aTelephoneLoadedWithAStaleRingSettles(TestContext context) {
    buildNetwork(context);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.setBlockState(
              SECOND,
              context
                  .getBlockState(SECOND)
                  .with(TelephoneBlock.RINGING, true)
                  .with(TelephoneBlock.LIFTED, true));
          Switchboard.of(context.getWorld())
              .chunkLoaded(new ChunkPos(context.getAbsolutePos(SECOND)));
        });
    context.waitAndRun(
        SETTLE_TICKS + 2L,
        () -> {
          context.expectBlockProperty(SECOND, TelephoneBlock.RINGING, false);
          context.expectBlockProperty(SECOND, TelephoneBlock.LIFTED, false);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 60)
  public void anExchangeWithNoTelephoneSaysSo(TestContext context) {
    context.setBlockState(CELL, ModBlocks.REDSTONE_CELL);
    WireEngine engine = WireEngine.of(context.getWorld());
    for (int x = 1; x <= 5; x++) {
      engine.lay(
          new WireCell(
              new WireFace(context.getAbsolutePos(new BlockPos(x, FLOOR_Y, 1)), Direction.UP),
              Hue.PLAIN));
    }
    RedstoneCellBlockEntity cell = context.getBlockEntity(CELL);
    cell.setCharge(500);
    placeExchange(context);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          DeviceStatus status =
              PhoneDirectory.readiness(context.getWorld(), context.getAbsolutePos(EXCHANGE));
          context.assertTrue(
              status == DeviceStatus.NO_TELEPHONE, "a lone Exchange must say so, was " + status);
          context.complete();
        });
  }
}
