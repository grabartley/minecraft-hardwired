package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.device.DeviceActions;
import com.grahambartley.hardwired.network.WireChunkPayload;
import com.grahambartley.hardwired.network.WireChunkPayload.CellView;
import com.grahambartley.hardwired.network.WireInspectPayload;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.registry.ModItems;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.LampLook;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import com.grahambartley.hardwired.wire.WireGrid;
import com.grahambartley.hardwired.wire.WireInspector;
import com.grahambartley.hardwired.wire.WireLaying;
import com.grahambartley.hardwired.wire.WireOverrides;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class WireSealGameTest implements FabricGameTest {

  private static final String BENCH = "hardwired:bench";
  private static final int FLOOR_Y = -1;
  private static final int VIEW_TICKS = 6;
  private static final int MOVE_TICKS = 8;

  private static WireGrid grid(TestContext context) {
    return WireEngine.of(context.getWorld()).grid();
  }

  private static BlockPos standing(int x, int z) {
    return new BlockPos(x, FLOOR_Y + 1, z);
  }

  private static WireFace floor(TestContext context, int x, int z) {
    return new WireFace(context.getAbsolutePos(new BlockPos(x, FLOOR_Y, z)), Direction.UP);
  }

  private static List<WireFace> run(TestContext context, int fromX, int toX, int z) {
    return IntStream.rangeClosed(fromX, toX).mapToObj(x -> floor(context, x, z)).toList();
  }

  private static ItemStack wire(Hue hue, int count) {
    return new ItemStack(ModItems.wire(hue), count);
  }

  private static ItemStack honeycomb(int count) {
    return new ItemStack(Items.HONEYCOMB, count);
  }

  private static void lay(ConnectedTestPlayer player, List<WireFace> faces, Hue hue) {
    player.hold(wire(hue, faces.size()), ItemStack.EMPTY);
    WireLaying.lay(player.player(), faces);
  }

  private static void layAndSeal(ConnectedTestPlayer player, List<WireFace> faces, Hue hue) {
    lay(player, faces, hue);
    player.hold(honeycomb(1), wire(hue, 1));
    player.useOn(faces.get(0));
  }

  private static WireCell cell(WireFace face, Hue hue, UUID owner) {
    return new WireCell(face, hue, owner);
  }

  private static int count(ConnectedTestPlayer player, ItemStack kind) {
    int total = 0;
    for (ItemStack stack : player.player().getInventory().main) {
      total += ItemStack.areItemsEqual(stack, kind) ? stack.getCount() : 0;
    }
    for (ItemStack stack : player.player().getInventory().offHand) {
      total += ItemStack.areItemsEqual(stack, kind) ? stack.getCount() : 0;
    }
    return total;
  }

  private static int droppedWire(TestContext context, WireFace near, Hue hue) {
    return context
        .getWorld()
        .getEntitiesByClass(
            ItemEntity.class,
            new Box(near.pos()).expand(2),
            item -> item.getStack().isOf(ModItems.wire(hue)))
        .stream()
        .mapToInt(item -> item.getStack().getCount())
        .sum();
  }

  private static void finish(TestContext context, ConnectedTestPlayer... players) {
    for (ConnectedTestPlayer player : players) {
      player.leave();
    }
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void aStrangerGetsNoPacketDataForSealedWire(TestContext context) {
    ConnectedTestPlayer owner = ConnectedTestPlayer.joinAt(context, standing(4, 4));
    List<WireFace> faces = run(context, 2, 4, 2);
    layAndSeal(owner, faces, Hue.RED);
    owner.hold(wire(Hue.RED, 1), ItemStack.EMPTY);
    ConnectedTestPlayer stranger = ConnectedTestPlayer.joinAt(context, standing(5, 4));
    stranger.hold(wire(Hue.RED, 1), ItemStack.EMPTY);
    context.waitAndRun(
        VIEW_TICKS,
        () -> {
          BlockPos sealed = faces.get(1).pos();
          context.assertTrue(
              owner.shownCellsAt(sealed).stream().anyMatch(CellView::isSealed),
              "the owner must be sent their sealed wire");
          context.assertTrue(
              stranger.receivedCellsAt(sealed).isEmpty(),
              "a stranger must get no packet data for sealed wire");
          finish(context, owner, stranger);
        });
  }

  @GameTest(templateName = BENCH)
  public void aPanelSendsASealedLookOnlyToThoseWhoMaySeeIt(TestContext context) {
    ConnectedTestPlayer owner = ConnectedTestPlayer.joinAt(context, standing(4, 4));
    ConnectedTestPlayer stranger = ConnectedTestPlayer.joinAt(context, standing(5, 4));
    ConnectedTestPlayer operator = ConnectedTestPlayer.joinAt(context, standing(3, 4));
    operator.setOperator(true);
    operator.command("hardwired override");
    BlockPos panel = standing(3, 2);
    context.setBlockState(
        panel,
        ModBlocks.CONTROL_PANEL.getDefaultState().with(WallMountedBlock.FACE, BlockFace.FLOOR));
    layAndSeal(owner, run(context, 2, 4, 2), Hue.RED);
    LampLook sealedLook = new LampLook(Hue.BLUE, 5);
    context.waitAndRun(
        VIEW_TICKS,
        () -> {
          BlockPos opened = context.getAbsolutePos(panel);
          WireEngine.of(context.getWorld()).applyLook(opened, sealedLook);
          DeviceActions.open(owner.player(), opened);
          DeviceActions.open(stranger.player(), opened);
          DeviceActions.open(operator.player(), opened);
          context.assertTrue(
              owner.openedLooks().equals(List.of(sealedLook)), "the owner sees their look");
          context.assertTrue(
              stranger.openedLooks().equals(List.of(LampLook.DEFAULT)),
              "a stranger must get the default look, not the sealed one");
          context.assertTrue(
              operator.openedLooks().equals(List.of(sealedLook)), "override shows the look");
          finish(context, owner, stranger, operator);
        });
  }

  @GameTest(templateName = BENCH)
  public void aStrangerStopsSeeingWireOnceItIsSealed(TestContext context) {
    ConnectedTestPlayer owner = ConnectedTestPlayer.joinAt(context, standing(4, 4));
    ConnectedTestPlayer stranger = ConnectedTestPlayer.joinAt(context, standing(5, 4));
    List<WireFace> faces = run(context, 2, 4, 2);
    lay(owner, faces, Hue.RED);
    stranger.hold(wire(Hue.RED, 1), ItemStack.EMPTY);
    context.waitAndRun(
        VIEW_TICKS,
        () -> {
          context.assertFalse(
              stranger.shownCellsAt(faces.get(1).pos()).isEmpty(), "open wire is seen by anyone");
          owner.hold(honeycomb(1), wire(Hue.RED, 1));
          owner.useOn(faces.get(1));
          context.waitAndRun(
              VIEW_TICKS,
              () -> {
                context.assertTrue(
                    stranger.shownCellsAt(faces.get(1).pos()).isEmpty(),
                    "sealing must take the wire out of a stranger's view");
                finish(context, owner, stranger);
              });
        });
  }

  @GameTest(templateName = BENCH)
  public void theInspectorRefusesSealedWire(TestContext context) {
    ConnectedTestPlayer owner = ConnectedTestPlayer.joinAt(context, standing(4, 4));
    ConnectedTestPlayer stranger = ConnectedTestPlayer.joinAt(context, standing(5, 4));
    List<WireFace> faces = run(context, 2, 4, 2);
    layAndSeal(owner, faces, Hue.RED);
    owner.hold(wire(Hue.RED, 1), ItemStack.EMPTY);
    stranger.hold(wire(Hue.RED, 1), ItemStack.EMPTY);
    WireInspectPayload request = new WireInspectPayload(faces.get(0), Hue.RED.ordinal());
    WireInspector.inspect(owner.player(), request);
    WireInspector.inspect(stranger.player(), request);
    context.assertTrue(owner.inspectResults() == 1, "the owner may inspect their wire");
    context.assertTrue(stranger.inspectResults() == 0, "a stranger gets no reply at all");
    finish(context, owner, stranger);
  }

  @GameTest(templateName = BENCH)
  public void twoPlayersSealingInOneTickUseOneHoneycomb(TestContext context) {
    ConnectedTestPlayer first = ConnectedTestPlayer.joinAt(context, standing(3, 4));
    ConnectedTestPlayer second = ConnectedTestPlayer.joinAt(context, standing(4, 4));
    List<WireFace> faces = run(context, 2, 4, 2);
    lay(first, faces, Hue.PLAIN);
    first.hold(honeycomb(2), wire(Hue.PLAIN, 1));
    second.hold(honeycomb(2), wire(Hue.PLAIN, 1));
    first.useOn(faces.get(0));
    second.useOn(faces.get(2));
    int honeycombs = count(first, honeycomb(1)) + count(second, honeycomb(1));
    context.assertTrue(honeycombs == 3, "one seal must use one honeycomb, left " + honeycombs);
    context.assertTrue(
        first
            .id()
            .equals(grid(context).networkOf(cell(faces.get(1), Hue.PLAIN, first.id())).owner()),
        "the first request wins the seal");
    finish(context, first, second);
  }

  @GameTest(templateName = BENCH)
  public void sealingTwiceInOneTickUsesOneHoneycomb(TestContext context) {
    ConnectedTestPlayer owner = ConnectedTestPlayer.joinAt(context, standing(3, 4));
    List<WireFace> faces = run(context, 2, 4, 2);
    lay(owner, faces, Hue.PLAIN);
    owner.hold(honeycomb(2), wire(Hue.PLAIN, 1));
    owner.useOn(faces.get(0));
    owner.useOn(faces.get(2));
    context.assertTrue(count(owner, honeycomb(1)) == 1, "a sealed network takes no more wax");
    finish(context, owner);
  }

  @GameTest(templateName = BENCH)
  public void aStrangerCannotCutSealedWire(TestContext context) {
    ConnectedTestPlayer owner = ConnectedTestPlayer.joinAt(context, standing(4, 4));
    ConnectedTestPlayer stranger = ConnectedTestPlayer.joinAt(context, standing(5, 4));
    List<WireFace> faces = run(context, 2, 4, 2);
    layAndSeal(owner, faces, Hue.RED);
    stranger.hold(wire(Hue.RED, 1), ItemStack.EMPTY);
    WireLaying.remove(stranger.player(), faces.get(1));
    context.assertTrue(
        grid(context).contains(cell(faces.get(1), Hue.RED, owner.id())), "wire must stay");
    context.assertTrue(count(stranger, wire(Hue.RED, 1)) == 1, "a stranger gains nothing");
    finish(context, owner, stranger);
  }

  @GameTest(templateName = BENCH)
  public void hiddenWireNeverDecidesWhichWireIsCut(TestContext context) {
    ConnectedTestPlayer owner = ConnectedTestPlayer.joinAt(context, standing(4, 4));
    ConnectedTestPlayer stranger = ConnectedTestPlayer.joinAt(context, standing(5, 4));
    List<WireFace> faces = run(context, 2, 3, 2);
    layAndSeal(owner, faces, Hue.RED);
    lay(stranger, faces.subList(0, 1), Hue.BLUE);
    stranger.hold(wire(Hue.RED, 1), ItemStack.EMPTY);
    WireLaying.remove(stranger.player(), faces.get(0));
    context.assertFalse(
        grid(context).contains(new WireCell(faces.get(0), Hue.BLUE)),
        "holding red with hidden red below, the visible blue wire is the one cut");
    context.assertTrue(
        grid(context).contains(cell(faces.get(0), Hue.RED, owner.id())), "sealed wire stays");
    finish(context, owner, stranger);
  }

  @GameTest(templateName = BENCH)
  public void aStrangersWireSitsOnAndBesideSealedWireWithNoSign(TestContext context) {
    ConnectedTestPlayer owner = ConnectedTestPlayer.joinAt(context, standing(4, 4));
    ConnectedTestPlayer stranger = ConnectedTestPlayer.joinAt(context, standing(5, 4));
    List<WireFace> faces = run(context, 2, 4, 2);
    layAndSeal(owner, faces, Hue.RED);
    WireFace shared = faces.get(1);
    WireFace beside = floor(context, 5, 2);
    lay(stranger, List.of(shared, beside), Hue.RED);
    stranger.hold(wire(Hue.RED, 1), ItemStack.EMPTY);
    WireGrid grid = grid(context);
    context.assertTrue(
        stranger.player().getMainHandStack().getCount() == 1
            && grid.networkOf(new WireCell(shared, Hue.RED)).size() == 1
            && grid.networkOf(new WireCell(beside, Hue.RED)).size() == 1,
        "each stranger's wire must stand alone");
    context.assertTrue(
        grid.networkOf(cell(faces.get(0), Hue.RED, owner.id())).size() == 3,
        "the sealed network must not grow");
    context.waitAndRun(
        VIEW_TICKS,
        () -> {
          CellView view = stranger.shownCellsAt(shared.pos()).get(0);
          context.assertTrue(
              stranger.shownCellsAt(shared.pos()).size() == 1
                  && view.arms() == 0
                  && (view.flags() & WireChunkPayload.LOOSE_END) != 0
                  && !view.isSealed(),
              "the stranger must see a lone open wire and nothing else: " + view);
          finish(context, owner, stranger);
        });
  }

  @GameTest(templateName = BENCH)
  public void joiningASealedNetworkSealsOpenWireAtNoCost(TestContext context) {
    ConnectedTestPlayer owner = ConnectedTestPlayer.joinAt(context, standing(4, 4));
    ConnectedTestPlayer stranger = ConnectedTestPlayer.joinAt(context, standing(5, 4));
    layAndSeal(owner, run(context, 1, 2, 2), Hue.PLAIN);
    lay(stranger, run(context, 4, 5, 2), Hue.PLAIN);
    lay(owner, run(context, 3, 3, 2), Hue.PLAIN);
    WireCell far = cell(floor(context, 5, 2), Hue.PLAIN, owner.id());
    context.assertTrue(grid(context).contains(far), "connected open wire must become sealed");
    context.assertTrue(grid(context).networkOf(far).size() == 5, "all of it is one network");
    context.assertFalse(
        grid(context).contains(new WireCell(far.face(), Hue.PLAIN)), "no open copy may stay");
    finish(context, owner, stranger);
  }

  @GameTest(templateName = BENCH)
  public void networksOfDifferentOwnersNeverJoin(TestContext context) {
    ConnectedTestPlayer first = ConnectedTestPlayer.joinAt(context, standing(3, 4));
    ConnectedTestPlayer second = ConnectedTestPlayer.joinAt(context, standing(4, 4));
    layAndSeal(first, run(context, 1, 3, 2), Hue.PLAIN);
    layAndSeal(second, run(context, 4, 6, 2), Hue.PLAIN);
    context.assertTrue(
        grid(context).networkOf(cell(floor(context, 3, 2), Hue.PLAIN, first.id())).size() == 3
            && grid(context).networkOf(cell(floor(context, 4, 2), Hue.PLAIN, second.id())).size()
                == 3,
        "touching lines of two owners must stay apart");
    finish(context, first, second);
  }

  @GameTest(templateName = BENCH)
  public void overrideOpensSealedWireToAnOperatorOnly(TestContext context) {
    ConnectedTestPlayer owner = ConnectedTestPlayer.joinAt(context, standing(4, 4));
    ConnectedTestPlayer operator = ConnectedTestPlayer.joinAt(context, standing(5, 4));
    ConnectedTestPlayer visitor = ConnectedTestPlayer.joinAt(context, standing(3, 4));
    List<WireFace> faces = run(context, 2, 4, 2);
    layAndSeal(owner, faces, Hue.RED);
    operator.setOperator(true);
    operator.hold(wire(Hue.RED, 1), ItemStack.EMPTY);
    visitor.hold(wire(Hue.RED, 1), ItemStack.EMPTY);
    visitor.command("hardwired override");
    context.assertFalse(
        WireOverrides.of(context.getWorld().getServer()).isOn(visitor.id()),
        "the command is for operators");
    context.waitAndRun(
        VIEW_TICKS,
        () -> {
          context.assertTrue(
              operator.receivedCellsAt(faces.get(1).pos()).isEmpty(),
              "an operator sees nothing until override is on");
          operator.command("hardwired override");
          context.waitAndRun(
              VIEW_TICKS,
              () -> {
                context.assertFalse(
                    operator.shownCellsAt(faces.get(1).pos()).isEmpty(), "override shows the wire");
                operator.setOperator(false);
                context.waitAndRun(
                    VIEW_TICKS,
                    () -> {
                      context.assertTrue(
                          operator.shownCellsAt(faces.get(1).pos()).isEmpty(),
                          "losing operator status must end the override");
                      operator.setOperator(true);
                      WireLaying.remove(operator.player(), faces.get(2));
                      context.assertFalse(
                          grid(context).contains(cell(faces.get(2), Hue.RED, owner.id())),
                          "override lets an operator cut sealed wire");
                      operator.command("hardwired override");
                      WireLaying.remove(operator.player(), faces.get(1));
                      context.assertTrue(
                          grid(context).contains(cell(faces.get(1), Hue.RED, owner.id())),
                          "with override off the operator is a stranger again");
                      finish(context, owner, operator, visitor);
                    });
              });
        });
  }

  @GameTest(templateName = BENCH)
  public void anAxeRemovesTheOwnersSealAndReturnsNothing(TestContext context) {
    ConnectedTestPlayer owner = ConnectedTestPlayer.joinAt(context, standing(4, 4));
    ConnectedTestPlayer stranger = ConnectedTestPlayer.joinAt(context, standing(5, 4));
    List<WireFace> faces = run(context, 2, 4, 2);
    layAndSeal(owner, faces, Hue.RED);
    ItemStack strangersAxe = new ItemStack(Items.IRON_AXE);
    stranger.hold(strangersAxe, wire(Hue.RED, 1));
    stranger.useOn(faces.get(0));
    context.assertTrue(
        grid(context).contains(cell(faces.get(0), Hue.RED, owner.id())),
        "a stranger cannot remove a seal");
    context.assertTrue(strangersAxe.getDamage() == 0, "a refused axe takes no wear");
    ItemStack axe = new ItemStack(Items.IRON_AXE);
    owner.hold(axe, wire(Hue.RED, 1));
    owner.useOn(faces.get(0));
    context.assertTrue(
        grid(context).contains(new WireCell(faces.get(2), Hue.RED)), "the whole network must open");
    context.assertTrue(count(owner, honeycomb(1)) == 0, "no honeycomb comes back");
    context.assertTrue(axe.getDamage() == 1, "scraping wears the axe as scraping copper does");
    finish(context, owner, stranger);
  }

  @GameTest(templateName = BENCH)
  public void removingASealOverOpenWireConservesWire(TestContext context) {
    ConnectedTestPlayer owner = ConnectedTestPlayer.joinAt(context, standing(4, 4));
    ConnectedTestPlayer stranger = ConnectedTestPlayer.joinAt(context, standing(5, 4));
    List<WireFace> faces = run(context, 2, 3, 2);
    layAndSeal(owner, faces, Hue.RED);
    lay(stranger, faces.subList(0, 1), Hue.RED);
    owner.hold(new ItemStack(Items.IRON_AXE), wire(Hue.RED, 1));
    owner.useOn(faces.get(1));
    context.assertTrue(
        grid(context).networkOf(new WireCell(faces.get(1), Hue.RED)).size() == 2,
        "two wires on one face become one network");
    context.assertTrue(droppedWire(context, faces.get(0), Hue.RED) == 1, "the spare wire drops");
    finish(context, owner, stranger);
  }

  @GameTest(templateName = BENCH)
  public void honeycombOnHiddenWireWaxesCopperAsUsual(TestContext context) {
    ConnectedTestPlayer owner = ConnectedTestPlayer.joinAt(context, standing(4, 4));
    ConnectedTestPlayer stranger = ConnectedTestPlayer.joinAt(context, standing(5, 4));
    BlockPos host = standing(3, 2);
    context.setBlockState(host, Blocks.COPPER_BLOCK);
    WireFace face = new WireFace(context.getAbsolutePos(host), Direction.UP);
    layAndSeal(owner, List.of(face), Hue.RED);
    context.expectBlock(Blocks.COPPER_BLOCK, host);
    stranger.hold(honeycomb(1), wire(Hue.RED, 1));
    stranger.useOn(face);
    context.expectBlock(Blocks.WAXED_COPPER_BLOCK, host);
    finish(context, owner, stranger);
  }

  @GameTest(templateName = BENCH)
  public void anOwnerWhoLeavesKeepsTheSealUntilAnOperatorClearsIt(TestContext context) {
    ConnectedTestPlayer owner = ConnectedTestPlayer.joinAt(context, standing(4, 4));
    ConnectedTestPlayer stranger = ConnectedTestPlayer.joinAt(context, standing(5, 4));
    ConnectedTestPlayer operator = ConnectedTestPlayer.joinAt(context, standing(3, 4));
    List<WireFace> faces = run(context, 2, 4, 2);
    layAndSeal(owner, faces, Hue.RED);
    UUID ownerId = owner.id();
    owner.leave();
    stranger.hold(wire(Hue.RED, 1), ItemStack.EMPTY);
    WireLaying.remove(stranger.player(), faces.get(0));
    context.assertTrue(
        grid(context).contains(cell(faces.get(0), Hue.RED, ownerId)),
        "the seal outlives its owner");
    operator.setOperator(true);
    operator.command("hardwired override");
    operator.hold(new ItemStack(Items.IRON_AXE), wire(Hue.RED, 1));
    operator.useOn(faces.get(0));
    context.assertTrue(
        grid(context).contains(new WireCell(faces.get(0), Hue.RED)),
        "an operator using override can clear it");
    finish(context, stranger, operator);
  }

  @GameTest(templateName = BENCH)
  public void breakingTheBlockDropsSealedWireForAnyone(TestContext context) {
    ConnectedTestPlayer owner = ConnectedTestPlayer.joinAt(context, standing(4, 4));
    BlockPos host = standing(3, 2);
    context.setBlockState(host, Blocks.STONE);
    WireFace face = new WireFace(context.getAbsolutePos(host), Direction.UP);
    layAndSeal(owner, List.of(face), Hue.RED);
    context.setBlockState(host, Blocks.AIR);
    context.waitAndRun(
        VIEW_TICKS,
        () -> {
          context.assertFalse(
              grid(context).contains(cell(face, Hue.RED, owner.id())), "the wire must come off");
          context.assertTrue(droppedWire(context, face, Hue.RED) == 1, "the wire must drop");
          finish(context, owner);
        });
  }

  @GameTest(templateName = BENCH)
  public void aPistonCarriesTheSealWithItsWire(TestContext context) {
    UUID owner = UUID.randomUUID();
    BlockPos host = standing(3, 3);
    context.setBlockState(
        standing(2, 3), Blocks.PISTON.getDefaultState().with(Properties.FACING, Direction.EAST));
    context.setBlockState(host, Blocks.STONE);
    WireFace face = new WireFace(context.getAbsolutePos(host), Direction.UP);
    WireEngine.of(context.getWorld()).lay(cell(face, Hue.PLAIN, owner));
    context.setBlockState(standing(2, 3).west(), Blocks.REDSTONE_BLOCK);
    context.waitAndRun(
        MOVE_TICKS,
        () -> {
          WireFace landed = new WireFace(context.getAbsolutePos(host.east()), Direction.UP);
          context.assertTrue(
              grid(context).contains(cell(landed, Hue.PLAIN, owner)),
              "the seal must land with the wire");
          context.assertFalse(
              grid(context).contains(new WireCell(landed, Hue.PLAIN)), "it must not land open");
          context.complete();
        });
  }
}
