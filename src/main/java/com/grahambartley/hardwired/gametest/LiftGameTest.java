package com.grahambartley.hardwired.gametest;

import static com.grahambartley.hardwired.gametest.LiftGameSupport.*;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.ElectronicBlockEntity;
import com.grahambartley.hardwired.block.RedstoneCellBlockEntity;
import com.grahambartley.hardwired.config.BurnRates;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.lift.LiftCarEntity;
import com.grahambartley.hardwired.lift.LiftDial;
import com.grahambartley.hardwired.lift.LiftPhase;
import com.grahambartley.hardwired.lift.LiftRiders;
import com.grahambartley.hardwired.lift.LiftShaft;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.registry.ModEntities;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.AfterBatch;
import net.minecraft.test.BeforeBatch;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameMode;

public class LiftGameTest implements FabricGameTest {

  private static HardwiredConfig savedConfig;

  @BeforeBatch(batchId = BATCH)
  public void strongCells(ServerWorld world) {
    savedConfig = Hardwired.SERVER_CONFIG;
    Hardwired.SERVER_CONFIG =
        savedConfig
            .withLift(TEST_PACE)
            .withPower(
                savedConfig
                    .power()
                    .withRatings(savedConfig.power().ratings().withCellRating(STRONG_CELL)));
  }

