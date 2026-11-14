package com.grahambartley.hardwired.gametest;

import static com.grahambartley.hardwired.gametest.PhoneStage.BENCH;
import static com.grahambartley.hardwired.gametest.PhoneStage.CELL;
import static com.grahambartley.hardwired.gametest.PhoneStage.EXCHANGE;
import static com.grahambartley.hardwired.gametest.PhoneStage.FIRST;
import static com.grahambartley.hardwired.gametest.PhoneStage.FLOOR_Y;
import static com.grahambartley.hardwired.gametest.PhoneStage.SECOND;
import static com.grahambartley.hardwired.gametest.PhoneStage.SETTLE_TICKS;
import static com.grahambartley.hardwired.gametest.PhoneStage.THIRD;
import static com.grahambartley.hardwired.gametest.PhoneStage.abs;
import static com.grahambartley.hardwired.gametest.PhoneStage.at;
import static com.grahambartley.hardwired.gametest.PhoneStage.buildNetwork;
import static com.grahambartley.hardwired.gametest.PhoneStage.dial;
import static com.grahambartley.hardwired.gametest.PhoneStage.expectStatus;
import static com.grahambartley.hardwired.gametest.PhoneStage.leaveAll;
import static com.grahambartley.hardwired.gametest.PhoneStage.numberOf;
import static com.grahambartley.hardwired.gametest.PhoneStage.pickUp;
import static com.grahambartley.hardwired.gametest.PhoneStage.placeExchange;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.BellBoxBlock;
import com.grahambartley.hardwired.block.RedstoneCellBlockEntity;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.PhoneDevice;
import com.grahambartley.hardwired.network.CallVoicePayload;
import com.grahambartley.hardwired.network.ExchangeSharePayload;
import com.grahambartley.hardwired.network.PhoneActionPayload;
import com.grahambartley.hardwired.phone.CallVoice;
import com.grahambartley.hardwired.phone.ExchangeActions;
import com.grahambartley.hardwired.phone.LineStatus;
import com.grahambartley.hardwired.phone.PhoneActions;
import com.grahambartley.hardwired.phone.PhoneDirectory;
import com.grahambartley.hardwired.phone.PhoneNumbering;
import com.grahambartley.hardwired.phone.Switchboard;
import com.grahambartley.hardwired.radio.AdpcmCodec;
import com.grahambartley.hardwired.radio.VoiceFormat;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import com.grahambartley.hardwired.wire.WireNetwork;
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

  private static final String VOICE_OFF_BATCH = "telephone-voice-off";
  private static final BlockPos BELL = at(3, 1);
  private static final Identifier NUMBER_PLEASE = Identifier.of(Hardwired.MOD_ID, "number_please");
  private static final Identifier PARTY_LINE = Identifier.of(Hardwired.MOD_ID, "party_line");

  private static HardwiredConfig savedConfig;

  private static void placeBellBox(TestContext context, boolean ringing) {
    context.setBlockState(BELL.south(), Blocks.STONE);
    context.setBlockState(
        BELL,
        ModBlocks.BELL_BOX
            .getDefaultState()
            .with(BellBoxBlock.FACING, Direction.NORTH)
            .with(BellBoxBlock.POWERED, ringing));
  }

  private static void expectBell(TestContext context, boolean ringing) {
    context.expectBlockProperty(BELL, BellBoxBlock.POWERED, ringing);
    context.assertTrue(
        WireEngine.of(context.getWorld()).isOn(context.getAbsolutePos(BELL)) == ringing,
        "the bell's network must be " + (ringing ? "on" : "off"));
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
          context.expectBlockProperty(FIRST, PhoneDevice.LIFTED, true);
          dial(context, caller, FIRST, numberOf(context, SECOND));
          expectStatus(context, caller, LineStatus.RINGING_OUT);
        });
    context.waitAndRun(
        SETTLE_TICKS + 2L,
        () -> {
          context.expectBlockProperty(SECOND, PhoneDevice.RINGING, true);
          pickUp(context, answerer, SECOND);
          expectStatus(context, caller, LineStatus.CONNECTED);
          expectStatus(context, answerer, LineStatus.CONNECTED);
          context.expectBlockProperty(SECOND, PhoneDevice.RINGING, false);
          context.expectBlockProperty(SECOND, PhoneDevice.LIFTED, true);
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
          context.expectBlockProperty(FIRST, PhoneDevice.LIFTED, false);
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
          context.expectBlockProperty(SECOND, PhoneDevice.RINGING, true);
          context.expectBlockProperty(THIRD, PhoneDevice.RINGING, true);
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

  @GameTest(templateName = BENCH, tickLimit = 100)
  public void breakingALiftedPhoneTellsItsUserTheLineWentDead(TestContext context) {
    buildNetwork(context);
    ConnectedTestPlayer user = ConnectedTestPlayer.joinAt(context, at(2, 3));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          pickUp(context, user, FIRST);
          context.setBlockState(FIRST, Blocks.AIR);
          expectStatus(context, user, LineStatus.LINE_DEAD);
          context.assertFalse(
              Switchboard.of(context.getWorld()).isLifted(abs(context, FIRST)),
              "a broken phone must not stay lifted");
          leaveAll(user);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 100)
  public void breakingAPhoneOnACallTellsBothSidesTheLineWentDead(TestContext context) {
    buildNetwork(context);
    ConnectedTestPlayer caller = ConnectedTestPlayer.joinAt(context, at(2, 3));
    ConnectedTestPlayer answerer = ConnectedTestPlayer.joinAt(context, at(6, 3));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          pickUp(context, caller, FIRST);
          dial(context, caller, FIRST, numberOf(context, SECOND));
          pickUp(context, answerer, SECOND);
          expectStatus(context, caller, LineStatus.CONNECTED);
          context.setBlockState(SECOND, Blocks.AIR);
          expectStatus(context, answerer, LineStatus.LINE_DEAD);
          expectStatus(context, caller, LineStatus.LINE_DEAD);
          context.assertTrue(
              Switchboard.of(context.getWorld()).callOf(abs(context, FIRST)).isEmpty(),
              "breaking one side must end a two-way call");
          leaveAll(caller, answerer);
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
                  .with(PhoneDevice.RINGING, true)
                  .with(PhoneDevice.LIFTED, true));
          Switchboard.of(context.getWorld())
              .chunkLoaded(new ChunkPos(context.getAbsolutePos(SECOND)));
        });
    context.waitAndRun(
        SETTLE_TICKS + 2L,
        () -> {
          context.expectBlockProperty(SECOND, PhoneDevice.RINGING, false);
          context.expectBlockProperty(SECOND, PhoneDevice.LIFTED, false);
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

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void aRingingPhoneRingsABellBoxThatTurnsItsNetworkOn(TestContext context) {
    buildNetwork(context);
    placeBellBox(context, false);
    ConnectedTestPlayer caller = ConnectedTestPlayer.joinAt(context, at(2, 3));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          expectBell(context, false);
          pickUp(context, caller, FIRST);
          dial(context, caller, FIRST, numberOf(context, SECOND));
        });
    context.waitAndRun(SETTLE_TICKS + 3L, () -> expectBell(context, true));
    context.waitAndRun(
        SETTLE_TICKS + 5L, () -> Switchboard.of(context.getWorld()).hangUp(abs(context, FIRST)));
    context.waitAndRun(
        SETTLE_TICKS + 8L,
        () -> {
          expectBell(context, false);
          leaveAll(caller);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 60)
  public void aBellBoxLeftRingingWithNoCallFallsSilent(TestContext context) {
    buildNetwork(context);
    placeBellBox(context, true);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          expectBell(context, false);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void aBellBoxWithNoPowerStaysSilentWhileItsPhoneRings(TestContext context) {
    buildNetwork(context);
    WireEngine engine = WireEngine.of(context.getWorld());
    WireNetwork unpowered = null;
    for (int z = 2; z <= 3; z++) {
      unpowered =
          engine.lay(
              new WireCell(
                  new WireFace(context.getAbsolutePos(new BlockPos(6, FLOOR_Y, z)), Direction.UP),
                  Hue.RED));
    }
    engine.reown(unpowered, UUID.randomUUID());
    BlockPos bell = at(6, 3);
    context.setBlockState(bell.south(), Blocks.STONE);
    context.setBlockState(
        bell, ModBlocks.BELL_BOX.getDefaultState().with(BellBoxBlock.FACING, Direction.NORTH));
    ConnectedTestPlayer caller = ConnectedTestPlayer.joinAt(context, at(2, 3));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          pickUp(context, caller, FIRST);
          dial(context, caller, FIRST, numberOf(context, SECOND));
        });
    context.waitAndRun(
        SETTLE_TICKS + 3L,
        () -> {
          context.expectBlockProperty(SECOND, PhoneDevice.RINGING, true);
          context.expectBlockProperty(bell, BellBoxBlock.POWERED, false);
          leaveAll(caller);
          context.complete();
        });
  }
}
