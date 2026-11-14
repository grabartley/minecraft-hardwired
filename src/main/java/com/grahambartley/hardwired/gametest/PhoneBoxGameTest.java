package com.grahambartley.hardwired.gametest;

import static com.grahambartley.hardwired.gametest.PhoneStage.BENCH;
import static com.grahambartley.hardwired.gametest.PhoneStage.FIRST;
import static com.grahambartley.hardwired.gametest.PhoneStage.SETTLE_TICKS;
import static com.grahambartley.hardwired.gametest.PhoneStage.at;
import static com.grahambartley.hardwired.gametest.PhoneStage.buildNetwork;
import static com.grahambartley.hardwired.gametest.PhoneStage.dial;
import static com.grahambartley.hardwired.gametest.PhoneStage.expectStatus;
import static com.grahambartley.hardwired.gametest.PhoneStage.leaveAll;
import static com.grahambartley.hardwired.gametest.PhoneStage.numberOf;
import static com.grahambartley.hardwired.gametest.PhoneStage.pickUp;

import com.grahambartley.hardwired.block.PhoneBoxBlock;
import com.grahambartley.hardwired.block.PhoneBoxPaint;
import com.grahambartley.hardwired.block.PhoneBoxPart;
import com.grahambartley.hardwired.device.PhoneDevice;
import com.grahambartley.hardwired.phone.LineStatus;
import com.grahambartley.hardwired.registry.ModBlocks;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class PhoneBoxGameTest implements FabricGameTest {

  private static final BlockPos BOX = at(3, 1);
  private static final BlockPos OUTSIDE = at(3, 0);

  private static void placeBox(TestContext context, PhoneBoxPaint paint) {
    for (PhoneBoxPart part : PhoneBoxPart.values()) {
      context.setBlockState(
          BOX.up(part.height()),
          ModBlocks.PHONE_BOX
              .getDefaultState()
              .with(PhoneBoxBlock.FACING, Direction.NORTH)
              .with(PhoneBoxBlock.PART, part)
              .with(PhoneBoxBlock.PAINT, paint));
    }
  }

  private static void expectEveryCell(
      TestContext context, Predicate<BlockState> rule, String message) {
    for (PhoneBoxPart part : PhoneBoxPart.values()) {
      context.assertTrue(rule.test(context.getBlockState(BOX.up(part.height()))), message);
    }
  }

  @GameTest(templateName = BENCH, tickLimit = 60)
  public void onlyAPlayerInsideTheBoxCanLiftItsPhone(TestContext context) {
    buildNetwork(context);
    placeBox(context, PhoneBoxPaint.OAK);
    ConnectedTestPlayer outsider = ConnectedTestPlayer.joinAt(context, OUTSIDE);
    ConnectedTestPlayer insider = ConnectedTestPlayer.joinAt(context, BOX);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          pickUp(context, outsider, BOX);
          context.expectBlockProperty(BOX, PhoneDevice.LIFTED, false);
          context.assertTrue(outsider.phoneViews().isEmpty(), "an outsider must get no dial");
          pickUp(context, insider, BOX);
          context.expectBlockProperty(BOX, PhoneDevice.LIFTED, true);
          expectStatus(context, insider, LineStatus.DIAL_TONE);
          leaveAll(outsider, insider);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 60)
  public void steppingOutOfTheBoxHangsUp(TestContext context) {
    buildNetwork(context);
    placeBox(context, PhoneBoxPaint.OAK);
    ConnectedTestPlayer insider = ConnectedTestPlayer.joinAt(context, BOX);
    context.waitAndRun(SETTLE_TICKS, () -> pickUp(context, insider, BOX));
    context.waitAndRun(
        SETTLE_TICKS + 2L,
        () -> {
          context.expectBlockProperty(BOX, PhoneDevice.LIFTED, true);
          Vec3d out = Vec3d.ofBottomCenter(context.getAbsolutePos(OUTSIDE));
          insider.player().teleport(context.getWorld(), out.x, out.y, out.z, 0F, 0F);
        });
    context.waitAndRun(
        SETTLE_TICKS + 5L,
        () -> {
          context.expectBlockProperty(BOX, PhoneDevice.LIFTED, false);
          leaveAll(insider);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void theBoxRingsLikeAnyPhone(TestContext context) {
    buildNetwork(context);
    placeBox(context, PhoneBoxPaint.OAK);
    ConnectedTestPlayer caller = ConnectedTestPlayer.joinAt(context, at(2, 3));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          pickUp(context, caller, FIRST);
          dial(context, caller, FIRST, numberOf(context, BOX));
          expectStatus(context, caller, LineStatus.RINGING_OUT);
        });
    context.waitAndRun(
        SETTLE_TICKS + 2L,
        () -> {
          context.expectBlockProperty(BOX, PhoneDevice.RINGING, true);
          leaveAll(caller);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 60)
  public void theDoorTurnsFromOutsideAndOnlyAClosedDoorLightsTheLamp(TestContext context) {
    buildNetwork(context);
    placeBox(context, PhoneBoxPaint.OAK);
    ConnectedTestPlayer outsider = ConnectedTestPlayer.joinAt(context, OUTSIDE);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          expectEveryCell(context, state -> state.get(PhoneBoxBlock.LIT), "a shut box lights");
          context.useBlock(BOX.up(), outsider.player());
          expectEveryCell(context, state -> state.get(PhoneBoxBlock.OPEN), "the door opens");
          expectEveryCell(context, state -> !state.get(PhoneBoxBlock.LIT), "an open box is dark");
          context.useBlock(BOX.up(), outsider.player());
          expectEveryCell(context, state -> !state.get(PhoneBoxBlock.OPEN), "the door shuts");
          expectEveryCell(context, state -> state.get(PhoneBoxBlock.LIT), "the lamp comes back");
          leaveAll(outsider);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 60)
  public void aBoxWithNoPowerStaysDark(TestContext context) {
    placeBox(context, PhoneBoxPaint.OAK);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          expectEveryCell(context, state -> !state.get(PhoneBoxBlock.LIT), "no power, no lamp");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 40)
  public void dyeingTheBoxPaintsEveryCell(TestContext context) {
    placeBox(context, PhoneBoxPaint.OAK);
    ConnectedTestPlayer outsider = ConnectedTestPlayer.joinAt(context, OUTSIDE);
    outsider.hold(new ItemStack(Items.RED_DYE, 2), ItemStack.EMPTY);
    context.waitAndRun(
        2L,
        () -> {
          context.useBlock(BOX.up(2), outsider.player());
          expectEveryCell(
              context,
              state -> state.get(PhoneBoxBlock.PAINT) == PhoneBoxPaint.RED,
              "every cell takes the paint");
          context.assertTrue(
              outsider.player().getMainHandStack().getCount() == 1, "painting uses one dye");
          leaveAll(outsider);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 40)
  public void breakingAnyCellTakesTheWholeBoxAndDropsOnePaintedItem(TestContext context) {
    placeBox(context, PhoneBoxPaint.GREEN);
    context.waitAndRun(
        2L,
        () -> {
          context.getWorld().breakBlock(context.getAbsolutePos(BOX.up(2)), true);
          for (PhoneBoxPart part : PhoneBoxPart.values()) {
            context.expectBlock(Blocks.AIR, BOX.up(part.height()));
          }
          BlockPos at = context.getAbsolutePos(BOX);
          List<ItemEntity> drops =
              context
                  .getWorld()
                  .getEntitiesByClass(ItemEntity.class, new Box(at).expand(3), drop -> true);
          context.assertTrue(drops.size() == 1, "the box must drop once, saw " + drops.size());
          ItemStack stack = drops.getFirst().getStack();
          context.assertTrue(stack.isOf(ModBlocks.PHONE_BOX.asItem()), "the drop is the box");
          context.assertTrue(
              stack
                      .getOrDefault(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT)
                      .getValue(PhoneBoxBlock.PAINT)
                  == PhoneBoxPaint.GREEN,
              "the drop keeps its paint");
          context.complete();
        });
  }
}
