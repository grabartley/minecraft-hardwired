package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.BroadcastDeskBlock;
import com.grahambartley.hardwired.block.BroadcastDeskBlockEntity;
import com.grahambartley.hardwired.block.DeskPart;
import com.grahambartley.hardwired.block.ElectronicBlockEntity;
import com.grahambartley.hardwired.block.RadioBlockEntity;
import com.grahambartley.hardwired.block.RedstoneCellBlockEntity;
import com.grahambartley.hardwired.block.SpeakerBlockEntity;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.network.DeskActionPayload;
import com.grahambartley.hardwired.network.RadioSetPayload;
import com.grahambartley.hardwired.radio.DeskAction;
import com.grahambartley.hardwired.radio.DeskActions;
import com.grahambartley.hardwired.radio.Frequency;
import com.grahambartley.hardwired.radio.RadioActions;
import com.grahambartley.hardwired.radio.ReceptionMix;
import com.grahambartley.hardwired.radio.StationRegistry;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightningRodBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

final class RadioStage {

  static final String BENCH = "hardwired:bench";
  static final String BATCH = "radio";
  static final String SHORT_REACH_BATCH = "radio-short-reach";
  static final int FLOOR_Y = -1;
  static final int SETTLE = 4;
  static final int BEAT = 24;
  static final int MASTS = 3;
  static final BlockPos CELL = at(1, 1);
  static final BlockPos PANEL = at(3, 1);
  static final BlockPos TOP = PANEL.up();
  static final BlockPos DESK = at(3, 2);
  static final BlockPos RADIO = at(4, 1);
  static final BlockPos SPEAKER = at(5, 1);
  static final BlockPos FOOT = at(6, 1);
  static final BlockPos SECOND_PANEL = at(7, 1);
  static final Identifier TUNED_IN = Identifier.of(Hardwired.MOD_ID, "tuned_in");
  static final Identifier ON_THE_AIR = Identifier.of(Hardwired.MOD_ID, "on_the_air");

  static final int STATION_SPACING = 40;
  private static final int STATION_SLOTS = (Frequency.MAX - Frequency.MIN) / STATION_SPACING;
  private static final int MIN_STEPS_FROM_DEFAULT = 2;
  private static final AtomicInteger nextStation = new AtomicInteger();

  private RadioStage() {}

  static BlockPos at(int x, int z) {
    return new BlockPos(x, FLOOR_Y + 1, z);
  }

  static RedstoneCellBlockEntity wiredCell(TestContext context, int charge) {
    context.setBlockState(CELL, ModBlocks.REDSTONE_CELL);
    WireEngine engine = WireEngine.of(context.getWorld());
    for (int x = 1; x <= 7; x++) {
      engine.lay(
          new WireCell(
              new WireFace(context.getAbsolutePos(new BlockPos(x, FLOOR_Y, 1)), Direction.UP),
              Hue.PLAIN));
    }
    RedstoneCellBlockEntity cell = context.getBlockEntity(CELL);
    cell.setCharge(charge);
    return cell;
  }

  static void placeDesk(TestContext context, BlockPos panel) {
    BlockPos desk = panel.offset(Direction.SOUTH);
    context.setBlockState(desk, deskState(DeskPart.DESK));
    context.setBlockState(panel, deskState(DeskPart.PANEL));
    context.setBlockState(panel.up(), deskState(DeskPart.TOP));
  }

  static net.minecraft.block.BlockState deskState(DeskPart part) {
    return ModBlocks.BROADCAST_DESK
        .getDefaultState()
        .with(BroadcastDeskBlock.FACING, Direction.NORTH)
        .with(BroadcastDeskBlock.PART, part);
  }

