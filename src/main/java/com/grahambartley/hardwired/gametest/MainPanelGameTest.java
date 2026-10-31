package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.AbstractLampBlock;
import com.grahambartley.hardwired.block.ControllerBlockEntity;
import com.grahambartley.hardwired.block.LampBlockEntity;
import com.grahambartley.hardwired.block.MainPanelBlockEntity;
import com.grahambartley.hardwired.block.MountedDeviceBlock;
import com.grahambartley.hardwired.controller.ControllerProgram;
import com.grahambartley.hardwired.controller.ControllerWiring;
import com.grahambartley.hardwired.controller.Timeline;
import com.grahambartley.hardwired.controller.TimelineStep;
import com.grahambartley.hardwired.device.MainPanelActions;
import com.grahambartley.hardwired.device.PanelAction;
import com.grahambartley.hardwired.device.PanelKind;
import com.grahambartley.hardwired.device.PanelMemory;
import com.grahambartley.hardwired.device.PanelRoster;
import com.grahambartley.hardwired.device.PanelRow;
import com.grahambartley.hardwired.network.MainPanelActionPayload;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.LampLook;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import com.grahambartley.hardwired.wire.WireViewer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class MainPanelGameTest implements FabricGameTest {

  private static final String BENCH = "hardwired:bench";
  private static final int FLOOR_Y = -1;
  private static final int SETTLE_TICKS = 4;
  private static final int LAMP_SETTLE_TICKS = 12;
  private static final int TICK_LIMIT = 80;
  private static final int MAX_RUN_TO_UNLOADED = 1200;
  private static final BlockPos PANEL = at(1, 1);
  private static final BlockPos LAMP = at(3, 1);
  private static final BlockPos SWITCH = at(5, 1);
  private static final BlockPos CONTROLLER = at(7, 1);
  private static final BlockPos NEAR_PANEL = at(1, 3);
  private static final LampLook RED_DIM = new LampLook(Hue.RED, 4);
  private static final LampLook BLUE_BRIGHT = new LampLook(Hue.BLUE, 9);
  private static final Identifier SMART_HOME = Identifier.of(Hardwired.MOD_ID, "smart_home");

  private static BlockPos at(int x, int z) {
    return new BlockPos(x, FLOOR_Y + 1, z);
  }

  private static WireEngine engine(TestContext context) {
    return WireEngine.of(context.getWorld());
  }

  private static WireCell cell(TestContext context, int x, int z, UUID owner) {
    return new WireCell(
        new WireFace(context.getAbsolutePos(new BlockPos(x, FLOOR_Y, z)), Direction.UP),
        Hue.PLAIN,
        owner);
  }

  private static BlockState floor(net.minecraft.block.Block block) {
    return block.getDefaultState().with(WallMountedBlock.FACE, BlockFace.FLOOR);
  }

  private static void buildHouse(TestContext context, UUID owner) {
    context.setBlockState(PANEL, floor(ModBlocks.MAIN_PANEL));
    context.setBlockState(LAMP, ModBlocks.COPPER_LAMP);
    context.setBlockState(SWITCH, floor(ModBlocks.WALL_SWITCH));
    context.setBlockState(CONTROLLER, ModBlocks.CONTROLLER);
    for (int x = 1; x <= 7; x++) {
      engine(context).lay(cell(context, x, 1, owner));
    }
  }

  private static MainPanelActionPayload order(
      TestContext context, BlockPos target, PanelAction action, LampLook look) {
    return new MainPanelActionPayload(
        context.getAbsolutePos(PANEL), context.getAbsolutePos(target), action, look);
  }

  private static PanelMemory memory(TestContext context) {
    MainPanelBlockEntity panel = context.getBlockEntity(PANEL);
    return panel.memory();
  }

  private static PanelRoster.Listing listFor(TestContext context, UUID viewer) {
    return PanelRoster.list(
        context.getWorld(),
        context.getAbsolutePos(PANEL),
        new WireViewer(viewer, false),
        memory(context));
  }

  private static void assertLamp(TestContext context, LampLook look) {
    LampBlockEntity lamp = context.getBlockEntity(LAMP);
    context.assertTrue(
        lamp.color() == look.color() && lamp.brightness() == look.brightness(),
        "lamp shows " + lamp.color() + " " + lamp.brightness() + ", wanted " + look);
  }

  private static void finish(TestContext context, ConnectedTestPlayer... players) {
    for (ConnectedTestPlayer player : players) {
      player.leave();
    }
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void thePanelListsEveryDeviceOnItsNetwork(TestContext context) {
    buildHouse(context, null);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          List<PanelRow> rows = listFor(context, UUID.randomUUID()).rows();
          context.assertTrue(
              rows.stream()
                  .map(PanelRow::kind)
                  .toList()
                  .equals(List.of(PanelKind.LAMP, PanelKind.SWITCH, PanelKind.CONTROLLER)),
              "wrong rows: " + rows);
          context.assertTrue(
              rows.get(0).pos().equals(context.getAbsolutePos(LAMP)), "wrong lamp position");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void opensWithAViewAndAnswersARefresh(TestContext context) {
    buildHouse(context, null);
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, NEAR_PANEL);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          BlockPos panel = context.getAbsolutePos(PANEL);
          MainPanelActions.open(player.player(), panel);
          MainPanelActions.act(player.player(), MainPanelActionPayload.refresh(panel));
          var views = player.panelViews();
          context.assertTrue(views.size() == 2, "two views must be sent, got " + views.size());
          context.assertTrue(views.get(0).open(), "the first view opens the screen");
          context.assertFalse(views.get(1).open(), "a refresh must not reopen the screen");
          context.assertTrue(views.get(1).rows().size() == 3, "the view must list three devices");
          finish(context, player);
        });
  }

  @GameTest(templateName = BENCH)
  public void aDistantRefreshGetsNoAnswer(TestContext context) {
    buildHouse(context, null);
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, at(1, 3).add(60, 0, 0));
    MainPanelActions.act(
        player.player(), MainPanelActionPayload.refresh(context.getAbsolutePos(PANEL)));
    context.assertTrue(player.panelViews().isEmpty(), "out of reach must see nothing");
    finish(context, player);
  }

  @GameTest(templateName = BENCH, tickLimit = TICK_LIMIT)
  public void theScreenFlipsSwitchesSetsLampsAndRunsControllers(TestContext context) {
    buildHouse(context, null);
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, NEAR_PANEL);
    ControllerBlockEntity controller = context.getBlockEntity(CONTROLLER);
    controller.setProgram(
        new ControllerProgram(
            ControllerWiring.DEFAULT,
            new Timeline(List.of(new TimelineStep(1, 40), new TimelineStep(3, 40)), 0)));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          MainPanelActions.perform(
              player.player(), order(context, SWITCH, PanelAction.TOGGLE, LampLook.DEFAULT));
          context.expectBlockProperty(SWITCH, MountedDeviceBlock.POWERED, true);
          MainPanelActions.perform(
              player.player(), order(context, LAMP, PanelAction.SET_LOOK, RED_DIM));
          MainPanelActions.perform(
              player.player(), order(context, CONTROLLER, PanelAction.TOGGLE, LampLook.DEFAULT));
          context.assertTrue(controller.run().isRunning(), "the panel must run the Controller");
          context.waitAndRun(
              LAMP_SETTLE_TICKS,
              () -> {
                assertLamp(context, RED_DIM);
                context.expectBlockProperty(LAMP, AbstractLampBlock.LEVEL, RED_DIM.brightness());
                context.complete();
                player.leave();
              });
        });
  }

  @GameTest(templateName = BENCH, tickLimit = TICK_LIMIT)
  public void aLampKeepsItsOwnLookThroughASwitchFlip(TestContext context) {
    buildHouse(context, null);
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, NEAR_PANEL);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          MainPanelActions.perform(
              player.player(), order(context, LAMP, PanelAction.SET_LOOK, RED_DIM));
          context.useBlock(SWITCH);
          context.waitAndRun(
              LAMP_SETTLE_TICKS,
              () -> {
                context.useBlock(SWITCH);
                context.waitAndRun(
                    LAMP_SETTLE_TICKS,
                    () -> {
                      context.useBlock(SWITCH);
                      context.waitAndRun(
                          LAMP_SETTLE_TICKS,
                          () -> {
                            assertLamp(context, RED_DIM);
                            context.expectBlockProperty(
                                LAMP, AbstractLampBlock.LEVEL, RED_DIM.brightness());
                            finish(context, player);
                          });
                    });
              });
        });
  }

  @GameTest(templateName = BENCH)
  public void aDistantPlayerCannotAct(TestContext context) {
    buildHouse(context, null);
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, at(1, 3).add(60, 0, 0));
    boolean acted =
        MainPanelActions.perform(
            player.player(), order(context, SWITCH, PanelAction.TOGGLE, LampLook.DEFAULT));
    context.assertFalse(acted, "out of reach must be refused");
    context.expectBlockProperty(SWITCH, MountedDeviceBlock.POWERED, false);
    finish(context, player);
  }

  @GameTest(templateName = BENCH)
  public void aPlayerWhoCannotBuildCanLookButNotAct(TestContext context) {
    buildHouse(context, null);
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, NEAR_PANEL);
    player.player().getAbilities().allowModifyWorld = false;
    MainPanelActions.perform(
        player.player(), order(context, SWITCH, PanelAction.TOGGLE, LampLook.DEFAULT));
    context.expectBlockProperty(SWITCH, MountedDeviceBlock.POWERED, false);
    finish(context, player);
  }

  @GameTest(templateName = BENCH)
  public void aTargetOffTheNetworkIsRefused(TestContext context) {
    buildHouse(context, null);
    BlockPos stray = at(3, 5);
    context.setBlockState(stray, floor(ModBlocks.WALL_SWITCH));
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, NEAR_PANEL);
    MainPanelActions.perform(
        player.player(), order(context, stray, PanelAction.TOGGLE, LampLook.DEFAULT));
    context.expectBlockProperty(stray, MountedDeviceBlock.POWERED, false);
    finish(context, player);
  }

  @GameTest(templateName = BENCH)
  public void sealedWireStaysHiddenFromStrangers(TestContext context) {
    UUID owner = UUID.randomUUID();
    buildHouse(context, owner);
    ConnectedTestPlayer stranger = ConnectedTestPlayer.joinAt(context, NEAR_PANEL);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.assertTrue(
              listFor(context, stranger.id()).rows().isEmpty(), "a stranger must list nothing");
          context.assertTrue(
              listFor(context, owner).rows().size() == 3, "the owner must list everything");
          MainPanelActions.perform(
              stranger.player(), order(context, SWITCH, PanelAction.TOGGLE, LampLook.DEFAULT));
          context.expectBlockProperty(SWITCH, MountedDeviceBlock.POWERED, false);
          finish(context, stranger);
        });
  }

  @GameTest(templateName = BENCH, tickLimit = TICK_LIMIT)
  public void anUnloadedDeviceShowsWhatThePanelKnows(TestContext context) {
    buildHouse(context, null);
    List<WireCell> run = new ArrayList<>();
    BlockPos unloaded = null;
    for (int x = 8; x < MAX_RUN_TO_UNLOADED && unloaded == null; x++) {
      WireCell next = cell(context, x, 1, null);
      engine(context).lay(next);
      run.add(next);
      if (!engine(context).isLoaded(next.face().pos())) {
        unloaded = next.face().occupant();
      }
    }
    BlockPos far = unloaded;
    context.assertTrue(far != null, "the run must reach an unloaded chunk");
    memory(context).remember(new PanelRow(far, PanelKind.SWITCH, true, Hue.PLAIN, 0, 0, false));
    PanelRoster.Listing listing = listFor(context, UUID.randomUUID());
    context.assertTrue(
        listing.rows().stream().anyMatch(row -> row.pos().equals(far) && row.stale() && row.on()),
        "the panel must show what it remembers");
    context.assertTrue(listing.unknownPoints() > 0, "other points must count as unknown");
    context.assertFalse(
        context.getWorld().getChunkManager().isChunkLoaded(far.getX() >> 4, far.getZ() >> 4),
        "listing must not load the chunk");
    run.forEach(engine(context)::cut);
    context.complete();
  }

  @GameTest(templateName = BENCH, tickLimit = TICK_LIMIT)
  public void twoPlayersOnOneLampLeaveTheLastChange(TestContext context) {
    buildHouse(context, null);
    ConnectedTestPlayer first = ConnectedTestPlayer.joinAt(context, NEAR_PANEL);
    ConnectedTestPlayer second = ConnectedTestPlayer.joinAt(context, at(2, 3));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          MainPanelActions.perform(
              first.player(), order(context, LAMP, PanelAction.SET_LOOK, RED_DIM));
          MainPanelActions.perform(
              second.player(), order(context, LAMP, PanelAction.SET_LOOK, BLUE_BRIGHT));
          context.waitAndRun(
              LAMP_SETTLE_TICKS,
              () -> {
                assertLamp(context, BLUE_BRIGHT);
                finish(context, first, second);
              });
        });
  }

  @GameTest(templateName = BENCH, tickLimit = TICK_LIMIT)
  public void aControlPanelChangeAfterAMainPanelChangeWins(TestContext context) {
    buildHouse(context, null);
    context.setBlockState(at(2, 1), floor(ModBlocks.CONTROL_PANEL));
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, NEAR_PANEL);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          MainPanelActions.perform(
              player.player(), order(context, LAMP, PanelAction.SET_LOOK, RED_DIM));
          engine(context).applyLook(context.getAbsolutePos(LAMP), BLUE_BRIGHT);
          context.waitAndRun(
              LAMP_SETTLE_TICKS,
              () -> {
                assertLamp(context, BLUE_BRIGHT);
                finish(context, player);
              });
        });
  }

  @GameTest(templateName = BENCH, tickLimit = TICK_LIMIT)
  public void aMainPanelChangeAfterAControlPanelChangeWins(TestContext context) {
    buildHouse(context, null);
    context.setBlockState(at(2, 1), floor(ModBlocks.CONTROL_PANEL));
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, NEAR_PANEL);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          engine(context).applyLook(context.getAbsolutePos(LAMP), BLUE_BRIGHT);
          MainPanelActions.perform(
              player.player(), order(context, LAMP, PanelAction.SET_LOOK, RED_DIM));
          context.waitAndRun(
              LAMP_SETTLE_TICKS,
              () -> {
                assertLamp(context, RED_DIM);
                finish(context, player);
              });
        });
  }

  @GameTest(templateName = BENCH)
  public void smartHomeCountsDistinctDevicesAndShowsProgress(TestContext context) {
    context.setBlockState(PANEL, floor(ModBlocks.MAIN_PANEL));
    List<BlockPos> lamps = new ArrayList<>();
    for (int x = 2; x <= 7; x++) {
      lamps.add(at(x, 1));
    }
    lamps.add(at(7, 2));
    lamps.add(at(7, 3));
    lamps.forEach(lamp -> context.setBlockState(lamp, ModBlocks.COPPER_LAMP));
    for (int x = 1; x <= 7; x++) {
      engine(context).lay(cell(context, x, 1, null));
    }
    engine(context).lay(cell(context, 7, 2, null));
    engine(context).lay(cell(context, 7, 3, null));
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, NEAR_PANEL);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          for (int worked = 0; worked < lamps.size() - 1; worked++) {
            for (int repeat = 0; repeat < 2; repeat++) {
              MainPanelActions.perform(
                  player.player(),
                  order(context, lamps.get(worked), PanelAction.SET_LOOK, RED_DIM));
            }
          }
          context.assertTrue(
              player.stepsEarned(SMART_HOME) == lamps.size() - 1,
              "seven devices must show seven steps, got " + player.stepsEarned(SMART_HOME));
          context.assertFalse(player.hasEarned(SMART_HOME), "seven devices are not enough");
          MainPanelActions.perform(
              player.player(),
              order(context, lamps.get(lamps.size() - 1), PanelAction.SET_LOOK, RED_DIM));
          context.assertTrue(player.hasEarned(SMART_HOME), "the eighth device must earn it");
          finish(context, player);
        });
  }
}
