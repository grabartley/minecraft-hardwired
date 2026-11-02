package com.grahambartley.hardwired.gametest;

import static com.grahambartley.hardwired.gametest.LiftGameSupport.*;

import com.grahambartley.hardwired.block.LargeLiftBlock;
import com.grahambartley.hardwired.block.LiftFloorBlock;
import com.grahambartley.hardwired.lift.LiftCarEntity;
import com.grahambartley.hardwired.lift.LiftCorner;
import com.grahambartley.hardwired.lift.LiftRiders;
import com.grahambartley.hardwired.lift.LiftShaft;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.registry.ModEntities;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class LargeLiftGameTest implements FabricGameTest {

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void aLargeLiftCarriesRidersAcrossItsWholeFloor(TestContext context) {
    build(context, FULL_CHARGE);
    growToLarge(context);
    BlockPos farCell = LiftCorner.SOUTH_EAST.cell(inCar(LOW));
    ConnectedTestPlayer near = riderFrom(context, LOW);
    ConnectedTestPlayer far = ConnectedTestPlayer.joinAt(context, farCell);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, near, LOW);
          choose(context, near, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 4,
        () -> {
          LiftCarEntity car = car(context);
          context.assertEquals(2, car.span(), "a large car");
          context.assertEquals(2, LiftRiders.aboard(car).size(), "both corners aboard");
          context.assertTrue(
              car.getType() == ModEntities.LARGE_LIFT_CAR, "the large car type: " + car.getType());
        });
    context.runAtTick(
        START_TICK + SIX_BLOCK_RIDE_TICKS,
        () -> {
          context.assertFalse(riding(context, near), "near rider set down");
          context.assertFalse(riding(context, far), "far rider set down");
          context.assertTrue(isOpen(context, HIGH), "the gate opens on to the large car");
          finish(context, near, far);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void aBlockInAnyColumnRefusesALargeLift(TestContext context) {
    build(context, FULL_CHARGE);
    growToLarge(context);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          context.setBlockState(LiftCorner.SOUTH_EAST.cell(inCar(MIDDLE)), Blocks.STONE);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 10,
        () -> {
          context.assertFalse(riding(context, rider), "no ride into a blocked column");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void breakingOneCellTakesTheWholeLargeLiftAndDropsOneItem(TestContext context) {
    growToLarge(context);
    context.getWorld().breakBlock(context.getAbsolutePos(LiftCorner.SOUTH_EAST.cell(HEAD)), true);
    context.runAtTick(
        2,
        () -> {
          for (LiftCorner corner : LiftCorner.values()) {
            context.expectBlock(Blocks.AIR, corner.cell(HEAD));
          }
          Item large = ModBlocks.LARGE_LIFT.asItem();
          int dropped =
              context
                  .getWorld()
                  .getEntitiesByClass(ItemEntity.class, context.getTestBox(), item -> true)
                  .stream()
                  .filter(item -> item.getStack().isOf(large))
                  .mapToInt(item -> item.getStack().getCount())
                  .sum();
          context.assertEquals(dropped, 1, "large lift items dropped");
          context.complete();
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void wireOnAnyLargeLiftCellServesItsGates(TestContext context) {
    build(context, FULL_CHARGE);
    growToLarge(context);
    WireEngine engine = WireEngine.of(context.getWorld());
    engine.cut(new WireCell(new WireFace(context.getAbsolutePos(HEAD), Direction.WEST), Hue.PLAIN));
    context.setBlockState(new BlockPos(GATE_X, HEAD_Y - 1, COLUMN_Z + 1), Blocks.STONE);
    wire(engine, new BlockPos(GATE_X, HEAD_Y - 1, COLUMN_Z + 1), Direction.UP, context);
    wire(engine, LiftCorner.SOUTH_WEST.cell(HEAD), Direction.WEST, context);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          context.assertFalse(shaft(context).gates().isEmpty(), "gates found through a far cell");
          openAt(context, rider, LOW);
          context.assertTrue(isOpen(context, LOW), "the car opens the gate");
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 4,
        () -> {
          context.assertTrue(riding(context, rider), "powered through a far cell");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void aGateMayFaceAnyColumnOfALargeLift(TestContext context) {
    build(context, FULL_CHARGE);
    growToLarge(context);
    BlockPos side = gate(HIGH).south();
    BlockState lower =
        ModBlocks.LIFT_FLOOR
            .getDefaultState()
            .with(LiftFloorBlock.FACING, Direction.EAST)
            .with(LiftFloorBlock.HALF, DoubleBlockHalf.LOWER);
    context.setBlockState(side.down(), Blocks.STONE);
    context.setBlockState(side, lower);
    context.setBlockState(side.up(), lower.with(LiftFloorBlock.HALF, DoubleBlockHalf.UPPER));
    WireEngine engine = WireEngine.of(context.getWorld());
    wire(engine, side.down(), Direction.UP, context);
    wire(engine, gate(HIGH).down(), Direction.UP, context);
    context.runAtTick(
        START_TICK,
        () -> {
          BlockPos absolute = context.getAbsolutePos(side);
          context.assertTrue(
              shaft(context).gates().contains(absolute), "the side gate is on the shaft");
          context.assertTrue(
              LiftShaft.forGate(context.getWorld(), absolute).isPresent(), "it finds the Lift");
          context.complete();
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void placingALargeLiftFillsFourCellsFacingOut(TestContext context) {
    BlockPos placed = new BlockPos(COLUMN_X, HEAD_Y, COLUMN_Z);
    BlockPos absolute = context.getAbsolutePos(placed);
    PlayerEntity player = context.createMockPlayer(GameMode.CREATIVE);
    ItemStack stack = new ItemStack(ModBlocks.LARGE_LIFT);
    player.setStackInHand(Hand.MAIN_HAND, stack);
    BlockHitResult hit =
        new BlockHitResult(Vec3d.ofCenter(absolute), Direction.UP, absolute, false);
    ActionResult result =
        ((BlockItem) stack.getItem())
            .place(new ItemPlacementContext(player, Hand.MAIN_HAND, stack, hit));
    context.assertTrue(result.isAccepted(), "placed: " + result);
    int cells = 0;
    for (BlockPos cell : BlockPos.iterate(placed.add(-1, 0, -1), placed.add(1, 0, 1))) {
      if (context.getBlockState(cell).isOf(ModBlocks.LARGE_LIFT)) {
        cells++;
      }
    }
    context.assertEquals(4, cells, "four cells");
    context.assertTrue(
        context.getBlockState(placed).get(LargeLiftBlock.FACING) == Direction.NORTH,
        "the near row faces the player");
    context.assertTrue(
        context.getBlockState(placed.south()).get(LargeLiftBlock.FACING) == Direction.SOUTH,
        "the far row faces away");
    context.complete();
  }
}