  static void raiseTower(TestContext context, int masts, boolean rod) {
    for (int y = 0; y < masts; y++) {
      context.setBlockState(FOOT.up(y), ModBlocks.RADIO_MAST);
    }
    if (rod) {
      context.setBlockState(
          FOOT.up(masts),
          Blocks.LIGHTNING_ROD.getDefaultState().with(LightningRodBlock.FACING, Direction.UP));
    }
  }

  static RedstoneCellBlockEntity buildStation(TestContext context, int charge) {
    RedstoneCellBlockEntity cell = wiredCell(context, charge);
    placeDesk(context, PANEL);
    raiseTower(context, MASTS, true);
    context.setBlockState(RADIO, ModBlocks.RADIO);
    context.setBlockState(SPEAKER, ModBlocks.SPEAKER);
    int frequency = nextFrequency();
    desk(context).setFrequency(frequency);
    radio(context).set(true, frequency, ReceptionMix.MAX_VOLUME);
    return cell;
  }

  static int nextFrequency() {
    int frequency;
    do {
      frequency = Frequency.MIN + STATION_SPACING * (nextStation.getAndIncrement() % STATION_SLOTS);
    } while (Frequency.stepsApart(frequency, Frequency.DEFAULT) <= MIN_STEPS_FROM_DEFAULT);
    return frequency;
  }

  static boolean onTheAir(TestContext context, BlockPos panel) {
    BlockPos desk = context.getAbsolutePos(panel);
    return StationRegistry.of(context.getWorld()).stations().stream()
        .anyMatch(station -> station.desk().equals(desk));
  }

  static BroadcastDeskBlockEntity desk(TestContext context) {
    return context.getBlockEntity(PANEL);
  }

  static RadioBlockEntity radio(TestContext context) {
    return context.getBlockEntity(RADIO);
  }

  static SpeakerBlockEntity speaker(TestContext context) {
    return context.getBlockEntity(SPEAKER);
  }

  static ConnectedTestPlayer playerAt(TestContext context, int x) {
    return ConnectedTestPlayer.joinAt(context, at(x, 3));
  }

  static void act(
      TestContext context, ConnectedTestPlayer player, BlockPos panel, DeskAction action) {
    DeskActions.act(
        player.player(),
        new DeskActionPayload(context.getAbsolutePos(panel), action.ordinal(), Frequency.DEFAULT));
  }

  static void setRadio(
      TestContext context, ConnectedTestPlayer player, boolean on, int frequency, int volume) {
    RadioActions.set(
        player.player(), new RadioSetPayload(context.getAbsolutePos(RADIO), on, frequency, volume));
  }

  static void insertDisc(TestContext context, ConnectedTestPlayer player) {
    player.hold(new ItemStack(Items.MUSIC_DISC_13), ItemStack.EMPTY);
    player.useOn(new WireFace(context.getAbsolutePos(DESK), Direction.UP));
  }

  static void goOnAirWithSong(TestContext context, ConnectedTestPlayer player) {
    insertDisc(context, player);
    act(context, player, PANEL, DeskAction.TOGGLE_AIR);
    act(context, player, PANEL, DeskAction.PLAY);
  }

  static DeviceStatus status(TestContext context, BlockPos pos) {
    ElectronicBlockEntity entity = context.getBlockEntity(pos);
    return entity.status();
  }

  static void expectStatus(TestContext context, BlockPos pos, DeviceStatus expected) {
    DeviceStatus actual = status(context, pos);
    context.assertTrue(actual == expected, "expected " + expected + " but was " + actual);
  }

  static int discsNear(TestContext context, BlockPos pos) {
    return context
        .getWorld()
        .getEntitiesByClass(
            ItemEntity.class,
            new Box(context.getAbsolutePos(pos)).expand(4),
            item -> item.getStack().isOf(Items.MUSIC_DISC_13))
        .stream()
        .mapToInt(item -> item.getStack().getCount())
        .sum();
  }

  static int discsHeld(ConnectedTestPlayer player) {
    return player.player().getInventory().count(Items.MUSIC_DISC_13);
  }
}
