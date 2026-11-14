package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.block.ExchangeBlock;
import com.grahambartley.hardwired.block.ExchangeLayout;
import com.grahambartley.hardwired.block.ExchangePart;
import com.grahambartley.hardwired.block.RedstoneCellBlockEntity;
import com.grahambartley.hardwired.block.TelephoneBlock;
import com.grahambartley.hardwired.network.PhoneActionPayload;
import com.grahambartley.hardwired.network.PhoneViewPayload;
import com.grahambartley.hardwired.phone.LineStatus;
import com.grahambartley.hardwired.phone.PhoneActions;
import com.grahambartley.hardwired.phone.PhoneDirectory;
import com.grahambartley.hardwired.phone.PhoneEntry;
import com.grahambartley.hardwired.phone.PhoneNumbering;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import java.util.List;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

final class PhoneStage {

  static final String BENCH = "hardwired:bench";
  static final int FLOOR_Y = -1;
  static final int SETTLE_TICKS = 25;
  static final BlockPos CELL = at(1, 1);
  static final BlockPos FIRST = at(2, 1);
  static final BlockPos EXCHANGE = at(4, 1);
  static final BlockPos SECOND = at(6, 1);
  static final BlockPos THIRD = at(7, 1);

  private PhoneStage() {}

  static BlockPos at(int x, int z) {
    return new BlockPos(x, FLOOR_Y + 1, z);
  }

  static RedstoneCellBlockEntity buildNetwork(TestContext context) {
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

  static void placePhone(TestContext context, BlockPos pos) {
    context.setBlockState(
        pos,
        ModBlocks.CANDLESTICK_TELEPHONE
            .getDefaultState()
            .with(TelephoneBlock.FACING, Direction.NORTH));
  }

  static void placeExchange(TestContext context) {
    for (ExchangePart part : ExchangePart.values()) {
      context.setBlockState(
          ExchangeLayout.cell(part, Direction.SOUTH, EXCHANGE),
          ModBlocks.EXCHANGE
              .getDefaultState()
              .with(ExchangeBlock.FACING, Direction.SOUTH)
              .with(ExchangeBlock.PART, part));
    }
  }

  static long abs(TestContext context, BlockPos pos) {
    return context.getAbsolutePos(pos).asLong();
  }

  static int numberOf(TestContext context, BlockPos pos) {
    return PhoneDirectory.of(context.getWorld())
        .entry(abs(context, pos))
        .map(PhoneEntry::number)
        .orElse(PhoneNumbering.NONE);
  }

  static LineStatus lastStatus(ConnectedTestPlayer player) {
    List<PhoneViewPayload> views = player.phoneViews();
    return views.isEmpty() ? null : LineStatus.byIndex(views.getLast().status());
  }

  static void expectStatus(TestContext context, ConnectedTestPlayer player, LineStatus expected) {
    LineStatus actual = lastStatus(player);
    context.assertTrue(actual == expected, "expected " + expected + " but saw " + actual);
  }

  static void pickUp(TestContext context, ConnectedTestPlayer player, BlockPos phone) {
    PhoneActions.use(player.player(), context.getAbsolutePos(phone));
  }

  static void dial(TestContext context, ConnectedTestPlayer player, BlockPos phone, int number) {
    PhoneActions.act(
        player.player(), PhoneActionPayload.dial(context.getAbsolutePos(phone), number));
  }

  static void leaveAll(ConnectedTestPlayer... players) {
    for (ConnectedTestPlayer player : players) {
      player.leave();
    }
  }
}