  @AfterBatch(batchId = BATCH)
  public void plainCells(ServerWorld world) {
    Hardwired.SERVER_CONFIG = savedConfig;
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void aRideEndsStandingInTheCarBehindAnOpenGate(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          context.assertTrue(isOpen(context, LOW), "the car opens the gate it stands at");
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 2,
        () -> {
          context.assertTrue(riding(context, rider), "rider mounted");
          context.assertFalse(isOpen(context, LOW), "gate closed behind the car");
        });
    context.runAtTick(
        START_TICK + SIX_BLOCK_RIDE_TICKS,
        () -> {
          expectInCar(context, rider, HIGH, "rider");
          context.assertTrue(isOpen(context, HIGH), "arrival gate open");
          BlockPos open = context.getAbsolutePos(gate(HIGH));
          context.assertTrue(
              context
                  .getWorld()
                  .getBlockState(open)
                  .getCollisionShape(context.getWorld(), open)
                  .isEmpty(),
              "an open gate leaves the whole way clear");
          context.assertFalse(isOpen(context, MIDDLE), "passed gate shut");
          context.assertTrue(rider.hasEarned(GOING_UP), "Going Up earned");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void theRiderStaysInTheCarWholeRide(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtEveryTick(
        () -> {
          long tick = context.getTick();
          if (tick > START_TICK + 2 && tick < START_TICK + TICKS_FOR_SIX_BLOCKS - 2) {
            ServerPlayerEntity player = rider.player();
            player.setSneaking(true);
            LiftCarEntity car = car(context);
            context.assertTrue(riding(context, rider), "dropped out at tick " + tick);
            context.assertFalse(player.isInsideWall(), "in a wall at tick " + tick);
            context.assertTrue(
                Math.abs(player.getY() - (car.getY() + LiftCarEntity.PLATE)) < 0.51,
                "rider is standing on the car");
          }
        });
    context.runAtTick(START_TICK + SIX_BLOCK_RIDE_TICKS, () -> finish(context, rider));
  }

  @BeforeBatch(batchId = BURN_BATCH)
  public void burnFast(ServerWorld world) {
    savedConfig = Hardwired.SERVER_CONFIG;
    Hardwired.SERVER_CONFIG =
        savedConfig
            .withLift(TEST_PACE)
            .withPower(
                savedConfig
                    .power()
                    .withRatings(savedConfig.power().ratings().withCellRating(STRONG_CELL))
                    .withBurn(new BurnRates(6, 6, 6)));
  }

  @AfterBatch(batchId = BURN_BATCH)
  public void restoreBurn(ServerWorld world) {
    Hardwired.SERVER_CONFIG = savedConfig;
  }

  @GameTest(templateName = SHAFT, batchId = BURN_BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void aCellBurnsOnlyWhileTheLiftMoves(TestContext context) {
    RedstoneCellBlockEntity cell = build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    double[] afterRide = new double[1];
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + SIX_BLOCK_RIDE_TICKS + AFTER_RIDE_TICKS,
        () -> {
          afterRide[0] = cell.exactCharge();
          context.assertTrue(afterRide[0] < FULL_CHARGE, "a ride burns charge: " + afterRide[0]);
        });
    context.runAtTick(
        START_TICK + 2 * SIX_BLOCK_RIDE_TICKS,
        () -> {
          context.assertTrue(afterRide[0] == cell.exactCharge(), "a waiting lift is free");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void callsFromOtherFloorsQueueWhileRiding(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, MIDDLE);
    ConnectedTestPlayer caller = ConnectedTestPlayer.joinAt(context, gate(LOW));
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, MIDDLE);
          choose(context, rider, MIDDLE, HIGH);
        });
    context.runAtTick(START_TICK + 4, () -> useGate(context, caller, LOW));
    context.runAtTick(
        START_TICK + 46,
        () -> {
          expectInCar(context, rider, HIGH, "rider");
          context.assertTrue(
              car(context).run().restingAt(context.getAbsolutePos(gate(HIGH)).getY()),
              "car waits at the top");
        });
    context.runAtTick(
        START_TICK + 46 + 112,
        () -> {
          context.assertTrue(isOpen(context, LOW), "the queued call is answered");
          context.assertFalse(isOpen(context, HIGH), "the top gate shut behind it");
          finish(context, rider, caller);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void twoRidersBothArriveSafely(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer first = riderFrom(context, LOW);
    ConnectedTestPlayer second = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, first, LOW);
          choose(context, first, LOW, MIDDLE);
        });
    context.runAtTick(
        START_TICK + 4,
        () -> {
          context.assertEquals(2, LiftRiders.aboard(car(context)).size(), "both aboard");
        });
    context.runAtTick(
        START_TICK + SIX_BLOCK_RIDE_TICKS,
        () -> {
          expectInCar(context, first, MIDDLE, "first");
          expectInCar(context, second, MIDDLE, "second");
          context.assertTrue(first.hasEarned(GOING_UP), "first earned it");
          context.assertTrue(second.hasEarned(GOING_UP), "second earned it");
          finish(context, first, second);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void runningDryStopsThenLowersToTheFloorBelow(TestContext context) {
    RedstoneCellBlockEntity cell = build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(START_TICK + 36, () -> cell.setCharge(0));
    context.runAtTick(
        START_TICK + 42,
        () -> {
          LiftCarEntity car = car(context);
          context.assertTrue(
              car.run().phase() == LiftPhase.STOPPED, "stopped: " + car.run().phase());
          context.assertTrue(
              car.run().stopReason() == DeviceStatus.OUT_OF_FUEL,
              "reason " + car.run().stopReason());
          context.assertTrue(riding(context, rider), "still riding while stopped");
          ElectronicBlockEntity head = context.getBlockEntity(HEAD);
          context.assertTrue(head.status() == DeviceStatus.OUT_OF_FUEL, "head shows why");
        });
    context.runAtTick(
        START_TICK + 160,
        () -> {
          expectInCar(context, rider, MIDDLE, "rider");
          context.assertTrue(isOpen(context, MIDDLE), "gate opened at the floor below");
          context.assertEquals(0, cell.charge(), "lowering is free");
          context.assertFalse(rider.hasEarned(GOING_UP), "a failed ride earns nothing");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void chargingAgainAnswersTheNextCall(TestContext context) {
    RedstoneCellBlockEntity cell = build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(START_TICK + 36, () -> cell.setCharge(0));
    context.runAtTick(
        START_TICK + 160,
        () -> {
          expectInCar(context, rider, MIDDLE, "rider");
          cell.setCharge(FULL_CHARGE);
          rider
              .player()
              .requestTeleport(
                  context.getAbsolute(net.minecraft.util.math.Vec3d.ofBottomCenter(inCar(MIDDLE)))
                      .x,
                  rider.player().getY(),
                  context.getAbsolute(net.minecraft.util.math.Vec3d.ofBottomCenter(inCar(MIDDLE)))
                      .z);
        });
    context.runAtTick(START_TICK + 164, () -> choose(context, rider, MIDDLE, HIGH));
    context.runAtTick(
        START_TICK + 164 + SIX_BLOCK_RIDE_TICKS,
        () -> {
          expectInCar(context, rider, HIGH, "rider");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void withNoFloorBelowRidersGetSlowFalling(TestContext context) {
    RedstoneCellBlockEntity cell = build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, MIDDLE);
    context.runAtTick(START_TICK + 20, () -> cell.setCharge(0));
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, MIDDLE);
          choose(context, rider, MIDDLE, HIGH);
        });
    context.runAtTick(
        START_TICK + 4,
        () -> {
          context.setBlockState(gate(LOW).up(), Blocks.AIR);
          context.setBlockState(gate(LOW), Blocks.AIR);
          context.setBlockState(gate(MIDDLE).up(), Blocks.AIR);
          context.setBlockState(gate(MIDDLE), Blocks.AIR);
        });
    context.runAtTick(
        START_TICK + 80,
        () -> {
          context.assertFalse(riding(context, rider), "rider let down");
          context.expectEntityHasEffect(rider.player(), StatusEffects.SLOW_FALLING, 0);
          context.assertTrue(
              rider.player().getBlockX() == context.getAbsolutePos(HEAD).getX(),
              "the rider falls in the shaft and is not moved to a gate");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void aBlockInTheShaftSendsTheCarBackToTheLastSafeFloor(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(START_TICK + 28, () -> context.setBlockState(inCar(HIGH), Blocks.STONE));
    context.runAtTick(
        START_TICK + 80,
        () -> {
          expectInCar(context, rider, MIDDLE, "rider");
          context.assertTrue(isOpen(context, MIDDLE), "safe floor open");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void aBlockedShaftRefusesTheRideBeforeItStarts(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          context.setBlockState(inCar(MIDDLE), Blocks.STONE);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 10,
        () -> {
          context.assertFalse(riding(context, rider), "no ride into a blocked shaft");
          context.assertTrue(isOpen(context, LOW), "gate stays open");
          ElectronicBlockEntity gate = context.getBlockEntity(gate(LOW));
          context.assertTrue(gate.status() == DeviceStatus.SHAFT_BLOCKED, "says why");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void breakingTheLiftMidRideLeavesRidersSafeAndDropsOneItem(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 34, () -> context.getWorld().breakBlock(context.getAbsolutePos(HEAD), true));
    context.runAtTick(
        START_TICK + 38,
        () -> {
          context.assertFalse(riding(context, rider), "rider off the car");
          context.assertTrue(shaft(context).car().isEmpty(), "car removed");
          BlockPos spot = rider.player().getBlockPos();
          context.assertTrue(
              spot.equals(context.getAbsolutePos(gate(MIDDLE))), "at the nearest gate: " + spot);
          context.assertFalse(isOpen(context, MIDDLE), "no car, no open gate");
          context.assertTrue(
              dial(context, MIDDLE).dial().equals(LiftDial.NONE), "the dial goes back to rest");
          Box box = context.getTestBox();
          int dropped =
              context.getWorld().getEntitiesByClass(ItemEntity.class, box, item -> true).stream()
                  .filter(item -> item.getStack().isOf(ModBlocks.LIFT.asItem()))
                  .mapToInt(item -> item.getStack().getCount())
                  .sum();
          context.assertEquals(dropped, 1, "Lift items dropped");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void aRiderWhoLeavesIsPlacedSafelyAndTheCarCarriesOn(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer leaver = ConnectedTestPlayer.joinAt(context, inCar(LOW));
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, leaver, LOW);
          choose(context, leaver, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 34,
        () -> {
          ServerPlayerEntity player = leaver.player();
          leaver.leave();
          context.assertTrue(
              player.getBlockPos().equals(context.getAbsolutePos(gate(MIDDLE))),
              "saved at the nearest gate: " + player.getBlockPos());
          context.assertTrue(shaft(context).car().isPresent(), "the car survives its rider");
        });
    context.runAtTick(
        START_TICK + SIX_BLOCK_RIDE_TICKS,
        () -> {
          context.assertTrue(isOpen(context, HIGH), "the car finished its trip");
          finish(context);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void aSavedCarResumesItsTripAfterAChunkReload(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 24,
        () -> {
          LiftCarEntity saved = car(context);
          NbtCompound nbt = new NbtCompound();
          saved.writeNbt(nbt);
          saved.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
          LiftCarEntity loaded = ModEntities.LIFT_CAR.create(context.getWorld());
          loaded.readNbt(nbt);
          loaded.setUuid(UUID.randomUUID());
          context.getWorld().spawnEntity(loaded);
          context.assertTrue(loaded.run().phase() == LiftPhase.MOVING, "still moving");
        });
    context.runAtTick(
        START_TICK + SIX_BLOCK_RIDE_TICKS,
        () -> {
          LiftCarEntity car = car(context);
          context.assertTrue(
              car.run().restingAt(context.getAbsolutePos(gate(HIGH)).getY()), "resumed to the top");
          context.assertTrue(isOpen(context, HIGH), "gate opened on arrival");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void aChoiceFromOutOfReachOrASpectatorIsIgnored(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    ConnectedTestPlayer far = ConnectedTestPlayer.joinAt(context, new BlockPos(5, HIGH + 4, 5));
    ConnectedTestPlayer watcher = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          watcher.player().changeGameMode(GameMode.SPECTATOR);
          choose(context, far, LOW, HIGH);
          choose(context, watcher, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 10,
        () -> {
          context.assertTrue(car(context).run().phase() == LiftPhase.RESTING, "no one rode");
          context.assertFalse(riding(context, rider), "no one mounted");
          finish(context, rider, far, watcher);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void floorsComeBackOnlyForThisShaftInHeightOrder(TestContext context) {
    build(context, FULL_CHARGE);
    context.setBlockState(new BlockPos(COLUMN_X + 1, LOW, COLUMN_Z), Blocks.STONE);
    int base = context.getAbsolutePos(BlockPos.ORIGIN).getY();
    List<Integer> levels = LiftShaft.levels(shaft(context).gates());
    context.assertTrue(
        levels.equals(List.of(base + LOW, base + MIDDLE, base + HIGH)), "levels " + levels);
    context.complete();
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void aCarIsGroundForAPlayerStandingOnIt(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, MIDDLE);
    context.runAtTick(START_TICK, () -> openAt(context, rider, MIDDLE));
    context.runAtTick(
        START_TICK + 2,
        () -> {
          context.assertTrue(LiftCarEntity.holdsUp(rider.player()), "stands on the plate");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, batchId = BATCH, tickLimit = RIDE_TICK_LIMIT)
  public void withNoGatesLeftTheCarGoesAndRidersLand(TestContext context) {
    RedstoneCellBlockEntity cell = build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 20,
        () -> {
          for (int level : List.of(LOW, MIDDLE, HIGH)) {
            context.setBlockState(gate(level).up(), Blocks.AIR);
            context.setBlockState(gate(level), Blocks.AIR);
          }
          cell.setCharge(0);
        });
    context.runAtTick(
        START_TICK + 80,
        () -> {
          context.assertFalse(riding(context, rider), "rider let down");
          context.assertTrue(shaft(context).car().isEmpty(), "the car is gone");
          finish(context, rider);
        });
  }
}
