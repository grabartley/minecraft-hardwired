package com.grahambartley.hardwired.gametest;

import static com.grahambartley.hardwired.gametest.RadioStage.*;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.BroadcastDeskBlock;
import com.grahambartley.hardwired.block.RadioBlock;
import com.grahambartley.hardwired.block.RadioBlockEntity;
import com.grahambartley.hardwired.block.RedstoneCellBlockEntity;
import com.grahambartley.hardwired.block.SpeakerBlock;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.config.RadioSettings;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.radio.DeskAction;
import com.grahambartley.hardwired.radio.Frequency;
import com.grahambartley.hardwired.radio.ReceptionMix;
import com.grahambartley.hardwired.radio.Signal;
import com.grahambartley.hardwired.radio.SongClock;
import com.grahambartley.hardwired.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.AfterBatch;
import net.minecraft.test.BeforeBatch;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class RadioGameTest implements FabricGameTest {

  private static final int MEASURE_FROM = SETTLE + 2 * BEAT;
  private static final int DRAIN_WINDOW = 3 * ElectronicsDriver.BEAT_TICKS;
  private static final int TOGGLE_TICKS = 4;

  private static HardwiredConfig savedConfig;

  @BeforeBatch(batchId = BATCH)
  public void lowerTheTowerRule(ServerWorld world) {
    savedConfig = Hardwired.SERVER_CONFIG;
    Hardwired.SERVER_CONFIG =
        savedConfig.withRadio(RadioSettings.DEFAULTS.withTowerMinMasts(MASTS));
  }

  @AfterBatch(batchId = BATCH)
  public void restoreTheTowerRule(ServerWorld world) {
    Hardwired.SERVER_CONFIG = savedConfig;
  }

  @BeforeBatch(batchId = SHORT_REACH_BATCH)
  public void shortenTheReach(ServerWorld world) {
    savedConfig = Hardwired.SERVER_CONFIG;
    Hardwired.SERVER_CONFIG =
        savedConfig.withRadio(
            RadioSettings.DEFAULTS
                .withTowerMinMasts(MASTS)
                .withTowerBaseReach(0)
                .withTowerReachPerMast(1));
  }

  @AfterBatch(batchId = SHORT_REACH_BATCH)
  public void restoreTheReach(ServerWorld world) {
    Hardwired.SERVER_CONFIG = savedConfig;
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 100)
  public void aSongOnTheAirReachesARadioAndItsSpeaker(TestContext context) {
    RedstoneCellBlockEntity cell = buildStation(context, 500);
    ConnectedTestPlayer player = playerAt(context, 3);
    context.waitAndRun(
        SETTLE,
        () -> {
          int before = cell.charge();
          goOnAirWithSong(context, player);
          context.expectBlockProperty(PANEL, BroadcastDeskBlock.ON_AIR, true);
          context.expectBlockProperty(DESK, BroadcastDeskBlock.DISC, true);
          context.assertTrue(desk(context).songPlaying(), "Play must start the song");
          context.assertTrue(
              cell.charge() < before, "going on the air pays in the second it starts");
        });
    context.waitAndRun(
        SETTLE + 2L * BEAT,
        () -> {
          expectStatus(context, RADIO, DeviceStatus.RUNNING);
          context.expectBlockProperty(RADIO, RadioBlock.PLAYING, true);
          context.expectBlockProperty(SPEAKER, SpeakerBlock.PLAYING, true);
          Identifier song = Identifier.ofVanilla("13");
          context.assertTrue(song.equals(radio(context).song()), "the Radio plays the song");
          context.assertTrue(song.equals(speaker(context).song()), "the Speaker repeats it");
          long start = desk(context).songStart();
          context.assertTrue(radio(context).songStart() == start, "the Radio shares the moment");
          context.assertTrue(speaker(context).songStart() == start, "the Speaker shares it too");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 140)
  public void stoppingAndGoingOffTheAirSilencesEverything(TestContext context) {
    buildStation(context, 500);
    ConnectedTestPlayer player = playerAt(context, 3);
    context.waitAndRun(SETTLE, () -> goOnAirWithSong(context, player));
    context.waitAndRun(
        SETTLE + 2L * BEAT,
        () -> {
          act(context, player, PANEL, DeskAction.STOP);
          context.assertFalse(desk(context).songPlaying(), "Stop must stop the song");
        });
    context.waitAndRun(
        SETTLE + 3L * BEAT,
        () -> {
          context.assertTrue(radio(context).song() == null, "a stopped song leaves silence");
          expectStatus(context, RADIO, DeviceStatus.RUNNING);
          act(context, player, PANEL, DeskAction.TOGGLE_AIR);
          context.expectBlockProperty(PANEL, BroadcastDeskBlock.ON_AIR, false);
          context.assertFalse(onTheAir(context, PANEL), "off the air means no station");
        });
    context.waitAndRun(
        SETTLE + 4L * BEAT,
        () -> {
          expectStatus(context, RADIO, DeviceStatus.NO_STATION);
          expectStatus(context, SPEAKER, DeviceStatus.NO_RADIO);
          player.leave();
          context.complete();
        });
  }

  private static void expectTogglingCostsNoMoreThanRunning(
      TestContext context, RedstoneCellBlockEntity cell, int spareCharge, Runnable toggle) {
    int[] charge = new int[2];
    context.runAtTick(MEASURE_FROM, () -> charge[0] = cell.charge());
    context.runAtTick(MEASURE_FROM + DRAIN_WINDOW, () -> charge[1] = cell.charge());
    context.runAtEveryTick(
        () -> {
          long tick = context.getTick();
          if (tick > MEASURE_FROM + DRAIN_WINDOW
              && tick < MEASURE_FROM + 2L * DRAIN_WINDOW
              && tick % TOGGLE_TICKS == 0) {
            toggle.run();
          }
        });
    context.runAtTick(
        MEASURE_FROM + 2L * DRAIN_WINDOW,
        () -> {
          int running = charge[0] - charge[1];
          int toggling = charge[1] - cell.charge();
          context.assertTrue(running > 0, "the control must drain, it used " + running);
          context.assertTrue(
              toggling <= running + spareCharge,
              "toggling used " + toggling + " where running used " + running);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 200)
  public void aRadioToggledFlatOutDrainsNoFasterThanOneLeftRunning(TestContext context) {
    RedstoneCellBlockEntity cell = buildStation(context, 5000);
    ConnectedTestPlayer player = playerAt(context, 3);
    context.waitAndRun(SETTLE, () -> act(context, player, PANEL, DeskAction.TOGGLE_AIR));
    int oneBeat =
        Hardwired.SERVER_CONFIG.power().radioChargePerSecond()
            + Hardwired.SERVER_CONFIG.power().speakerChargePerSecond();
    expectTogglingCostsNoMoreThanRunning(
        context,
        cell,
        oneBeat,
        () ->
            setRadio(
                context,
                player,
                !radio(context).isOn(),
                radio(context).frequency(),
                ReceptionMix.MAX_VOLUME));
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 200)
  public void aDeskToggledFlatOutDrainsNoFasterThanOneLeftOnTheAir(TestContext context) {
    RedstoneCellBlockEntity cell = wiredCell(context, 5000);
    placeDesk(context, PANEL);
    raiseTower(context, MASTS, true);
    desk(context).setFrequency(nextFrequency());
    ConnectedTestPlayer player = playerAt(context, 3);
    context.waitAndRun(SETTLE, () -> act(context, player, PANEL, DeskAction.TOGGLE_AIR));
    expectTogglingCostsNoMoreThanRunning(
        context,
        cell,
        Hardwired.SERVER_CONFIG.power().broadcastChargePerSecond(),
        () -> act(context, player, PANEL, DeskAction.TOGGLE_AIR));
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 60)
  public void theLastOfABurstOfRadioSetsIsTheOneThatHolds(TestContext context) {
    buildStation(context, 500);
    ConnectedTestPlayer player = playerAt(context, 3);
    context.waitAndRun(
        SETTLE,
        () -> {
          int frequency = radio(context).frequency();
          setRadio(context, player, false, frequency, ReceptionMix.MAX_VOLUME);
          expectStatus(context, RADIO, DeviceStatus.RADIO_OFF);
          setRadio(context, player, true, frequency, ReceptionMix.MAX_VOLUME);
          expectStatus(context, RADIO, DeviceStatus.RADIO_OFF);
          setRadio(context, player, false, frequency, ReceptionMix.MAX_VOLUME);
          setRadio(context, player, true, Frequency.MAX, 40);
        });
    context.waitAndRun(
        SETTLE + BEAT,
        () -> {
          expectStatus(context, RADIO, DeviceStatus.NO_STATION);
          context.assertTrue(radio(context).frequency() == Frequency.MAX, "the dial must land");
          context.assertTrue(radio(context).volume() == 40, "the volume must land");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 100)
  public void aRadioWithNoChargeIsNotToldWhatItCouldHear(TestContext context) {
    buildStation(context, 500);
    ConnectedTestPlayer player = playerAt(context, 3);
    BlockPos unwired = at(6, 3);
    context.setBlockState(unwired, ModBlocks.RADIO);
    RadioBlockEntity silent = context.getBlockEntity(unwired);
    silent.set(true, radio(context).frequency(), ReceptionMix.MAX_VOLUME);
    context.waitAndRun(SETTLE, () -> act(context, player, PANEL, DeskAction.TOGGLE_AIR));
    context.waitAndRun(
        SETTLE + 2L * BEAT,
        () -> {
          expectStatus(context, unwired, DeviceStatus.NO_CELL);
          DeviceStatus demand =
              ((RadioBlock) ModBlocks.RADIO)
                  .demand(context.getWorld(), silent.getPos(), context.getBlockState(unwired));
          context.assertTrue(demand == DeviceStatus.RUNNING, "the station must be in reach");
          context.assertTrue(silent.station() == null, "working out demand must not tune it");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 40)
  public void aDeskWithNoMastsSaysSo(TestContext context) {
    wiredCell(context, 50);
    placeDesk(context, PANEL);
    expectAirRefused(context, DeviceStatus.NO_TOWER);
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 40)
  public void aColumnTooShortIsNotATower(TestContext context) {
    wiredCell(context, 50);
    placeDesk(context, PANEL);
    raiseTower(context, MASTS - 1, true);
    expectAirRefused(context, DeviceStatus.TOWER_TOO_SHORT);
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 40)
  public void aColumnWithNoRodIsNotATower(TestContext context) {
    wiredCell(context, 50);
    placeDesk(context, PANEL);
    raiseTower(context, MASTS, false);
    expectAirRefused(context, DeviceStatus.TOWER_NO_ROD);
  }

  private static void expectAirRefused(TestContext context, DeviceStatus reason) {
    ConnectedTestPlayer player = playerAt(context, 3);
    context.waitAndRun(
        SETTLE,
        () -> {
          act(context, player, PANEL, DeskAction.TOGGLE_AIR);
          expectStatus(context, PANEL, reason);
          context.expectBlockProperty(PANEL, BroadcastDeskBlock.ON_AIR, false);
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 100)
  public void aTowerCarriesOneStationAndTheNextDeskTakesOver(TestContext context) {
    wiredCell(context, 500);
    placeDesk(context, PANEL);
    placeDesk(context, SECOND_PANEL);
    raiseTower(context, MASTS, true);
    ConnectedTestPlayer first = playerAt(context, 5);
    ConnectedTestPlayer second = playerAt(context, 6);
    context.waitAndRun(
        SETTLE,
        () -> {
          act(context, first, PANEL, DeskAction.TOGGLE_AIR);
          act(context, second, SECOND_PANEL, DeskAction.TOGGLE_AIR);
          expectStatus(context, PANEL, DeviceStatus.RUNNING);
          expectStatus(context, SECOND_PANEL, DeviceStatus.TOWER_IN_USE);
          context.assertFalse(
              onTheAir(context, SECOND_PANEL), "two Desks on one tower give one station");
        });
    context.waitAndRun(SETTLE + 5L, () -> act(context, first, PANEL, DeskAction.TOGGLE_AIR));
    context.waitAndRun(
        SETTLE + BEAT,
        () -> {
          expectStatus(context, SECOND_PANEL, DeviceStatus.RUNNING);
          first.leave();
          second.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 200)
  public void aStationOutOfChargeComesBackWithTheSongFromTheTop(TestContext context) {
    RedstoneCellBlockEntity cell = buildStation(context, 500);
    ConnectedTestPlayer player = playerAt(context, 3);
    long[] firstStart = new long[1];
    context.waitAndRun(
        SETTLE,
        () -> {
          goOnAirWithSong(context, player);
          firstStart[0] = desk(context).songStart();
          cell.setCharge(0);
        });
    context.waitAndRun(
        SETTLE + BEAT,
        () -> {
          expectStatus(context, PANEL, DeviceStatus.NO_CHARGE);
          context.expectBlockProperty(PANEL, BroadcastDeskBlock.ON_AIR, false);
          context.assertFalse(onTheAir(context, PANEL), "out of charge means off the air");
          cell.fill(200);
        });
    context.waitAndRun(
        SETTLE + 3L * BEAT,
        () -> {
          expectStatus(context, PANEL, DeviceStatus.RUNNING);
          context.assertTrue(desk(context).songPlaying(), "the song must come back");
          context.assertTrue(
              desk(context).songStart() > firstStart[0], "the song must start from the top");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 80)
  public void aSongEndingLeavesTheStationOnTheAirInSilence(TestContext context) {
    buildStation(context, 500);
    ConnectedTestPlayer player = playerAt(context, 3);
    context.waitAndRun(
        SETTLE,
        () -> {
          goOnAirWithSong(context, player);
          long length = desk(context).song().orElseThrow().value().getLengthInTicks();
          NbtCompound nbt = desk(context).createNbt(context.getWorld().getRegistryManager());
          nbt.putLong("song_start", context.getWorld().getTime() - length);
          desk(context).read(nbt, context.getWorld().getRegistryManager());
        });
    context.waitAndRun(
        SETTLE + BEAT,
        () -> {
          expectStatus(context, PANEL, DeviceStatus.RUNNING);
          context.assertFalse(desk(context).songPlaying(), "the song must be over");
          context.assertTrue(
              desk(context).songStart() == SongClock.STOPPED, "nothing is left playing");
          context.assertTrue(desk(context).hasDisc(), "the disc stays on the turntable");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 80)
  public void oneStepOffHalvesTheSignalAndTwoStepsLoseIt(TestContext context) {
    buildStation(context, 500);
    ConnectedTestPlayer player = playerAt(context, 3);
    double[] tuned = new double[1];
    context.waitAndRun(SETTLE, () -> goOnAirWithSong(context, player));
    context.waitAndRun(
        SETTLE + BEAT,
        () -> {
          tuned[0] = radio(context).strength();
          context.assertTrue(tuned[0] > 0, "tuned in must have signal");
          int frequency = radio(context).frequency();
          setRadio(context, player, true, frequency + Frequency.STEP, 50);
          context.assertTrue(
              Math.abs(radio(context).strength() - tuned[0] / 2) < 1e-3,
              "one step off must halve the signal");
          context.assertTrue(radio(context).volume() == 50, "the volume must be kept");
          setRadio(context, player, true, frequency + 2 * Frequency.STEP, 50);
          context.waitAndRun(
              BEAT,
              () -> {
                expectStatus(context, RADIO, DeviceStatus.NO_STATION);
                player.leave();
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 80)
  public void aRadioThatIsOffOrFindsNothingUsesNoCharge(TestContext context) {
    RedstoneCellBlockEntity cell = wiredCell(context, 50);
    context.setBlockState(RADIO, ModBlocks.RADIO);
    radio(context).set(true, Frequency.MAX, ReceptionMix.MAX_VOLUME);
    context.waitAndRun(
        SETTLE + 2L * BEAT,
        () -> {
          expectStatus(context, RADIO, DeviceStatus.NO_STATION);
          context.assertTrue(cell.charge() == 50, "nothing heard must cost nothing");
          ConnectedTestPlayer player = playerAt(context, 3);
          setRadio(context, player, false, Frequency.DEFAULT, 100);
          expectStatus(context, RADIO, DeviceStatus.RADIO_OFF);
          context.assertTrue(cell.charge() == 50, "off must cost nothing");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = SHORT_REACH_BATCH, tickLimit = 60)
  public void aRadioPastTheReachOfTheTowerFindsNothing(TestContext context) {
    wiredCell(context, 200);
    placeDesk(context, PANEL);
    raiseTower(context, MASTS, true);
    BlockPos near = at(5, 1);
    BlockPos far = at(2, 1);
    context.setBlockState(near, ModBlocks.RADIO);
    context.setBlockState(far, ModBlocks.RADIO);
    ConnectedTestPlayer player = playerAt(context, 3);
    context.waitAndRun(SETTLE, () -> act(context, player, PANEL, DeskAction.TOGGLE_AIR));
    context.waitAndRun(
        SETTLE + BEAT,
        () -> {
          expectStatus(context, near, DeviceStatus.RUNNING);
          expectStatus(context, far, DeviceStatus.NO_STATION);
          RadioBlockEntity heard = context.getBlockEntity(near);
          context.assertTrue(
              Math.abs(heard.strength() - Signal.byDistance(1, MASTS)) < 1e-3,
              "strength must fall with distance");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 80)
  public void tuningInAndBroadcastingEarnTheirAdvancements(TestContext context) {
    buildStation(context, 500);
    ConnectedTestPlayer player = playerAt(context, 3);
    context.waitAndRun(
        SETTLE,
        () -> {
          context.assertFalse(player.hasEarned(ON_THE_AIR), "nothing earned yet");
          goOnAirWithSong(context, player);
          context.assertTrue(player.hasEarned(ON_THE_AIR), "playing a disc earns On The Air");
          setRadio(context, player, true, radio(context).frequency(), 100);
          context.assertTrue(player.hasEarned(TUNED_IN), "tuning to a station earns Tuned In");
          player.leave();
          context.complete();
        });
  }
}
