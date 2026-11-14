package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.AbstractLampBlock;
import com.grahambartley.hardwired.block.MountedDeviceBlock;
import com.grahambartley.hardwired.block.PolePart;
import com.grahambartley.hardwired.block.RedstoneCellBlockEntity;
import com.grahambartley.hardwired.block.TelephonePoleBlock;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.registry.ModItems;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import com.grahambartley.hardwired.wire.WireSpan;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class PoleGameTest implements FabricGameTest {

  private static final String BENCH = "hardwired:bench";
  private static final int FLOOR_Y = -1;
  private static final int SETTLE_TICKS = 4;
  private static final int WEST_X = 1;
  private static final int MIDDLE_X = 4;
  private static final int EAST_X = 7;
  private static final int POLE_Z = 4;
  private static final int LIGHTNING_TICKS = 40;

  private static BlockPos standing(int x, int z) {
    return new BlockPos(x, FLOOR_Y + 1, z);
  }

  private static BlockPos top(int x) {
    return standing(x, POLE_Z).up();
  }

  private static void raisePole(TestContext context, int x, int height) {
    for (int y = 0; y < height; y++) {
      context.setBlockState(standing(x, POLE_Z).up(y), ModBlocks.OAK_TELEPHONE_POLE);
    }
  }

  private static void layRun(TestContext context, int x, int fromZ, int toZ, Hue hue) {
    WireEngine engine = WireEngine.of(context.getWorld());
    for (int z = fromZ; z <= toZ; z++) {
      engine.lay(
          new WireCell(
              new WireFace(context.getAbsolutePos(new BlockPos(x, FLOOR_Y, z)), Direction.UP),
              hue));
    }
  }

  private static BlockState floorSwitch(boolean on) {
    return ModBlocks.WALL_SWITCH
        .getDefaultState()
        .with(WallMountedBlock.FACE, BlockFace.FLOOR)
        .with(MountedDeviceBlock.POWERED, on);
  }

  private static ConnectedTestPlayer lineman(TestContext context, Hue hue, int wire) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, standing(MIDDLE_X, 2));
    player.hold(new ItemStack(ModItems.wire(hue), wire), ItemStack.EMPTY);
    return player;
  }

  private static void string(TestContext context, ConnectedTestPlayer player, int fromX, int toX) {
    use(context, player, top(fromX));
    use(context, player, top(toX));
  }

  private static ActionResult use(
      TestContext context, ConnectedTestPlayer player, BlockPos relative) {
    BlockPos target = context.getAbsolutePos(relative);
    return player
        .player()
        .interactionManager
        .interactBlock(
            player.player(),
            context.getWorld(),
            player.player().getMainHandStack(),
            Hand.MAIN_HAND,
            new BlockHitResult(Vec3d.ofCenter(target), Direction.UP, target, false));
  }

  private static List<WireSpan> spansAt(TestContext context, BlockPos relative) {
    return WireEngine.of(context.getWorld()).spansAt(context.getAbsolutePos(relative));
  }

  private static int wireHeld(ConnectedTestPlayer player, Hue hue) {
    return player.player().getInventory().count(ModItems.wire(hue));
  }

  private static int droppedWire(TestContext context, BlockPos near, Hue hue) {
    return context
        .getWorld()
        .getEntitiesByClass(
            ItemEntity.class,
            new Box(context.getAbsolutePos(near)).expand(3),
            item -> item.getStack().isOf(ModItems.wire(hue)))
        .stream()
        .mapToInt(item -> item.getStack().getCount())
        .sum();
  }

  @GameTest(templateName = BENCH)
  public void polesFitThemselvesToTheirColumn(TestContext context) {
    raisePole(context, WEST_X, 3);
    raisePole(context, EAST_X, 1);
    context.expectBlockProperty(standing(WEST_X, POLE_Z), TelephonePoleBlock.PART, PolePart.FOOT);
    context.expectBlockProperty(
        standing(WEST_X, POLE_Z).up(), TelephonePoleBlock.PART, PolePart.MIDDLE);
    context.expectBlockProperty(
        standing(WEST_X, POLE_Z).up(2), TelephonePoleBlock.PART, PolePart.TOP);
    context.expectBlockProperty(standing(EAST_X, POLE_Z), TelephonePoleBlock.PART, PolePart.SINGLE);
    context.complete();
  }

  @GameTest(templateName = BENCH, tickLimit = 60)
  public void aLampLightsFromASwitchAcrossThreePoles(TestContext context) {
    TestPower.keepFed(context);
    raisePole(context, WEST_X, 2);
    raisePole(context, MIDDLE_X, 2);
    raisePole(context, EAST_X, 2);
    context.setBlockState(standing(WEST_X, 1), floorSwitch(false));
    context.setBlockState(standing(EAST_X, 1), ModBlocks.LIGHT_STRIP);
    layRun(context, WEST_X, 1, POLE_Z, Hue.PLAIN);
    layRun(context, EAST_X, 1, POLE_Z, Hue.PLAIN);
    ConnectedTestPlayer player = lineman(context, Hue.PLAIN, 16);
    string(context, player, WEST_X, MIDDLE_X);
    string(context, player, MIDDLE_X, EAST_X);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.expectBlockProperty(standing(EAST_X, 1), AbstractLampBlock.LEVEL, 0);
          context.setBlockState(standing(WEST_X, 1), floorSwitch(true));
          context.waitAndRun(
              SETTLE_TICKS,
              () -> {
                context.expectBlockProperty(standing(EAST_X, 1), AbstractLampBlock.LEVEL, 15);
                player.leave();
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 60)
  public void twoColoursOnOnePoleStayApart(TestContext context) {
    raisePole(context, WEST_X, 2);
    raisePole(context, EAST_X, 2);
    context.setBlockState(standing(WEST_X, 1), ModBlocks.REDSTONE_CELL);
    RedstoneCellBlockEntity cell = context.getBlockEntity(standing(WEST_X, 1));
    cell.setCharge(500);
    context.setBlockState(standing(WEST_X, 2), floorSwitch(true));
    context.setBlockState(standing(WEST_X, 3), ModBlocks.LIGHT_STRIP);
    context.setBlockState(standing(EAST_X, 1), ModBlocks.LIGHT_STRIP);
    layRun(context, WEST_X, 1, POLE_Z, Hue.RED);
    layRun(context, EAST_X, 1, POLE_Z, Hue.BLUE);
    ConnectedTestPlayer red = lineman(context, Hue.RED, 16);
    ConnectedTestPlayer blue = lineman(context, Hue.BLUE, 16);
    string(context, red, WEST_X, EAST_X);
    string(context, blue, WEST_X, EAST_X);
    context.waitAndRun(
        SETTLE_TICKS * 4L,
        () -> {
          context.assertTrue(spansAt(context, top(WEST_X)).size() == 2, "both colours strung");
          context.expectBlockProperty(standing(WEST_X, 3), AbstractLampBlock.LEVEL, 15);
          context.expectBlockProperty(standing(EAST_X, 1), AbstractLampBlock.LEVEL, 0);
          context.assertFalse(
              WireEngine.of(context.getWorld())
                  .power()
                  .hasPower(context.getAbsolutePos(standing(EAST_X, 1))),
              "the blue lamp must not share the red line's power");
          red.leave();
          blue.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void twoPlayersFinishingOneSpanTogetherPayOnce(TestContext context) {
    raisePole(context, WEST_X, 2);
    raisePole(context, EAST_X, 2);
    ConnectedTestPlayer first = lineman(context, Hue.PLAIN, 16);
    ConnectedTestPlayer second = lineman(context, Hue.PLAIN, 16);
    use(context, first, top(WEST_X));
    use(context, second, top(WEST_X));
    use(context, first, top(EAST_X));
    use(context, second, top(EAST_X));
    int paid = 32 - wireHeld(first, Hue.PLAIN) - wireHeld(second, Hue.PLAIN);
    List<WireSpan> spans = spansAt(context, top(WEST_X));
    context.assertTrue(spans.size() == 1, "one span, not " + spans.size());
    context.assertTrue(
        paid == spans.getFirst().wire() && paid == 2, "one payment of 2 wire, not " + paid);
    first.leave();
    second.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void removingASpanGivesItsWireBack(TestContext context) {
    raisePole(context, WEST_X, 2);
    raisePole(context, EAST_X, 2);
    ConnectedTestPlayer player = lineman(context, Hue.PLAIN, 16);
    string(context, player, WEST_X, EAST_X);
    context.assertTrue(wireHeld(player, Hue.PLAIN) == 14, "stringing must cost 2 wire");
    player.player().setSneaking(true);
    use(context, player, top(EAST_X));
    context.assertTrue(spansAt(context, top(WEST_X)).isEmpty(), "the span must be gone");
    context.assertTrue(wireHeld(player, Hue.PLAIN) == 16, "all wire must come back");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH, tickLimit = 40)
  public void breakingAPoleDropsItsSpansWire(TestContext context) {
    raisePole(context, WEST_X, 2);
    raisePole(context, EAST_X, 2);
    ConnectedTestPlayer player = lineman(context, Hue.PLAIN, 16);
    string(context, player, WEST_X, EAST_X);
    context.setBlockState(standing(WEST_X, POLE_Z), Blocks.AIR);
    context.waitAndRun(
        2,
        () -> {
          context.assertTrue(spansAt(context, top(EAST_X)).isEmpty(), "the span must be gone");
          int dropped = droppedWire(context, standing(WEST_X, POLE_Z), Hue.PLAIN);
          context.assertTrue(dropped == 2, "the span's 2 wire must drop, not " + dropped);
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void buildingAPoleTallerMovesItsSpansUp(TestContext context) {
    raisePole(context, WEST_X, 2);
    raisePole(context, EAST_X, 2);
    ConnectedTestPlayer player = lineman(context, Hue.PLAIN, 16);
    string(context, player, WEST_X, EAST_X);
    context.setBlockState(top(WEST_X).up(), ModBlocks.OAK_TELEPHONE_POLE);
    context.assertTrue(spansAt(context, top(WEST_X)).isEmpty(), "the old top holds no span");
    context.assertTrue(
        spansAt(context, top(WEST_X).up()).size() == 1, "the span must move to the new top");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void joiningTwoColumnsDropsTheWireOfSpansThatWouldClash(TestContext context) {
    raisePole(context, WEST_X, 1);
    context.setBlockState(standing(WEST_X, POLE_Z).up(2), ModBlocks.OAK_TELEPHONE_POLE);
    raisePole(context, EAST_X, 3);
    ConnectedTestPlayer player = lineman(context, Hue.PLAIN, 32);
    use(context, player, standing(WEST_X, POLE_Z));
    use(context, player, top(EAST_X).up());
    use(context, player, standing(WEST_X, POLE_Z).up(2));
    use(context, player, top(EAST_X).up());
    int strung = spansAt(context, top(EAST_X).up()).size();
    context.assertTrue(strung == 2, "both columns hold a plain span, not " + strung);
    context.setBlockState(top(WEST_X), ModBlocks.OAK_TELEPHONE_POLE);
    context.waitAndRun(
        2,
        () -> {
          int spent = 32 - wireHeld(player, Hue.PLAIN);
          List<WireSpan> left = spansAt(context, top(EAST_X).up());
          context.assertTrue(left.size() == 1, "the clashing span must go, not " + left.size());
          int kept = left.stream().mapToInt(WireSpan::wire).sum();
          int dropped = droppedWire(context, standing(WEST_X, POLE_Z).up(2), Hue.PLAIN);
          context.assertTrue(
              kept + dropped == spent, "wire spent " + spent + " must equal kept plus dropped");
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void lightningOnAPolesRodFillsACellAlongItsSpanOnce(TestContext context) {
    raisePole(context, WEST_X, 2);
    raisePole(context, EAST_X, 2);
    context.setBlockState(standing(EAST_X, 2), ModBlocks.REDSTONE_CELL);
    layRun(context, EAST_X, 2, POLE_Z, Hue.PLAIN);
    ConnectedTestPlayer player = lineman(context, Hue.PLAIN, 16);
    string(context, player, WEST_X, EAST_X);
    context.setBlockState(top(WEST_X).up(), Blocks.LIGHTNING_ROD);
    RedstoneCellBlockEntity cell = context.getBlockEntity(standing(EAST_X, 2));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.spawnEntity(EntityType.LIGHTNING_BOLT, top(WEST_X).up(2));
          context.waitAndRun(
              LIGHTNING_TICKS,
              () -> {
                context.assertTrue(
                    cell.charge() == Hardwired.SERVER_CONFIG.power().lightningCharge(),
                    "one strike must fill once, got " + cell.charge());
                player.leave();
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH)
  public void honeycombOnAPoleWaxesItsCopperSpans(TestContext context) {
    raisePole(context, WEST_X, 2);
    raisePole(context, EAST_X, 2);
    ConnectedTestPlayer player = lineman(context, Hue.PLAIN, 16);
    string(context, player, WEST_X, EAST_X);
    player.hold(new ItemStack(Items.HONEYCOMB, 2), ItemStack.EMPTY);
    context.assertTrue(
        use(context, player, standing(EAST_X, POLE_Z)).isAccepted(), "honeycomb must wax the span");
    context.assertTrue(spansAt(context, top(WEST_X)).getFirst().waxed(), "the span is waxed");
    context.assertTrue(player.player().getMainHandStack().getCount() == 1, "one honeycomb used");
    context.assertFalse(
        use(context, player, top(EAST_X)).isAccepted(), "a waxed span takes no more wax");
    player.leave();
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void aSealOnTheLineCoversItsSpansAndKeepsStrangersOut(TestContext context) {
    raisePole(context, WEST_X, 2);
    raisePole(context, EAST_X, 2);
    layRun(context, WEST_X, 1, POLE_Z, Hue.PLAIN);
    ConnectedTestPlayer owner = lineman(context, Hue.PLAIN, 16);
    ConnectedTestPlayer stranger = lineman(context, Hue.PLAIN, 16);
    string(context, owner, WEST_X, EAST_X);
    WireEngine engine = WireEngine.of(context.getWorld());
    WireCell floor =
        new WireCell(
            new WireFace(
                context.getAbsolutePos(new BlockPos(WEST_X, FLOOR_Y, POLE_Z)), Direction.UP),
            Hue.PLAIN);
    engine.reown(engine.grid().networkOf(floor), owner.id());
    WireSpan sealed = spansAt(context, top(WEST_X)).getFirst();
    context.assertTrue(owner.id().equals(sealed.owner()), "the seal must cover the span");
    stranger.hold(new ItemStack(Items.HONEYCOMB), ItemStack.EMPTY);
    context.assertFalse(
        use(context, stranger, top(WEST_X)).isAccepted(), "a stranger may not wax a sealed span");
    stranger.hold(new ItemStack(ModItems.wire(Hue.PLAIN), 16), ItemStack.EMPTY);
    stranger.player().setSneaking(true);
    use(context, stranger, top(WEST_X));
    context.assertTrue(
        spansAt(context, top(WEST_X)).size() == 1, "a stranger may not take down a sealed span");
    context.assertTrue(wireHeld(stranger, Hue.PLAIN) == 16, "a stranger gains no wire");
    owner.hold(new ItemStack(Items.HONEYCOMB), ItemStack.EMPTY);
    context.assertTrue(use(context, owner, top(WEST_X)).isAccepted(), "the owner may wax it");
    owner.leave();
    stranger.leave();
    context.complete();
  }
}
