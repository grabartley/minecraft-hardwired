package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.MountedDeviceBlock;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.config.WireSettings;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.registry.ModItems;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.test.AfterBatch;
import net.minecraft.test.BeforeBatch;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class WireEdgeGameTest implements FabricGameTest {

  private static final String BENCH = "hardwired:bench";
  private static final int FLOOR_Y = -1;
  private static final int SETTLE_TICKS = 4;
  private static final int NO_UPDATES = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
  private static final int LIMIT = WireSettings.NETWORK_SIZE_MIN;
  private static final int CLIMB = LIMIT / 2 - 1;
  private static final int VIEW_TICKS = 6;

  private final HardwiredConfig savedConfig = Hardwired.SERVER_CONFIG;

  private static WireEngine engine(TestContext context) {
    return WireEngine.of(context.getWorld());
  }

  private static WireCell floorCell(TestContext context, int x, int z) {
    return new WireCell(
        new WireFace(context.getAbsolutePos(new BlockPos(x, FLOOR_Y, z)), Direction.UP), Hue.PLAIN);
  }

  private static BlockPos standing(int x, int z) {
    return new BlockPos(x, FLOOR_Y + 1, z);
  }

  private static BlockState floorSwitch(boolean on) {
    return ModBlocks.WALL_SWITCH
        .getDefaultState()
        .with(WallMountedBlock.FACE, BlockFace.FLOOR)
        .with(MountedDeviceBlock.POWERED, on);
  }

  private static int firstUnloadedChunkEast(TestContext context, BlockPos start) {
    int chunkX = start.getX() >> 4;
    while (context.getWorld().getChunkManager().isChunkLoaded(chunkX, start.getZ() >> 4)) {
      chunkX++;
    }
    return chunkX;
  }

  @GameTest(templateName = BENCH)
  public void aBlockBesideOneToppedByFedDustTurnsItsNetworkOn(TestContext context) {
    BlockPos touched = standing(2, 1);
    BlockPos besideIt = standing(2, 2);
    context.setBlockState(touched, Blocks.STONE);
    context.setBlockState(besideIt, Blocks.STONE);
    context.setBlockState(besideIt.up(), Blocks.REDSTONE_WIRE);
    context.setBlockState(besideIt.up().east(), Blocks.REDSTONE_BLOCK);
    for (int x = 0; x <= 4; x++) {
      engine(context).lay(floorCell(context, x, 1));
    }
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.assertTrue(
              context.getWorld().isReceivingRedstonePower(context.getAbsolutePos(touched)),
              "vanilla must call the touched block powered");
          context.assertTrue(
              engine(context).grid().networkOf(floorCell(context, 1, 1)).isOn(),
              "the network must read that power");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = "chunk-edge")
  public void pushingALampAtTheLoadBorderLoadsNothing(TestContext context) {
    ServerWorld world = context.getWorld();
    BlockPos start = context.getAbsolutePos(standing(4, 4));
    int unloaded = firstUnloadedChunkEast(context, start);
    BlockPos lamp = new BlockPos((unloaded << 4) - 1, start.getY(), start.getZ());
    BlockState before = world.getBlockState(lamp);
    world.setBlockState(lamp, Blocks.REDSTONE_LAMP.getDefaultState(), NO_UPDATES);
    WireCell cell = new WireCell(new WireFace(lamp.down(), Direction.UP), Hue.PLAIN);
    engine(context).lay(cell);
    engine(context).tick();
    boolean loaded = world.getChunkManager().isChunkLoaded(unloaded, start.getZ() >> 4);
    engine(context).cut(cell);
    world.setBlockState(lamp, before, NO_UPDATES);
    context.assertFalse(loaded, "pushing a wire update must not load the next chunk");
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void dustFedOnlyByWireThroughASolidBlockDoesNotHoldTheNetworkOn(TestContext context) {
    context.setBlockState(standing(1, 1), floorSwitch(true));
    context.setBlockState(standing(3, 1), Blocks.STONE);
    context.setBlockState(standing(3, 2), Blocks.STONE);
    context.setBlockState(standing(5, 1), Blocks.REDSTONE_WIRE);
    context.setBlockState(standing(5, 2), Blocks.REDSTONE_WIRE);
    context.setBlockState(standing(4, 2), Blocks.REDSTONE_WIRE);
    for (int x = 1; x <= 5; x++) {
      engine(context).lay(floorCell(context, x, 1));
    }
    context.waitAndRun(
        SETTLE_TICKS, () -> context.setBlockState(standing(1, 1), floorSwitch(false)));
    context.waitAndRun(
        SETTLE_TICKS * 2,
        () -> {
          context.assertFalse(
              engine(context).grid().networkOf(floorCell(context, 3, 1)).isOn(),
              "wire must not hold itself on through dust and a solid block");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, batchId = "wire-limit")
  public void aPistonDropsWireThatWouldPassTheLimitAndLosesNone(TestContext context) {
    BlockPos host = standing(3, 3);
    context.setBlockState(
        standing(2, 3), Blocks.PISTON.getDefaultState().with(Properties.FACING, Direction.EAST));
    context.setBlockState(host, Blocks.STONE);
    WireCell carried =
        new WireCell(new WireFace(context.getAbsolutePos(host), Direction.UP), Hue.PLAIN);
    WireCell destination =
        new WireCell(new WireFace(context.getAbsolutePos(host.east()), Direction.UP), Hue.PLAIN);
    BlockPos northOfHost = context.getAbsolutePos(host.north());
    engine(context).lay(carried);
    engine(context).lay(new WireCell(new WireFace(northOfHost, Direction.UP), Hue.PLAIN));
    climb(context, new WireFace(northOfHost.east(), Direction.UP), Direction.NORTH);
    BlockPos eastOfDestination = context.getAbsolutePos(host.east(2));
    climb(context, new WireFace(eastOfDestination, Direction.UP), Direction.EAST);
    WireCell north = new WireCell(new WireFace(northOfHost, Direction.UP), Hue.PLAIN);
    WireCell east = new WireCell(new WireFace(eastOfDestination, Direction.UP), Hue.PLAIN);
    int total =
        engine(context).grid().networkOf(north).size()
            + engine(context).grid().networkOf(east).size();
    context.assertTrue(total > LIMIT, "the bridge must pass the limit: " + total);
    context.setBlockState(standing(2, 3).west(), Blocks.REDSTONE_BLOCK);
    context.waitAndRun(
        SETTLE_TICKS + 4,
        () -> {
          context.assertFalse(engine(context).grid().contains(carried), "wire must leave");
          context.assertFalse(
              engine(context).grid().contains(destination), "wire past the limit must not land");
          int left =
              engine(context).grid().networkOf(north).size()
                  + engine(context).grid().networkOf(east).size();
          context.assertTrue(droppedWire(context, host.east()) == 1, "one item must drop");
          context.assertTrue(left + 1 == total, "no wire may be lost: " + left);
          context.complete();
        });
  }

  private static void climb(TestContext context, WireFace start, Direction wall) {
    engine(context).lay(new WireCell(start, Hue.PLAIN));
    for (int rise = 0; rise < CLIMB; rise++) {
      engine(context).lay(new WireCell(new WireFace(start.pos().up(rise), wall), Hue.PLAIN));
    }
  }

  private static int droppedWire(TestContext context, BlockPos near) {
    return context
        .getWorld()
        .getEntitiesByClass(
            ItemEntity.class,
            new Box(context.getAbsolutePos(near)).expand(3),
            item -> item.getStack().isOf(ModItems.wire(Hue.PLAIN)))
        .stream()
        .mapToInt(item -> item.getStack().getCount())
        .sum();
  }

  @BeforeBatch(batchId = "wire-limit")
  public void useTheSmallestLimit(ServerWorld world) {
    Hardwired.SERVER_CONFIG = savedConfig.withWire(savedConfig.wire().withMaxNetworkSize(LIMIT));
  }

  @AfterBatch(batchId = "wire-limit")
  public void restoreTheLimit(ServerWorld world) {
    Hardwired.SERVER_CONFIG = savedConfig;
  }

  @GameTest(templateName = BENCH, batchId = "wire-view-range", tickLimit = 60)
  public void wirePastTheRangeIsNotSentAndFollowsTheHolderWithinAChunk(TestContext context) {
    int range = Hardwired.SERVER_CONFIG.wire().viewRangeBlocks();
    BlockPos feet = standing(1, 1);
    WireFace near = new WireFace(context.getAbsolutePos(new BlockPos(1, FLOOR_Y, 1)), Direction.UP);
    WireFace far = new WireFace(near.pos().up(range + 8), Direction.UP);
    engine(context).lay(new WireCell(near, Hue.PLAIN));
    engine(context).lay(new WireCell(far, Hue.PLAIN));
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, feet);
    player.hold(new ItemStack(ModItems.wire(Hue.PLAIN)), ItemStack.EMPTY);
    context.waitAndRun(
        VIEW_TICKS,
        () -> {
          context.assertTrue(player.shownCellsAt(near.pos()).size() == 1, "near wire shows");
          context.assertTrue(player.receivedCellsAt(far.pos()).isEmpty(), "far wire stays unsent");
          Vec3d above = Vec3d.ofBottomCenter(far.pos());
          player.player().refreshPositionAndAngles(above.x, above.y, above.z, 0.0F, 0.0F);
        });
    context.waitAndRun(
        VIEW_TICKS * 2,
        () -> {
          context.assertTrue(player.shownCellsAt(far.pos()).size() == 1, "far wire now shows");
          context.assertTrue(player.shownCellsAt(near.pos()).isEmpty(), "left wire is cleared");
          player.leave();
          context.complete();
        });
  }
}
