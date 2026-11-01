package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.ControllerBlock;
import com.grahambartley.hardwired.block.ControllerBlockEntity;
import com.grahambartley.hardwired.block.MountedDeviceBlock;
import com.grahambartley.hardwired.controller.ControllerProgram;
import com.grahambartley.hardwired.controller.ControllerWiring;
import com.grahambartley.hardwired.controller.Timeline;
import com.grahambartley.hardwired.controller.TimelineOrder;
import com.grahambartley.hardwired.controller.TimelineStep;
import com.grahambartley.hardwired.device.ControllerActions;
import com.grahambartley.hardwired.network.ControllerSetPayload;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.registry.ModComponents;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class ControllerGameTest implements FabricGameTest {

  private static final String BENCH = "hardwired:bench";
  private static final int FLOOR_Y = -1;
  private static final int SETTLE_TICKS = 4;
  private static final int LONG_WAIT = 20;
  private static final int RUN_TICKS = 2 * LONG_WAIT + 8;
  private static final int TICK_LIMIT = 160;
  private static final BlockPos SWITCH = at(1, 1);
  private static final BlockPos CONTROLLER = at(4, 1);
  private static final BlockPos RED_PISTON = at(5, 4);
  private static final BlockPos BLUE_PISTON = at(4, 4);
  private static final Identifier NOW_YOU_SEE_ME =
      Identifier.of(Hardwired.MOD_ID, "now_you_see_me");
  private static final ControllerWiring DOOR_WIRING =
      new ControllerWiring(
          Hue.PLAIN, Hue.PLAIN, List.of(Optional.of(Hue.RED), Optional.of(Hue.BLUE)));
  private static final ControllerProgram DOOR =
      new ControllerProgram(
          DOOR_WIRING,
          new Timeline(
              List.of(new TimelineStep(0b01, LONG_WAIT), new TimelineStep(0b11, LONG_WAIT)), 0));

  private static BlockPos at(int x, int z) {
    return new BlockPos(x, FLOOR_Y + 1, z);
  }

  private static void lay(TestContext context, int x, int z, Hue hue) {
    WireEngine.of(context.getWorld())
        .lay(
            new WireCell(
                new WireFace(context.getAbsolutePos(new BlockPos(x, FLOOR_Y, z)), Direction.UP),
                hue));
  }

  private static BlockState piston() {
    return Blocks.PISTON.getDefaultState().with(PistonBlock.FACING, Direction.SOUTH);
  }

  private static ControllerBlockEntity buildDoor(TestContext context, ControllerProgram program) {
    context.setBlockState(
        SWITCH,
        ModBlocks.WALL_SWITCH.getDefaultState().with(WallMountedBlock.FACE, BlockFace.FLOOR));
    context.setBlockState(CONTROLLER, ModBlocks.CONTROLLER);
    context.setBlockState(RED_PISTON, piston());
    context.setBlockState(BLUE_PISTON, piston());
    for (int x = 1; x <= 3; x++) {
      lay(context, x, 1, Hue.PLAIN);
    }
    for (int z = 1; z <= 3; z++) {
      lay(context, 5, z, Hue.RED);
    }
    for (int z = 2; z <= 3; z++) {
      lay(context, 4, z, Hue.BLUE);
    }
    ControllerBlockEntity controller = context.getBlockEntity(CONTROLLER);
    controller.setProgram(program);
    return controller;
  }

  private static boolean extended(TestContext context, BlockPos piston) {
    BlockState state = context.getBlockState(piston);
    return state.contains(PistonBlock.EXTENDED) && state.get(PistonBlock.EXTENDED);
  }

  private static List<String> watchPistons(TestContext context) {
    List<String> moves = new ArrayList<>();
    boolean[] last = {false, false};
    context.runAtEveryTick(
        () -> {
          boolean red = extended(context, RED_PISTON);
          boolean blue = extended(context, BLUE_PISTON);
          if (red != last[0]) {
            moves.add(red ? "red out" : "red in");
            last[0] = red;
          }
          if (blue != last[1]) {
            moves.add(blue ? "blue out" : "blue in");
            last[1] = blue;
          }
        });
    return moves;
  }

  private static void flipSwitch(TestContext context) {
    context.useBlock(SWITCH);
  }

  private static void assertRan(TestContext context, List<String> moves, String... expected) {
    context.assertTrue(moves.equals(List.of(expected)), "wrong piston order: " + moves);
  }

  private static void assertIdle(TestContext context) {
    context.expectBlockProperty(CONTROLLER, ControllerBlock.RUNNING, false);
  }

  @GameTest(templateName = BENCH, tickLimit = TICK_LIMIT)
  public void aTwoPistonDoorOpensAndClosesFromOneSwitch(TestContext context) {
    TestPower.keepFed(context);
    buildDoor(context, DOOR);
    List<String> moves = watchPistons(context);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          flipSwitch(context);
          context.waitAndRun(
              RUN_TICKS,
              () -> {
                assertRan(context, moves, "red out", "blue out");
                assertIdle(context);
                flipSwitch(context);
                context.waitAndRun(
                    RUN_TICKS,
                    () -> {
                      assertRan(context, moves, "red out", "blue out", "blue in", "red in");
                      assertIdle(context);
                      context.complete();
                    });
              });
        });
  }

  @GameTest(templateName = BENCH, tickLimit = TICK_LIMIT)
  public void anInterruptInTheFirstStepFinishesItThenUndoesIt(TestContext context) {
    TestPower.keepFed(context);
    ControllerBlockEntity controller = buildDoor(context, DOOR);
    List<String> moves = watchPistons(context);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          flipSwitch(context);
          context.waitAndRun(
              SETTLE_TICKS * 2,
              () -> {
                flipSwitch(context);
                context.waitAndRun(
                    SETTLE_TICKS,
                    () -> {
                      context.assertTrue(
                          controller.run().pending() == TimelineOrder.BACKWARD,
                          "the new order must wait for the step");
                      context.assertTrue(
                          extended(context, RED_PISTON), "the step must be allowed to finish");
                      context.waitAndRun(
                          RUN_TICKS,
                          () -> {
                            assertRan(context, moves, "red out", "red in");
                            assertIdle(context);
                            context.complete();
                          });
                    });
              });
        });
  }

  @GameTest(templateName = BENCH, tickLimit = TICK_LIMIT)
  public void anInterruptInTheLastStepRetractsBothPistonsInOrder(TestContext context) {
    TestPower.keepFed(context);
    buildDoor(context, DOOR);
    List<String> moves = watchPistons(context);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          flipSwitch(context);
          context.waitAndRun(
              LONG_WAIT + SETTLE_TICKS * 2,
              () -> {
                flipSwitch(context);
                context.waitAndRun(
                    RUN_TICKS + LONG_WAIT,
                    () -> {
                      assertRan(context, moves, "red out", "blue out", "blue in", "red in");
                      assertIdle(context);
                      context.complete();
                    });
              });
        });
  }

  @GameTest(templateName = BENCH, tickLimit = TICK_LIMIT)
  public void anOpenColourAndACloseColourRunEachWay(TestContext context) {
    TestPower.keepFed(context);
    ControllerProgram split =
        new ControllerProgram(
            new ControllerWiring(Hue.PLAIN, Hue.PURPLE, DOOR_WIRING.outputs()),
            new Timeline(List.of(new TimelineStep(0b01, SETTLE_TICKS)), 0));
    ControllerBlockEntity controller = buildDoor(context, split);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          flipSwitch(context);
          context.waitAndRun(
              RUN_TICKS / 2,
              () -> {
                context.assertTrue(extended(context, RED_PISTON), "the open colour must run it");
                flipSwitch(context);
                context.waitAndRun(
                    RUN_TICKS / 2,
                    () -> {
                      context.assertTrue(
                          extended(context, RED_PISTON), "turning open off must do nothing");
                      context.assertTrue(controller.run().position() == 1, "it must stay put");
                      controller.order(TimelineOrder.BACKWARD);
                      context.waitAndRun(
                          RUN_TICKS / 2,
                          () -> {
                            context.assertFalse(
                                extended(context, RED_PISTON), "closing must run it back");
                            context.complete();
                          });
                    });
              });
        });
  }

  @GameTest(templateName = BENCH, tickLimit = TICK_LIMIT)
  public void aTimelineReversesByItself(TestContext context) {
    TestPower.keepFed(context);
    ControllerProgram timed =
        new ControllerProgram(
            DOOR_WIRING, new Timeline(List.of(new TimelineStep(0b01, SETTLE_TICKS)), LONG_WAIT));
    buildDoor(context, timed);
    List<String> moves = watchPistons(context);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          flipSwitch(context);
          context.waitAndRun(
              RUN_TICKS,
              () -> {
                assertRan(context, moves, "red out", "red in");
                assertIdle(context);
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH, tickLimit = TICK_LIMIT)
  public void aSavedControllerKeepsItsPlaceMidRun(TestContext context) {
    TestPower.keepFed(context);
    ControllerBlockEntity controller = buildDoor(context, DOOR);
    context.waitAndRun(SETTLE_TICKS, () -> controller.order(TimelineOrder.FORWARD));
    context.waitAndRun(
        2L * SETTLE_TICKS,
        () -> {
          var registries = context.getWorld().getRegistryManager();
          NbtCompound saved = controller.createNbt(registries);
          ControllerBlockEntity reloaded =
              new ControllerBlockEntity(controller.getPos(), controller.getCachedState());
          reloaded.read(saved, registries);
          context.assertTrue(reloaded.program().equals(DOOR), "the timeline must be saved");
          context.assertTrue(reloaded.run().position() == 1, "the place must be saved");
          context.assertTrue(
              reloaded.run().ticksLeft() == controller.run().ticksLeft(),
              "the time left in the step must be saved");
          context.assertTrue(
              reloaded.run().travel() == TimelineOrder.FORWARD, "the direction must be saved");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void aBrokenControllerDropsItsTimeline(TestContext context) {
    context.setBlockState(CONTROLLER, ModBlocks.CONTROLLER);
    ControllerBlockEntity controller = context.getBlockEntity(CONTROLLER);
    controller.setProgram(DOOR);
    context.getWorld().breakBlock(context.getAbsolutePos(CONTROLLER), true);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          List<ItemEntity> drops =
              context
                  .getWorld()
                  .getEntitiesByClass(
                      ItemEntity.class,
                      new Box(context.getAbsolutePos(CONTROLLER)).expand(3),
                      item -> item.getStack().isOf(ModBlocks.CONTROLLER.asItem()));
          context.assertTrue(drops.size() == 1, "one Controller must drop, got " + drops.size());
          ItemStack stack = drops.get(0).getStack();
          context.assertTrue(
              DOOR.equals(stack.get(ModComponents.CONTROLLER_PROGRAM)),
              "the drop must carry the timeline");
          context.setBlockState(CONTROLLER, ModBlocks.CONTROLLER);
          ControllerBlockEntity placed = context.getBlockEntity(CONTROLLER);
          placed.readComponents(stack);
          context.assertTrue(placed.program().equals(DOOR), "placing must restore the timeline");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void aBlankControllerDropsAPlainItem(TestContext context) {
    context.setBlockState(CONTROLLER, ModBlocks.CONTROLLER);
    context.getWorld().breakBlock(context.getAbsolutePos(CONTROLLER), true);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          List<ItemEntity> drops =
              context
                  .getWorld()
                  .getEntitiesByClass(
                      ItemEntity.class,
                      new Box(context.getAbsolutePos(CONTROLLER)).expand(3),
                      item -> item.getStack().isOf(ModBlocks.CONTROLLER.asItem()));
          context.assertTrue(drops.size() == 1, "one Controller must drop, got " + drops.size());
          context.assertTrue(
              ItemStack.areItemsAndComponentsEqual(
                  drops.get(0).getStack(), ModBlocks.CONTROLLER.asItem().getDefaultStack()),
              "a blank Controller must stack with a crafted one");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void savingATimelineNeedsReachAndBuildRights(TestContext context) {
    context.setBlockState(CONTROLLER, ModBlocks.CONTROLLER);
    ControllerBlockEntity controller = context.getBlockEntity(CONTROLLER);
    ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
    BlockPos target = context.getAbsolutePos(CONTROLLER);
    ControllerSetPayload save = new ControllerSetPayload(target, DOOR);

    player.refreshPositionAfterTeleport(Vec3d.ofCenter(target.add(40, 0, 0)));
    ControllerActions.save(player, save);
    context.assertTrue(controller.program().equals(ControllerProgram.DEFAULT), "out of reach");

    player.refreshPositionAfterTeleport(Vec3d.ofCenter(target.add(1, 0, 0)));
    player.getAbilities().allowModifyWorld = false;
    ControllerActions.save(player, save);
    context.assertTrue(controller.program().equals(ControllerProgram.DEFAULT), "cannot build");

    player.getAbilities().allowModifyWorld = true;
    ControllerActions.save(player, save);
    context.assertTrue(controller.program().equals(DOOR), "a player in reach may save");
    context.complete();
  }

  @GameTest(templateName = BENCH, tickLimit = TICK_LIMIT)
  public void movingAPistonEarnsNowYouSeeMe(TestContext context) {
    TestPower.keepFed(context);
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, at(7, 7));
    buildDoor(context, DOOR);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          flipSwitch(context);
          context.waitAndRun(
              SETTLE_TICKS,
              () -> {
                context.assertTrue(
                    player.hasEarned(NOW_YOU_SEE_ME), "moving a piston must earn it");
                player.leave();
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH)
  public void anIdleControllerDrivesNothing(TestContext context) {
    context.setBlockState(
        SWITCH,
        ModBlocks.WALL_SWITCH
            .getDefaultState()
            .with(WallMountedBlock.FACE, BlockFace.FLOOR)
            .with(MountedDeviceBlock.POWERED, false));
    buildDoor(context, DOOR);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.assertFalse(extended(context, RED_PISTON), "nothing has asked it to run");
          assertIdle(context);
          context.complete();
        });
  }
}
