package com.grahambartley.hardwired.gametest;

import static com.grahambartley.hardwired.gametest.LiftGameSupport.*;

import com.grahambartley.hardwired.lift.LiftCarEntity;
import com.grahambartley.hardwired.lift.LiftCarWalls;
import com.grahambartley.hardwired.lift.LiftShaft;
import com.grahambartley.hardwired.registry.ModBlocks;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class LiftGateGameTest implements FabricGameTest {

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void aClosedGateOpensForNothingButTheCar(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    ConnectedTestPlayer standing = ConnectedTestPlayer.joinAt(context, gate(MIDDLE).west(0));
    context.runAtTick(START_TICK, () -> openAt(context, rider, LOW));
    context.runAtTick(
        START_TICK + 2,
        () -> {
          context.setBlockState(gate(MIDDLE).north(), Blocks.REDSTONE_BLOCK);
          context.setBlockState(gate(MIDDLE).up().north(), Blocks.REDSTONE_BLOCK);
        });
    context.runAtTick(
        START_TICK + 6,
        () -> {
          context.assertFalse(isOpen(context, MIDDLE), "redstone opened a gate");
          BlockPos closed = context.getAbsolutePos(gate(MIDDLE));
          context.assertFalse(
              context
                  .getWorld()
                  .getBlockState(closed)
                  .getCollisionShape(context.getWorld(), closed)
                  .isEmpty(),
              "closed gate has collision");
          useGate(context, standing, MIDDLE);
          context.assertFalse(isOpen(context, MIDDLE), "a hand opened a gate");
          context.assertTrue(isOpen(context, LOW), "the car floor stays open");
        });
    context.runAtTick(
        START_TICK + 52,
        () -> {
          context.assertTrue(isOpen(context, MIDDLE), "the called car opens the gate");
          context.assertFalse(isOpen(context, LOW), "the left floor shut");
          finish(context, rider, standing);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void breakingAGateDropsOneItem(TestContext context) {
    build(context, FULL_CHARGE);
    context.getWorld().breakBlock(context.getAbsolutePos(gate(MIDDLE).up()), true);
    context.runAtTick(
        2,
        () -> {
          context.expectBlock(Blocks.AIR, gate(MIDDLE));
          ServerWorld world = context.getWorld();
          Item gateItem = ModBlocks.LIFT_FLOOR.asItem();
          int dropped =
              world
                  .getEntitiesByClass(ItemEntity.class, context.getTestBox(), item -> true)
                  .stream()
                  .filter(item -> item.getStack().isOf(gateItem))
                  .mapToInt(item -> item.getStack().getCount())
                  .sum();
          context.assertEquals(dropped, 1, "gate items dropped");
          context.complete();
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void theDialOnEveryGateFollowsTheCar(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + TICKS_FOR_SIX_BLOCKS / 2 + 2,
        () -> {
          double reading = dialReading(context, MIDDLE);
          context.assertTrue(reading > 0.1 && reading < 0.9, "mid ride reads " + reading);
          context.assertTrue(
              reading == dialReading(context, LOW) && reading == dialReading(context, HIGH),
              "every gate reads the same");
        });
    context.runAtTick(
        START_TICK + SIX_BLOCK_RIDE_TICKS,
        () -> {
          context.assertTrue(dialReading(context, LOW) == 1, "the top floor reads full");
          context.assertEquals(3, dial(context, HIGH).dial().levels().size(), "three floors");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void aWideGateOpensAndClosesAsOne(TestContext context) {
    build(context, FULL_CHARGE);
    growToLarge(context);
    placeWideGate(context, LOW);
    placeWideGate(context, HIGH);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          context.assertTrue(allOpen(context, LOW, true), "all four cells open");
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 3,
        () -> context.assertTrue(allOpen(context, LOW, false), "all four cells closed"));
    context.runAtTick(
        START_TICK + SIX_BLOCK_RIDE_TICKS,
        () -> {
          context.assertTrue(allOpen(context, HIGH, true), "the top gate opens whole");
          context.assertTrue(dialReading(context, HIGH) == 1, "the wide gate has a needle");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void aWideGateDoesNotServeASmallLift(TestContext context) {
    build(context, FULL_CHARGE);
    placeWideGate(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          BlockPos absolute = context.getAbsolutePos(gate(LOW));
          context.assertTrue(
              LiftShaft.forGate(context.getWorld(), absolute).isEmpty(), "no Lift for it");
          context.complete();
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void breakingOneCellTakesTheWholeWideGateAndDropsOneItem(TestContext context) {
    placeWideGate(context, MIDDLE);
    context.getWorld().breakBlock(context.getAbsolutePos(gate(MIDDLE).south().up()), true);
    context.runAtTick(
        2,
        () -> {
          context.expectBlock(Blocks.AIR, gate(MIDDLE));
          context.expectBlock(Blocks.AIR, gate(MIDDLE).up());
          context.expectBlock(Blocks.AIR, gate(MIDDLE).south());
          Item wide = ModBlocks.LARGE_LIFT_FLOOR.asItem();
          int dropped =
              context
                  .getWorld()
                  .getEntitiesByClass(ItemEntity.class, context.getTestBox(), item -> true)
                  .stream()
                  .filter(item -> item.getStack().isOf(wide))
                  .mapToInt(item -> item.getStack().getCount())
                  .sum();
          context.assertEquals(dropped, 1, "wide gate items dropped");
          context.complete();
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void placingAWideGateFillsFourCells(TestContext context) {
    BlockPos placed = new BlockPos(COLUMN_X, 1, 1);
    context.setBlockState(placed.down(), Blocks.STONE);
    context.setBlockState(placed.down().east(), Blocks.STONE);
    context.setBlockState(placed.down().west(), Blocks.STONE);
    context.setBlockState(placed.down().east(2), Blocks.STONE);
    BlockPos absolute = context.getAbsolutePos(placed);
    PlayerEntity player = context.createMockPlayer(GameMode.CREATIVE);
    ItemStack stack = new ItemStack(ModBlocks.LARGE_LIFT_FLOOR);
    player.setStackInHand(Hand.MAIN_HAND, stack);
    BlockHitResult hit =
        new BlockHitResult(Vec3d.ofCenter(absolute), Direction.UP, absolute, false);
    ActionResult result =
        ((BlockItem) stack.getItem())
            .place(new ItemPlacementContext(player, Hand.MAIN_HAND, stack, hit));
    context.assertTrue(result.isAccepted(), "placed: " + result);
    int cells = 0;
    for (BlockPos cell : BlockPos.iterate(placed.add(-1, 0, -1), placed.add(1, 1, 1))) {
      if (context.getBlockState(cell).isOf(ModBlocks.LARGE_LIFT_FLOOR)) {
        cells++;
      }
    }
    context.assertEquals(4, cells, "four cells");
    context.complete();
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void aWalkerStopsAtTheWallsButPassesTheOpening(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(START_TICK, () -> openAt(context, rider, LOW));
    context.runAtTick(
        START_TICK + 2,
        () -> {
          ServerPlayerEntity player = rider.player();
          Vec3d center = player.getPos();
          for (Direction wall : List.of(Direction.EAST, Direction.NORTH, Direction.SOUTH)) {
            player.setPosition(center);
            player.move(MovementType.SELF, Vec3d.of(wall.getVector()));
            context.assertTrue(
                player.getPos().distanceTo(center) < 0.25, wall + " wall let a walker through");
          }
          player.setPosition(center);
          player.move(MovementType.SELF, Vec3d.of(Direction.WEST.getVector()));
          context.assertTrue(
              player.getPos().distanceTo(center) > 0.9, "the open side stopped a walker");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void aParkedCarTurnsBackToItsGate(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(START_TICK, () -> openAt(context, rider, LOW));
    context.runAtTick(START_TICK + 2, () -> car(context).faceGate(Direction.NORTH));
    context.runAtTick(
        START_TICK + 42,
        () -> {
          context.assertTrue(
              LiftCarWalls.opening(car(context).quarterTurns()) == Direction.WEST,
              "the opening faces the gate again");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void aRiderCanWalkInAMovingCarButNotOutOfIt(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 34,
        () -> {
          ServerPlayerEntity player = rider.player();
          LiftCarEntity car = car(context);
          double floor = car.getY() + LiftCarEntity.PLATE;
          player.setPosition(car.getX(), floor - 0.02, car.getZ());
          player.move(MovementType.SELF, new Vec3d(0, 0, 0.1));
          context.assertTrue(
              player.getZ() > car.getZ() + 0.09, "a sunk rider can still step: " + player.getPos());
          player.setPosition(car.getX(), floor, car.getZ());
          player.move(MovementType.SELF, new Vec3d(-1, 0, 0));
          context.assertTrue(player.getX() > car.getX() - 0.5, "the gate side is shut mid ride");
          finish(context, rider);
        });
  }
}
