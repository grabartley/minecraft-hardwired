package com.grahambartley.hardwired.gametest;

import static com.grahambartley.hardwired.gametest.RadioStage.*;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.RedstoneCellBlockEntity;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.config.RadioSettings;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.network.VoiceFramePayload;
import com.grahambartley.hardwired.radio.DeskAction;
import com.grahambartley.hardwired.radio.DeskMic;
import com.grahambartley.hardwired.radio.DeskVoice;
import com.grahambartley.hardwired.radio.Frequency;
import com.grahambartley.hardwired.radio.ReceptionMix;
import com.grahambartley.hardwired.radio.StationRegistry;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.wire.WireEngine;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.AfterBatch;
import net.minecraft.test.BeforeBatch;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class VoiceGameTest implements FabricGameTest {

  private static final String VOICE_OFF_BATCH = "radio-voice-off";
  private static final int ON_AIR_TICKS = SETTLE + BEAT;
  private static final Identifier LIVE_MIC = Identifier.of(Hardwired.MOD_ID, "live_mic");
  private static final byte[] FRAME = new byte[DeskMic.MOST_FRAME_BYTES];
  private static final byte[] TOO_LONG = new byte[DeskMic.MOST_FRAME_BYTES + 1];

  private static HardwiredConfig savedConfig;

  @BeforeBatch(batchId = VOICE_OFF_BATCH)
  public void turnVoiceOff(ServerWorld world) {
    savedConfig = Hardwired.SERVER_CONFIG;
    Hardwired.SERVER_CONFIG =
        savedConfig.withRadio(
            RadioSettings.DEFAULTS.withTowerMinMasts(MASTS).withVoiceBroadcasts(false));
  }

  @AfterBatch(batchId = VOICE_OFF_BATCH)
  public void restoreVoice(ServerWorld world) {
    Hardwired.SERVER_CONFIG = savedConfig;
  }

  private static ConnectedTestPlayer goOnAir(TestContext context) {
    buildStation(context, 2000);
    ConnectedTestPlayer player = playerAt(context, 3);
    act(context, player, PANEL, DeskAction.TOGGLE_AIR);
    return player;
  }

  private static void send(TestContext context, ConnectedTestPlayer player, byte[] frame) {
    DeskVoice.frame(player.player(), new VoiceFramePayload(context.getAbsolutePos(PANEL), frame));
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 80)
  public void twoPlayersPressingTalkGiveOneSpeaker(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer first = goOnAir(context);
    ConnectedTestPlayer second = playerAt(context, 4);
    context.waitAndRun(
        ON_AIR_TICKS,
        () -> {
          expectStatus(context, PANEL, DeviceStatus.RUNNING);
          act(context, first, PANEL, DeskAction.TAKE_MIC);
          act(context, second, PANEL, DeskAction.TAKE_MIC);
          context.assertTrue(
              first.id().equals(desk(context).mic().speaker()), "the first press must win");
          act(context, second, PANEL, DeskAction.RELEASE_MIC);
          context.assertTrue(
              first.id().equals(desk(context).mic().speaker()), "a stranger must not free the mic");
          act(context, first, PANEL, DeskAction.RELEASE_MIC);
          act(context, second, PANEL, DeskAction.TAKE_MIC);
          context.assertTrue(
              second.id().equals(desk(context).mic().speaker()), "a free mic can be taken");
          first.leave();
          second.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 80)
  public void aSealedDeskGivesItsMicOnlyToTheOwner(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer owner = goOnAir(context);
    ConnectedTestPlayer stranger = playerAt(context, 4);
    WireEngine engine = WireEngine.of(context.getWorld());
    engine.grid().networksTouching(context.getAbsolutePos(PANEL)).stream()
        .findFirst()
        .ifPresent(network -> engine.reown(network, owner.id()));
    context.waitAndRun(
        ON_AIR_TICKS,
        () -> {
          act(context, stranger, PANEL, DeskAction.TAKE_MIC);
          context.assertTrue(desk(context).mic().speaker() == null, "a stranger gets no mic");
          act(context, owner, PANEL, DeskAction.TAKE_MIC);
          context.assertTrue(
              owner.id().equals(desk(context).mic().speaker()), "the owner gets the mic");
          owner.leave();
          stranger.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 60)
  public void aDeskOffTheAirHasNoMic(TestContext context) {
    buildStation(context, 2000);
    ConnectedTestPlayer player = playerAt(context, 3);
    context.waitAndRun(
        SETTLE,
        () -> {
          act(context, player, PANEL, DeskAction.TAKE_MIC);
          context.assertTrue(desk(context).mic().speaker() == null, "off air there is no mic");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = VOICE_OFF_BATCH, tickLimit = 80)
  public void theServerSettingTurnsTheMicOff(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer player = goOnAir(context);
    context.waitAndRun(
        ON_AIR_TICKS,
        () -> {
          expectStatus(context, PANEL, DeviceStatus.RUNNING);
          act(context, player, PANEL, DeskAction.TAKE_MIC);
          context.assertTrue(desk(context).mic().speaker() == null, "voice must stay off");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 80)
  public void aFrameThatFailsAnyRuleEarnsNothing(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer speaker = goOnAir(context);
    ConnectedTestPlayer stranger = playerAt(context, 4);
    context.waitAndRun(
        ON_AIR_TICKS + 2L * BEAT,
        () -> {
          expectStatus(context, RADIO, DeviceStatus.RUNNING);
          send(context, speaker, FRAME);
          context.assertFalse(speaker.hasEarned(LIVE_MIC), "no mic taken, no frame");
          act(context, speaker, PANEL, DeskAction.TAKE_MIC);
          send(context, stranger, FRAME);
          context.assertFalse(stranger.hasEarned(LIVE_MIC), "only the speaker is relayed");
          send(context, speaker, TOO_LONG);
          context.assertFalse(speaker.hasEarned(LIVE_MIC), "an oversized frame is dropped");
          speaker.leave();
          stranger.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 100)
  public void speakingToAReceivingRadioEarnsLiveMic(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer player = goOnAir(context);
    context.waitAndRun(
        ON_AIR_TICKS + 2L * BEAT,
        () -> {
          expectStatus(context, RADIO, DeviceStatus.RUNNING);
          context.assertTrue(
              radio(context).station().equals(context.getAbsolutePos(PANEL)),
              "the Radio must know its station even with no song");
          act(context, player, PANEL, DeskAction.TAKE_MIC);
          send(context, player, FRAME);
          context.assertTrue(player.hasEarned(LIVE_MIC), "speaking to a Radio earns Live Mic");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 100)
  public void aSpeakerFollowsItsRadiosStationAndDial(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer player = goOnAir(context);
    context.waitAndRun(
        ON_AIR_TICKS + 2L * BEAT,
        () -> {
          expectStatus(context, SPEAKER, DeviceStatus.RUNNING);
          context.assertTrue(
              context.getAbsolutePos(PANEL).equals(speaker(context).station()),
              "the Speaker must know the station");
          context.assertTrue(
              speaker(context).dial() == radio(context).frequency(),
              "the Speaker must follow the dial");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 100)
  public void nobodyListeningEarnsNothing(TestContext context) {
    ConnectedTestPlayer player = goOnAir(context);
    radio(context).set(false, Frequency.DEFAULT, ReceptionMix.MAX_VOLUME);
    context.waitAndRun(
        ON_AIR_TICKS + 2L * BEAT,
        () -> {
          expectStatus(context, PANEL, DeviceStatus.RUNNING);
          act(context, player, PANEL, DeskAction.TAKE_MIC);
          send(context, player, FRAME);
          context.assertFalse(player.hasEarned(LIVE_MIC), "no Radio is receiving");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 100)
  public void aVoiceGoesOnlyToPlayersInHearing(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer near = goOnAir(context);
    ConnectedTestPlayer far = playerAt(context, 4);
    BlockPos panel = context.getAbsolutePos(PANEL);
    far.player().requestTeleport(panel.getX() + 300.0, panel.getY(), panel.getZ());
    context.waitAndRun(
        ON_AIR_TICKS + 2L * BEAT,
        () -> {
          expectStatus(context, SPEAKER, DeviceStatus.RUNNING);
          var heard = DeskVoice.listeners(context.getWorld(), panel, far.player());
          context.assertTrue(heard.contains(near.player()), "a player by the Radio hears it");
          context.assertFalse(heard.contains(far.player()), "a far player is sent nothing");
          near.leave();
          far.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 100)
  public void aSpeakerIsNotSentTheirOwnVoice(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer speaker = goOnAir(context);
    ConnectedTestPlayer listener = playerAt(context, 4);
    context.waitAndRun(
        ON_AIR_TICKS + 2L * BEAT,
        () -> {
          expectStatus(context, RADIO, DeviceStatus.RUNNING);
          var heard =
              DeskVoice.listeners(
                  context.getWorld(), context.getAbsolutePos(PANEL), speaker.player());
          context.assertFalse(heard.contains(speaker.player()), "the speaker hears no echo");
          context.assertTrue(heard.contains(listener.player()), "a player beside them still does");
          speaker.leave();
          listener.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 100)
  public void breakingARadioTakesItOutOfTheAudience(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer player = goOnAir(context);
    context.waitAndRun(
        ON_AIR_TICKS + 2L * BEAT,
        () -> {
          BlockPos panel = context.getAbsolutePos(PANEL);
          StationRegistry stations = StationRegistry.of(context.getWorld());
          context.assertTrue(
              stations.audience().tunedTo(panel).contains(radio(context)),
              "the Radio must be in the audience");
          var radio = radio(context);
          context.setBlockState(RADIO, net.minecraft.block.Blocks.AIR);
          context.assertFalse(
              stations.audience().tunedTo(panel).contains(radio), "a broken Radio must leave");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 120)
  public void goingOffTheAirFreesTheMic(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer player = goOnAir(context);
    context.waitAndRun(
        ON_AIR_TICKS,
        () -> {
          act(context, player, PANEL, DeskAction.TAKE_MIC);
          context.assertTrue(desk(context).mic().speaker() != null, "the mic must be taken");
          act(context, player, PANEL, DeskAction.TOGGLE_AIR);
          context.assertTrue(desk(context).mic().speaker() == null, "off air frees the mic");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 120)
  public void movingOutOfReachFreesTheMic(TestContext context) {
    ConnectedTestPlayer player = goOnAir(context);
    context.waitAndRun(
        ON_AIR_TICKS,
        () -> {
          act(context, player, PANEL, DeskAction.TAKE_MIC);
          BlockPos panel = context.getAbsolutePos(PANEL);
          player.player().requestTeleport(panel.getX() + 40.0, panel.getY(), panel.getZ());
          context.waitAndRun(
              BEAT + 2,
              () -> {
                context.assertTrue(
                    desk(context).mic().speaker() == null, "out of reach frees the mic");
                player.leave();
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 120)
  public void leavingTheGameFreesTheMic(TestContext context) {
    ConnectedTestPlayer player = goOnAir(context);
    context.waitAndRun(
        ON_AIR_TICKS,
        () -> {
          act(context, player, PANEL, DeskAction.TAKE_MIC);
          player.leave();
          context.waitAndRun(
              BEAT + 2,
              () -> {
                context.assertTrue(desk(context).mic().speaker() == null, "a leaver frees it");
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 60)
  public void anEmptyDialHissesOnlyWithChargeOnTheWire(TestContext context) {
    RedstoneCellBlockEntity cell = wiredCell(context, 50);
    context.setBlockState(RADIO, ModBlocks.RADIO);
    radio(context).set(true, Frequency.MAX, ReceptionMix.MAX_VOLUME);
    context.waitAndRun(
        SETTLE + BEAT,
        () -> {
          expectStatus(context, RADIO, DeviceStatus.NO_STATION);
          context.assertTrue(radio(context).hisses(), "a charged Radio hisses");
          cell.setCharge(0);
          context.waitAndRun(
              BEAT + 2,
              () -> {
                context.assertFalse(radio(context).hisses(), "an empty cell gives no hiss");
                context.complete();
              });
        });
  }
}
