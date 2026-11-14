package com.grahambartley.hardwired.gametest;

import static com.grahambartley.hardwired.gametest.RadioStage.*;

import com.grahambartley.hardwired.block.BroadcastDeskBlock;
import com.grahambartley.hardwired.block.BroadcastDeskBlockEntity;
import com.grahambartley.hardwired.block.CopperCare;
import com.grahambartley.hardwired.block.DeskPart;
import com.grahambartley.hardwired.block.RadioMastBlock;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.network.DeskActionPayload;
import com.grahambartley.hardwired.network.RadioSetPayload;
import com.grahambartley.hardwired.network.VoiceFramePayload;
import com.grahambartley.hardwired.radio.DeskAction;
import com.grahambartley.hardwired.radio.DeskActions;
import com.grahambartley.hardwired.radio.DeskVoice;
import com.grahambartley.hardwired.radio.Frequency;
import com.grahambartley.hardwired.radio.RadioActions;
import com.grahambartley.hardwired.radio.SongClock;
import com.grahambartley.hardwired.radio.VoiceFormat;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightningRodBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;

public class BroadcastDeskGameTest implements FabricGameTest {

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 120)
  public void aPausedPlaceSurvivesASaveAndClearsWhenItShould(TestContext context) {
    TestPower.keepFed(context);
    buildStation(context, 500);
    ConnectedTestPlayer player = playerAt(context, 3);
    context.waitAndRun(SETTLE, () -> goOnAirWithSong(context, player));
    context.waitAndRun(
        SETTLE + BEAT,
        () -> {
          act(context, player, PANEL, DeskAction.STOP);
          BroadcastDeskBlockEntity paused = desk(context);
          context.assertTrue(paused.songPlace() > 0, "Stop keeps a place");
          var registries = context.getWorld().getRegistryManager();
          BroadcastDeskBlockEntity reloaded =
              new BroadcastDeskBlockEntity(paused.getPos(), paused.getCachedState());
          reloaded.read(paused.createNbt(registries), registries);
          context.assertTrue(reloaded.songPlace() == paused.songPlace(), "the place must save");
          long now = context.getWorld().getTime();
          paused.afterBeat(DeviceStatus.NO_SOURCE, now + 1);
          context.assertTrue(paused.songPlace() > 0, "a short dip keeps the place");
          paused.afterBeat(DeviceStatus.NO_SOURCE, now + 1000);
          context.assertTrue(paused.songPlace() == SongClock.NO_PLACE, "a real cut clears it");
        });
    context.waitAndRun(
        SETTLE + 3L * BEAT,
        () -> {
          act(context, player, PANEL, DeskAction.PLAY);
          long now = context.getWorld().getTime();
          context.assertTrue(desk(context).songStart() == now, "a cleared place starts at the top");
          desk(context).afterBeat(DeviceStatus.RUNNING, now + 10_000_000L);
          context.assertTrue(desk(context).songPlace() == SongClock.NO_PLACE, "an end keeps none");
          act(context, player, PANEL, DeskAction.PLAY);
          act(context, player, PANEL, DeskAction.EJECT);
          context.assertTrue(desk(context).songPlace() == SongClock.NO_PLACE, "eject clears it");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 80)
  public void pressingPlayTwiceDoesNotRestartTheSong(TestContext context) {
    TestPower.keepFed(context);
    buildStation(context, 500);
    ConnectedTestPlayer player = playerAt(context, 3);
    long[] start = new long[1];
    context.waitAndRun(SETTLE, () -> goOnAirWithSong(context, player));
    context.waitAndRun(
        SETTLE + BEAT,
        () -> {
          start[0] = desk(context).songStart();
          act(context, player, PANEL, DeskAction.PLAY);
          context.assertTrue(desk(context).songStart() == start[0], "the song carries on");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 60)
  public void mastsFitThemselvesToTheColumnInEveryChange(TestContext context) {
    ConnectedTestPlayer player = playerAt(context, 4);
    context.setBlockState(FOOT, ModBlocks.RADIO_MAST);
    for (int y = 0; y < 3; y++) {
      player.hold(new ItemStack(ModBlocks.RADIO_MAST), ItemStack.EMPTY);
      player.useOn(new WireFace(context.getAbsolutePos(FOOT.up(y)), Direction.UP));
    }
    context.expectBlockProperty(FOOT, RadioMastBlock.TIER, 0);
    context.expectBlockProperty(FOOT.up(1), RadioMastBlock.TIER, 1);
    context.expectBlockProperty(FOOT.up(3), RadioMastBlock.TIER, 3);
    context.setBlockState(
        FOOT.up(4),
        Blocks.LIGHTNING_ROD.getDefaultState().with(LightningRodBlock.FACING, Direction.UP));
    context.expectBlockProperty(FOOT.up(3), RadioMastBlock.SEAT, true);
    context.getWorld().breakBlock(context.getAbsolutePos(FOOT.up(1)), false);
    context.expectBlockProperty(FOOT.up(2), RadioMastBlock.TIER, 0);
    context.expectBlockProperty(FOOT.up(3), RadioMastBlock.TIER, 1);
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 40)
  public void honeycombWaxesAMastAndAnAxeScrapesIt(TestContext context) {
    context.setBlockState(FOOT, ModBlocks.RADIO_MAST);
    ConnectedTestPlayer player = playerAt(context, 5);
    WireFace top = new WireFace(context.getAbsolutePos(FOOT), Direction.UP);
    player.hold(new ItemStack(Items.HONEYCOMB), ItemStack.EMPTY);
    player.useOn(top);
    context.expectBlockProperty(FOOT, CopperCare.WAXED, true);
    context.assertTrue(player.player().getMainHandStack().isEmpty(), "the wax must be used up");
    player.hold(new ItemStack(Items.IRON_AXE), ItemStack.EMPTY);
    player.useOn(top);
    context.expectBlockProperty(FOOT, CopperCare.WAXED, false);
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 60)
  public void twoPlayersFightingOverADiscKeepExactlyOne(TestContext context) {
    placeDesk(context, PANEL);
    ConnectedTestPlayer first = playerAt(context, 2);
    ConnectedTestPlayer second = playerAt(context, 4);
    first.hold(new ItemStack(Items.MUSIC_DISC_13), ItemStack.EMPTY);
    second.hold(new ItemStack(Items.MUSIC_DISC_13), ItemStack.EMPTY);
    WireFace top = new WireFace(context.getAbsolutePos(DESK), Direction.UP);
    first.useOn(top);
    second.useOn(top);
    context.assertTrue(desk(context).hasDisc(), "the first disc must go in");
    context.assertTrue(discsHeld(first) == 0, "the first player gave theirs up");
    context.assertTrue(discsHeld(second) == 1, "the second disc must stay in the hand");
    second.hold(ItemStack.EMPTY, ItemStack.EMPTY);
    act(context, first, PANEL, DeskAction.EJECT);
    act(context, second, PANEL, DeskAction.EJECT);
    context.assertFalse(desk(context).hasDisc(), "the disc must come out");
    context.assertTrue(
        discsHeld(first) + discsHeld(second) == 1, "exactly one player takes the disc");
    first.leave();
    second.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 60)
  public void breakingAnyCellTakesTheWholeDeskAndDropsItsDiscOnce(TestContext context) {
    placeDesk(context, PANEL);
    ConnectedTestPlayer player = playerAt(context, 2);
    insertDisc(context, player);
    context.getWorld().breakBlock(context.getAbsolutePos(TOP), true);
    context.expectBlock(Blocks.AIR, PANEL);
    context.expectBlock(Blocks.AIR, TOP);
    context.expectBlock(Blocks.AIR, DESK);
    context.assertTrue(discsNear(context, PANEL) == 1, "one disc must drop");
    int desks =
        context
            .getWorld()
            .getEntitiesByClass(
                ItemEntity.class,
                new Box(context.getAbsolutePos(PANEL)).expand(4),
                item -> item.getStack().isOf(ModBlocks.BROADCAST_DESK.asItem()))
            .stream()
            .mapToInt(item -> item.getStack().getCount())
            .sum();
    context.assertTrue(desks == 1, "one Desk must drop, not " + desks);
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 40)
  public void aDiscInAnOldRadioSpillsOnceWhenItLoads(TestContext context) {
    context.setBlockState(RADIO, ModBlocks.RADIO);
    var registries = context.getWorld().getRegistryManager();
    NbtCompound nbt = radio(context).createNbt(registries);
    nbt.put("disc", new ItemStack(Items.MUSIC_DISC_13).encode(registries));
    radio(context).read(nbt, registries);
    context.waitAndRun(
        SETTLE,
        () -> {
          context.assertTrue(discsNear(context, RADIO) == 1, "the saved disc must spill once");
          context.assertFalse(
              radio(context).createNbt(registries).contains("disc"), "the disc must not stay");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 60)
  public void aSealedDeskAnswersOnlyItsOwner(TestContext context) {
    wiredCell(context, 200);
    placeDesk(context, PANEL);
    raiseTower(context, MASTS, true);
    ConnectedTestPlayer owner = playerAt(context, 2);
    ConnectedTestPlayer stranger = playerAt(context, 4);
    WireEngine engine = WireEngine.of(context.getWorld());
    engine.grid().networksTouching(context.getAbsolutePos(PANEL)).stream()
        .findFirst()
        .ifPresent(network -> engine.reown(network, owner.id()));
    context.waitAndRun(
        SETTLE,
        () -> {
          act(context, stranger, PANEL, DeskAction.TOGGLE_AIR);
          context.assertFalse(desk(context).wantsAir(), "a stranger must not work a sealed Desk");
          act(context, owner, PANEL, DeskAction.TOGGLE_AIR);
          context.assertTrue(desk(context).wantsAir(), "the owner must work it");
          owner.leave();
          stranger.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 60)
  public void radioAndDeskRefuseSpectatorsAndPlayersOutOfReach(TestContext context) {
    wiredCell(context, 200);
    placeDesk(context, PANEL);
    context.setBlockState(RADIO, ModBlocks.RADIO);
    ConnectedTestPlayer spectator = playerAt(context, 3);
    ConnectedTestPlayer distant = playerAt(context, 3);
    spectator.player().changeGameMode(GameMode.SPECTATOR);
    distant.player().requestTeleport(0, 200, 0);
    for (ConnectedTestPlayer player : new ConnectedTestPlayer[] {spectator, distant}) {
      setRadio(context, player, false, Frequency.MAX, 10);
      act(context, player, PANEL, DeskAction.TOGGLE_AIR);
    }
    context.assertTrue(radio(context).isOn(), "the Radio must stay on");
    context.assertTrue(radio(context).frequency() == Frequency.DEFAULT, "the dial must not move");
    context.assertFalse(desk(context).wantsAir(), "the Desk must stay off the air");
    spectator.leave();
    distant.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 20)
  public void aPositionFarAwayIsNeverLoadedByADeskOrRadioPacket(TestContext context) {
    ConnectedTestPlayer player = playerAt(context, 3);
    BlockPos far = new BlockPos(1_000_000, 64, 1_000_000);
    int chunkX = ChunkSectionPos.getSectionCoord(far.getX());
    int chunkZ = ChunkSectionPos.getSectionCoord(far.getZ());
    ServerWorld world = context.getWorld();
    context.assertFalse(world.isChunkLoaded(chunkX, chunkZ), "the far chunk must start unloaded");
    for (DeskAction action : DeskAction.values()) {
      DeskActions.act(player.player(), new DeskActionPayload(far, action.ordinal(), 0));
    }
    DeskVoice.frame(
        player.player(), new VoiceFramePayload(far, new byte[VoiceFormat.MOST_FRAME_BYTES]));
    RadioActions.set(player.player(), new RadioSetPayload(far, false, Frequency.MAX, 0));
    context.assertFalse(world.isChunkLoaded(chunkX, chunkZ), "the far chunk must stay unloaded");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 20)
  public void anActionOnAnythingButADeskIsIgnored(TestContext context) {
    ConnectedTestPlayer player = playerAt(context, 3);
    context.setBlockState(PANEL, Blocks.STONE);
    context.assertFalse(
        DeskActions.mayWork(player.player(), context.getAbsolutePos(PANEL)), "stone is not a Desk");
    act(context, player, PANEL, DeskAction.TOGGLE_AIR);
    context.expectBlock(Blocks.STONE, PANEL);
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 20)
  public void aSecondAirToggleInTheSameMomentIsDropped(TestContext context) {
    wiredCell(context, 200);
    placeDesk(context, PANEL);
    ConnectedTestPlayer player = playerAt(context, 3);
    act(context, player, PANEL, DeskAction.TOGGLE_AIR);
    act(context, player, PANEL, DeskAction.TOGGLE_AIR);
    context.assertTrue(desk(context).wantsAir(), "only the first toggle may land");
    context.waitAndRun(
        SETTLE,
        () -> {
          act(context, player, PANEL, DeskAction.TOGGLE_AIR);
          context.assertFalse(desk(context).wantsAir(), "a later toggle lands");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 20)
  public void anActionNumberThatDoesNotExistIsIgnored(TestContext context) {
    wiredCell(context, 200);
    placeDesk(context, PANEL);
    ConnectedTestPlayer player = playerAt(context, 3);
    int wrapsToToggleAir = DeskAction.TOGGLE_AIR.ordinal() + DeskAction.values().length;
    DeskActions.act(
        player.player(), new DeskActionPayload(context.getAbsolutePos(PANEL), wrapsToToggleAir, 0));
    context.assertFalse(desk(context).wantsAir(), "an unknown action must do nothing");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 80)
  public void aDeskKeepsItsSettingsThroughASaveAndNeverOffersItsDiscToACopy(TestContext context) {
    buildStation(context, 500);
    ConnectedTestPlayer player = playerAt(context, 3);
    context.waitAndRun(
        SETTLE,
        () -> {
          goOnAirWithSong(context, player);
          DeskActions.act(
              player.player(),
              new DeskActionPayload(
                  context.getAbsolutePos(PANEL), DeskAction.SET_FREQUENCY.ordinal(), 1387));
          var registries = context.getWorld().getRegistryManager();
          NbtCompound saved = desk(context).createNbt(registries);
          BroadcastDeskBlockEntity reloaded =
              new BroadcastDeskBlockEntity(desk(context).getPos(), desk(context).getCachedState());
          reloaded.setWorld(context.getWorld());
          reloaded.read(saved, registries);
          context.assertTrue(reloaded.frequency() == 1390, "the dial must snap and stay");
          context.assertTrue(reloaded.hasDisc(), "the disc must stay");
          context.assertTrue(reloaded.wantsAir(), "the on air state must stay");
          context.assertTrue(
              reloaded.songStart() == desk(context).songStart(), "the song start must stay");
          NbtCompound copied = saved.copy();
          reloaded.removeFromCopiedStackNbt(copied);
          context.assertFalse(copied.contains("disc"), "a copied block must not carry the disc");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = BATCH, tickLimit = 40)
  public void aDeskPlacedFromTheItemTakesItsThreeCells(TestContext context) {
    ConnectedTestPlayer player = playerAt(context, 4);
    context.setBlockState(at(4, 4), Blocks.STONE);
    player.hold(new ItemStack(ModBlocks.BROADCAST_DESK), ItemStack.EMPTY);
    player.player().setYaw(180F);
    player.useOn(new WireFace(context.getAbsolutePos(at(4, 4)), Direction.UP));
    BlockPos desk = at(4, 4).up();
    BlockPos panel = desk.west();
    Block expected = ModBlocks.BROADCAST_DESK;
    context.expectBlock(expected, desk);
    context.expectBlock(expected, panel);
    context.expectBlock(expected, panel.up());
    context.expectBlockProperty(panel, BroadcastDeskBlock.PART, DeskPart.PANEL);
    context.expectBlockProperty(panel.up(), BroadcastDeskBlock.PART, DeskPart.TOP);
    context.expectBlockProperty(desk, BroadcastDeskBlock.PART, DeskPart.DESK);
    context.assertTrue(
        context.getBlockEntity(panel) instanceof BroadcastDeskBlockEntity,
        "the panel owns the block entity");
    player.leave();
    context.complete();
  }
}
