package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.CircuitBoardBlockEntity;
import com.grahambartley.hardwired.circuit.Design;
import com.grahambartley.hardwired.circuit.DesignText;
import com.grahambartley.hardwired.circuit.Line;
import com.grahambartley.hardwired.circuit.Part;
import com.grahambartley.hardwired.circuit.PartType;
import com.grahambartley.hardwired.circuit.PortMode;
import com.grahambartley.hardwired.circuit.Refusal;
import com.grahambartley.hardwired.circuit.Side;
import com.grahambartley.hardwired.device.CircuitAction;
import com.grahambartley.hardwired.device.CircuitActions;
import com.grahambartley.hardwired.network.CircuitActionPayload;
import com.grahambartley.hardwired.network.CircuitResultPayload;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.registry.ModComponents;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class CircuitBoardGameTest implements FabricGameTest {

  private static final String BENCH = "hardwired:bench";
  private static final int SETTLE_TICKS = 6;
  private static final int RATE_GAP = 5;
  private static final BlockPos BOARD = new BlockPos(4, 0, 4);
  private static final BlockPos STANDING = new BlockPos(2, 0, 3);
  private static final Identifier LOGIC_GATE = Identifier.of(Hardwired.MOD_ID, "logic_gate");
  private static final Identifier POCKET_CIRCUIT =
      Identifier.of(Hardwired.MOD_ID, "pocket_circuit");
  private static final Identifier SHARE_THE_LOVE =
      Identifier.of(Hardwired.MOD_ID, "share_the_love");
  private static final int PART_NODE = Design.partNode(0);
  private static final List<PortMode> WEST_IN_EAST_OUT =
      List.of(PortMode.UNUSED, PortMode.OUTPUT, PortMode.UNUSED, PortMode.INPUT);
  private static final List<Line> THROUGH_PART =
      List.of(
          new Line(Design.sideNode(Side.WEST), PART_NODE, 0),
          new Line(PART_NODE, Design.sideNode(Side.EAST), 0));
  private static final Design INVERTER =
      new Design(WEST_IN_EAST_OUT, List.of(Part.of(PartType.NOT, 3, 3)), THROUGH_PART);
  private static final Design LATCH =
      new Design(WEST_IN_EAST_OUT, List.of(Part.of(PartType.SR_LATCH, 3, 3)), THROUGH_PART);
  private static final Design TIMED =
      new Design(WEST_IN_EAST_OUT, List.of(Part.of(PartType.TIMER, 3, 3)), THROUGH_PART);

  private static ItemStack circuitItem(Design design) {
    ItemStack stack = new ItemStack(ModBlocks.CIRCUIT_BLOCK);
    stack.set(ModComponents.CIRCUIT_DESIGN, design);
    return stack;
  }

  private static CircuitBoardBlockEntity board(TestContext context) {
    context.setBlockState(BOARD, ModBlocks.CIRCUIT_BOARD);
    return context.getBlockEntity(BOARD);
  }

  private static void act(
      TestContext context, ConnectedTestPlayer player, CircuitAction action, String text) {
    CircuitActions.act(
        player.player(), CircuitActionPayload.of(context.getAbsolutePos(BOARD), action, text));
  }

  private static String lastMessage(ConnectedTestPlayer player) {
    List<CircuitResultPayload> results = player.circuitResults();
    return results.isEmpty() ? "" : results.get(results.size() - 1).message();
  }

  private static int count(ConnectedTestPlayer player, Item item) {
    return player.player().getInventory().count(item);
  }

  private static void give(ConnectedTestPlayer player, Item item, int count) {
    player.player().getInventory().insertStack(new ItemStack(item, count));
  }

  @GameTest(templateName = BENCH)
  public void aBrokenBoardDropsItsDesign(TestContext context) {
    CircuitBoardBlockEntity board = board(context);
    board.setDesign(INVERTER, false);
    context.getWorld().breakBlock(context.getAbsolutePos(BOARD), true);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          List<ItemEntity> drops =
              context
                  .getWorld()
                  .getEntitiesByClass(
                      ItemEntity.class,
                      new Box(context.getAbsolutePos(BOARD)).expand(3),
                      entity -> entity.getStack().isOf(ModBlocks.CIRCUIT_BOARD.asItem()));
          context.assertTrue(drops.size() == 1, "one board must drop");
          context.assertTrue(
              INVERTER.equals(drops.get(0).getStack().get(ModComponents.CIRCUIT_DESIGN)),
              "the drop must carry the design");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void makingSpendsOneBlankAndTheCostAndGivesOneBlock(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    List<Part> parts = new ArrayList<>();
    for (int x = 0; x < 5; x++) {
      parts.add(Part.of(PartType.AND, x, 0));
    }
    Design fiveParts = new Design(Design.EMPTY.sides(), parts, List.of());
    board(context).setDesign(fiveParts, false);
    Item blank = ModBlocks.BLANK_CIRCUIT_BLOCK.asItem();
    Item made = ModBlocks.CIRCUIT_BLOCK.asItem();
    give(player, blank, 3);
    give(player, Items.REDSTONE, 7);
    act(context, player, CircuitAction.MAKE, "");
    context.assertTrue(count(player, blank) == 2, "one blank must be spent");
    context.assertTrue(count(player, Items.REDSTONE) == 5, "two redstone must be spent");
    context.assertTrue(count(player, made) == 1, "one Circuit Block must be given");
    context.assertTrue(
        3 + 7 == count(player, blank) + count(player, Items.REDSTONE) + 3,
        "items must only be spent at the stated cost");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void makingWithoutTheCostChangesNothing(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    board(context).setDesign(INVERTER, false);
    Item blank = ModBlocks.BLANK_CIRCUIT_BLOCK.asItem();
    give(player, Items.REDSTONE, 4);
    act(context, player, CircuitAction.MAKE, "");
    context.assertTrue(
        lastMessage(player).equals(Refusal.NEEDS_BLANK.key()), "the blank is missing");
    context.assertTrue(count(player, Items.REDSTONE) == 4, "nothing must be spent");
    context.assertTrue(count(player, ModBlocks.CIRCUIT_BLOCK.asItem()) == 0, "nothing made");
    player.player().getInventory().clear();
    give(player, blank, 1);
    context.waitAndRun(
        RATE_GAP,
        () -> {
          act(context, player, CircuitAction.MAKE, "");
          context.assertTrue(
              lastMessage(player).equals(Refusal.NEEDS_REDSTONE.key()), "redstone is missing");
          context.assertTrue(count(player, blank) == 1, "the blank must be kept");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void anEmptyBoardMakesNothing(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    board(context);
    Item blank = ModBlocks.BLANK_CIRCUIT_BLOCK.asItem();
    give(player, blank, 1);
    give(player, Items.REDSTONE, 1);
    act(context, player, CircuitAction.MAKE, "");
    context.assertTrue(lastMessage(player).equals(Refusal.EMPTY.key()), "an empty board");
    context.assertTrue(count(player, blank) == 1, "nothing must be spent");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void savingMakingAndCopyingEarnTheCircuitBranch(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    CircuitBoardBlockEntity board = board(context);
    give(player, ModBlocks.BLANK_CIRCUIT_BLOCK.asItem(), 1);
    give(player, Items.REDSTONE, 1);
    act(context, player, CircuitAction.SAVE, DesignText.encode(INVERTER));
    context.assertTrue(board.design().equals(INVERTER), "the board must hold the design");
    context.assertTrue(player.hasEarned(LOGIC_GATE), "saving earns Logic Gate");
    context.waitAndRun(
        RATE_GAP,
        () -> {
          act(context, player, CircuitAction.MAKE, "");
          context.assertTrue(player.hasEarned(POCKET_CIRCUIT), "making earns Pocket Circuit");
          context.assertFalse(player.hasEarned(SHARE_THE_LOVE), "a first make is not a copy");
          board.setDesign(LATCH, false);
          player.hold(circuitItem(INVERTER), ItemStack.EMPTY);
          give(player, ModBlocks.BLANK_CIRCUIT_BLOCK.asItem(), 1);
          give(player, Items.REDSTONE, 1);
          context.waitAndRun(
              RATE_GAP,
              () -> {
                act(context, player, CircuitAction.LOAD, "");
                context.assertTrue(
                    board.design().equals(INVERTER), "load must read the held block");
                context.waitAndRun(
                    RATE_GAP,
                    () -> {
                      act(context, player, CircuitAction.MAKE, "");
                      context.assertTrue(
                          player.hasEarned(SHARE_THE_LOVE), "copying earns Share The Love");
                      player.leave();
                      context.complete();
                    });
              });
        });
  }

  @GameTest(templateName = BENCH)
  public void loadingWithoutACircuitBlockIsRefused(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    CircuitBoardBlockEntity board = board(context);
    board.setDesign(LATCH, false);
    act(context, player, CircuitAction.LOAD, "");
    context.assertTrue(
        lastMessage(player).equals(Refusal.NOT_HOLDING_CIRCUIT.key()), "nothing is held");
    context.assertTrue(board.design().equals(LATCH), "the board must keep its design");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void aHostilePasteIsRefusedAndChangesNothing(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    CircuitBoardBlockEntity board = board(context);
    board.setDesign(LATCH, false);
    act(context, player, CircuitAction.PASTE, "HWC1|uuuu|not:1:1:0|4:4:0");
    context.assertTrue(
        lastMessage(player).equals(Refusal.LOOP_WITHOUT_DELAY.key()), "a loop must be refused");
    context.assertTrue(board.design().equals(LATCH), "the board must keep its design");
    context.waitAndRun(
        RATE_GAP,
        () -> {
          act(context, player, CircuitAction.PASTE, "not a design at all");
          context.assertTrue(lastMessage(player).equals(Refusal.BAD_TEXT.key()), "bad text");
          context.assertTrue(board.design().equals(LATCH), "the board must keep its design");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void aPasteFromAnotherWorldLandsOnTheBoard(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    CircuitBoardBlockEntity board = board(context);
    act(context, player, CircuitAction.PASTE, DesignText.encode(TIMED));
    context.assertTrue(board.design().equals(TIMED), "a good paste must land");
    context.assertFalse(board.isCopy(), "a paste is not a copy of a held block");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void aSecondActionInOneTickIsTooFast(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    CircuitBoardBlockEntity board = board(context);
    act(context, player, CircuitAction.SAVE, DesignText.encode(INVERTER));
    act(context, player, CircuitAction.SAVE, DesignText.encode(LATCH));
    context.assertTrue(lastMessage(player).equals(Refusal.TOO_FAST.key()), "must be held back");
    context.assertTrue(board.design().equals(INVERTER), "the held back save must do nothing");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void twoPlayersSavingOneBoardLeaveTheLastSave(TestContext context) {
    ConnectedTestPlayer first = ConnectedTestPlayer.joinAt(context, STANDING);
    ConnectedTestPlayer second = ConnectedTestPlayer.joinAt(context, STANDING);
    CircuitBoardBlockEntity board = board(context);
    act(context, first, CircuitAction.SAVE, DesignText.encode(INVERTER));
    act(context, second, CircuitAction.SAVE, DesignText.encode(LATCH));
    context.assertTrue(board.design().equals(LATCH), "the last save must win");
    first.leave();
    second.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void actionsNeedReachAndBuildRights(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    CircuitBoardBlockEntity board = board(context);
    BlockPos target = context.getAbsolutePos(BOARD);
    player.player().refreshPositionAfterTeleport(Vec3d.ofCenter(target.add(40, 0, 0)));
    act(context, player, CircuitAction.SAVE, DesignText.encode(INVERTER));
    context.assertTrue(board.design().equals(Design.EMPTY), "out of reach");
    player.player().refreshPositionAfterTeleport(Vec3d.ofCenter(target.add(1, 1, 0)));
    player.player().getAbilities().allowModifyWorld = false;
    act(context, player, CircuitAction.SAVE, DesignText.encode(INVERTER));
    context.assertTrue(board.design().equals(Design.EMPTY), "cannot build");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void anActionOnAnotherBlockDoesNothing(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, STANDING);
    context.setBlockState(BOARD, ModBlocks.CONTROLLER);
    CircuitActions.act(
        player.player(),
        new CircuitActionPayload(context.getAbsolutePos(BOARD), 99, "HWC1|uuuu||"));
    act(context, player, CircuitAction.SAVE, DesignText.encode(INVERTER));
    context.assertTrue(player.circuitResults().isEmpty(), "no reply for a block that is no board");
    player.leave();
    context.complete();
  }
}
