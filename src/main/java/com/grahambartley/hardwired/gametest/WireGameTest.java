package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.block.AbstractLampBlock;
import com.grahambartley.hardwired.block.JunctionBlock;
import com.grahambartley.hardwired.block.JunctionRule;
import com.grahambartley.hardwired.block.LampBlockEntity;
import com.grahambartley.hardwired.block.MountedDeviceBlock;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.registry.ModItems;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.LampLook;
import com.grahambartley.hardwired.wire.VanillaPower;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import com.grahambartley.hardwired.wire.WireGridNbt;
import com.grahambartley.hardwired.wire.WireLaying;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class WireGameTest implements FabricGameTest {

  private static final String BENCH = "hardwired:bench";
  private static final int FLOOR_Y = -1;
  private static final int SETTLE_TICKS = 4;

  private static WireEngine engine(TestContext context) {
    return WireEngine.of(context.getWorld());
  }

  private static WireCell floorCell(TestContext context, int x, int z, Hue hue) {
    return new WireCell(
        new WireFace(context.getAbsolutePos(new BlockPos(x, FLOOR_Y, z)), Direction.UP), hue);
  }

  private static void layRun(TestContext context, int fromX, int toX, int z, Hue hue) {
    for (int x = fromX; x <= toX; x++) {
      engine(context).lay(floorCell(context, x, z, hue));
    }
  }

  private static BlockState floorSwitch(boolean on) {
    return ModBlocks.WALL_SWITCH
        .getDefaultState()
        .with(WallMountedBlock.FACE, BlockFace.FLOOR)
        .with(MountedDeviceBlock.POWERED, on);
  }

  private static int droppedWire(TestContext context, BlockPos near, Hue hue) {
    return context
        .getWorld()
        .getEntitiesByClass(
            ItemEntity.class,
            new net.minecraft.util.math.Box(context.getAbsolutePos(near)).expand(2),
            item -> item.getStack().isOf(ModItems.wire(hue)))
        .stream()
        .mapToInt(item -> item.getStack().getCount())
        .sum();
  }

  private static BlockPos standing(int x, int z) {
    return new BlockPos(x, FLOOR_Y + 1, z);
  }

  @GameTest(templateName = BENCH)
  public void aSwitchLightsALampAcrossWire(TestContext context) {
    context.setBlockState(standing(1, 1), floorSwitch(false));
    context.setBlockState(standing(6, 1), ModBlocks.COPPER_LAMP);
    layRun(context, 1, 6, 1, Hue.PLAIN);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.setBlockState(standing(1, 1), floorSwitch(true));
          context.waitAndRun(
              SETTLE_TICKS,
              () -> {
                context.expectBlockProperty(standing(6, 1), AbstractLampBlock.LEVEL, 15);
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH)
  public void wireOfAnotherColourDoesNotCarryTheSignal(TestContext context) {
    context.setBlockState(standing(1, 1), floorSwitch(true));
    context.setBlockState(standing(4, 1), ModBlocks.COPPER_LAMP);
    layRun(context, 1, 2, 1, Hue.RED);
    layRun(context, 3, 4, 1, Hue.BLUE);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.expectBlockProperty(standing(4, 1), AbstractLampBlock.LEVEL, 0);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void wirePowersAVanillaLampFully(TestContext context) {
    context.setBlockState(standing(1, 1), floorSwitch(true));
    context.setBlockState(standing(5, 1), Blocks.REDSTONE_LAMP);
    layRun(context, 1, 5, 1, Hue.PLAIN);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.expectBlockProperty(standing(5, 1), Properties.LIT, true);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void vanillaPowerTurnsANetworkOn(TestContext context) {
    context.setBlockState(standing(1, 1), Blocks.REDSTONE_BLOCK);
    context.setBlockState(standing(5, 1), ModBlocks.COPPER_LAMP);
    layRun(context, 1, 5, 1, Hue.PLAIN);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.expectBlockProperty(standing(5, 1), AbstractLampBlock.LEVEL, 15);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void dustFedByWireDoesNotHoldTheNetworkOn(TestContext context) {
    context.setBlockState(standing(1, 1), floorSwitch(true));
    context.setBlockState(standing(5, 1), Blocks.REDSTONE_WIRE);
    context.setBlockState(standing(6, 1), Blocks.REDSTONE_WIRE);
    layRun(context, 1, 5, 1, Hue.PLAIN);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.setBlockState(standing(1, 1), floorSwitch(false));
          context.waitAndRun(
              SETTLE_TICKS,
              () -> {
                context.assertFalse(
                    engine(context).grid().networkOf(floorCell(context, 3, 1, Hue.PLAIN)).isOn(),
                    "wire must not hold itself on through dust");
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH)
  public void aPoweredNetworkDoesNotQuasiPowerAPiston(TestContext context) {
    context.setBlockState(standing(4, 1), Blocks.PISTON);
    WireEngine engine = engine(context);
    for (int x = 4; x <= 6; x++) {
      BlockPos above = standing(x, 1).up();
      context.setBlockState(above, Blocks.STONE);
      engine.lay(
          new WireCell(new WireFace(context.getAbsolutePos(above), Direction.UP), Hue.PLAIN));
    }
    context.setBlockState(standing(6, 1).up(2), floorSwitch(true));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.assertTrue(
              engine.isPowered(context.getAbsolutePos(standing(4, 1).up())),
              "the block above the piston must be powered");
          context.expectBlockProperty(standing(4, 1), Properties.EXTENDED, false);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void breakingTheHostDropsItsWire(TestContext context) {
    BlockPos host = standing(3, 3);
    context.setBlockState(host, Blocks.STONE);
    WireCell cell =
        new WireCell(new WireFace(context.getAbsolutePos(host), Direction.NORTH), Hue.ORANGE);
    engine(context).lay(cell);
    context.setBlockState(host, Blocks.AIR);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.assertFalse(engine(context).grid().contains(cell), "wire must leave the grid");
          int dropped = droppedWire(context, host, Hue.ORANGE);
          context.assertTrue(dropped == 1, "exactly one item must drop, got " + dropped);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void aFullFaceSwapKeepsTheWire(TestContext context) {
    BlockPos host = standing(3, 3);
    context.setBlockState(host, Blocks.STONE);
    WireCell cell =
        new WireCell(new WireFace(context.getAbsolutePos(host), Direction.NORTH), Hue.PLAIN);
    engine(context).lay(cell);
    context.setBlockState(host, Blocks.COBBLESTONE);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.assertTrue(engine(context).grid().contains(cell), "wire must stay");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void aPistonCarriesWireWithItsBlock(TestContext context) {
    BlockPos host = standing(3, 3);
    context.setBlockState(
        standing(2, 3), Blocks.PISTON.getDefaultState().with(Properties.FACING, Direction.EAST));
    context.setBlockState(host, Blocks.STONE);
    WireCell cell =
        new WireCell(new WireFace(context.getAbsolutePos(host), Direction.UP), Hue.PLAIN);
    engine(context).lay(cell);
    context.setBlockState(standing(2, 3).west(), Blocks.REDSTONE_BLOCK);
    context.waitAndRun(
        SETTLE_TICKS + 4,
        () -> {
          WireCell moved =
              new WireCell(
                  new WireFace(context.getAbsolutePos(host.east()), Direction.UP), Hue.PLAIN);
          context.assertTrue(engine(context).grid().contains(moved), "wire must arrive");
          context.assertFalse(engine(context).grid().contains(cell), "wire must leave");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void aPistonBreakingABlockDropsItsWire(TestContext context) {
    BlockPos melon = standing(4, 3);
    context.setBlockState(
        standing(2, 3), Blocks.PISTON.getDefaultState().with(Properties.FACING, Direction.EAST));
    context.setBlockState(standing(3, 3), Blocks.STONE);
    context.setBlockState(melon, Blocks.MELON);
    WireCell cell =
        new WireCell(new WireFace(context.getAbsolutePos(melon), Direction.UP), Hue.ORANGE);
    engine(context).lay(cell);
    context.setBlockState(standing(2, 3).west(), Blocks.REDSTONE_BLOCK);
    context.waitAndRun(
        SETTLE_TICKS + 4,
        () -> {
          context.assertFalse(engine(context).grid().contains(cell), "wire must drop");
          context.assertTrue(
              droppedWire(context, melon, Hue.ORANGE) == 1, "exactly one item must drop");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void extendingALiveNetworkLightsTheNewLamp(TestContext context) {
    context.setBlockState(standing(1, 1), floorSwitch(true));
    context.setBlockState(standing(6, 1), ModBlocks.COPPER_LAMP);
    layRun(context, 1, 3, 1, Hue.PLAIN);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          layRun(context, 4, 6, 1, Hue.PLAIN);
          context.waitAndRun(
              SETTLE_TICKS,
              () -> {
                context.expectBlockProperty(standing(6, 1), AbstractLampBlock.LEVEL, 15);
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH)
  public void cuttingALiveWireTurnsItsOutputOff(TestContext context) {
    context.setBlockState(standing(1, 1), floorSwitch(true));
    context.setBlockState(standing(5, 1), Blocks.REDSTONE_LAMP);
    layRun(context, 1, 5, 1, Hue.PLAIN);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.expectBlockProperty(standing(5, 1), Properties.LIT, true);
          for (int x = 3; x <= 5; x++) {
            engine(context).cut(floorCell(context, x, 1, Hue.PLAIN));
          }
          context.waitAndRun(
              SETTLE_TICKS + 4,
              () -> {
                context.expectBlockProperty(standing(5, 1), Properties.LIT, false);
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH)
  public void cuttingTheInputOffARunTurnsItOff(TestContext context) {
    context.setBlockState(standing(1, 1), floorSwitch(true));
    context.setBlockState(standing(5, 1), ModBlocks.COPPER_LAMP);
    layRun(context, 1, 5, 1, Hue.PLAIN);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.expectBlockProperty(standing(5, 1), AbstractLampBlock.LEVEL, 15);
          engine(context).cut(floorCell(context, 2, 1, Hue.PLAIN));
          context.waitAndRun(
              SETTLE_TICKS + 8,
              () -> {
                context.expectBlockProperty(standing(5, 1), AbstractLampBlock.LEVEL, 0);
                context.assertTrue(
                    engine(context)
                        .grid()
                        .networkOf(floorCell(context, 4, 1, Hue.PLAIN))
                        .rememberedInputs()
                        .isEmpty(),
                    "the cut part must forget the switch");
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH, batchId = "chunk-edge")
  public void powerIsNotReadWithinTwoBlocksOfAnUnloadedChunk(TestContext context) {
    BlockPos start = context.getAbsolutePos(standing(4, 4));
    int chunkX = start.getX() >> 4;
    while (context.getWorld().getChunkManager().isChunkLoaded(chunkX, start.getZ() >> 4)) {
      chunkX++;
    }
    int borderX = chunkX << 4;
    BlockPos clear = new BlockPos(borderX - 3, start.getY(), (start.getZ() & ~15) + 8);
    context.assertTrue(
        VanillaPower.isReadable(context.getWorld(), clear), "three blocks clear can be read");
    context.assertFalse(
        VanillaPower.isReadable(context.getWorld(), clear.east()),
        "the second read would cross into the unloaded chunk");
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void layingAndRemovingConservesItems(TestContext context) {
    ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
    player.getAbilities().creativeMode = false;
    player.refreshPositionAfterTeleport(
        net.minecraft.util.math.Vec3d.ofCenter(context.getAbsolutePos(standing(4, 4))));
    player.setStackInHand(Hand.MAIN_HAND, new ItemStack(ModItems.wire(Hue.PLAIN), 3));
    List<WireFace> faces =
        List.of(
            floorCell(context, 3, 4, Hue.PLAIN).face(),
            floorCell(context, 4, 4, Hue.PLAIN).face(),
            floorCell(context, 5, 4, Hue.PLAIN).face(),
            floorCell(context, 6, 4, Hue.PLAIN).face());
    WireLaying.lay(player, faces);
    context.assertTrue(
        player.getMainHandStack().isEmpty(), "three items lay three faces then stop");
    context.assertFalse(
        engine(context).grid().contains(floorCell(context, 6, 4, Hue.PLAIN)),
        "laying must refuse to continue when out of items");
    player.setStackInHand(Hand.MAIN_HAND, new ItemStack(ModItems.wire(Hue.PLAIN)));
    faces.subList(0, 3).forEach(face -> WireLaying.remove(player, face));
    context.assertTrue(
        player.getInventory().count(ModItems.wire(Hue.PLAIN)) == 4,
        "removing every face must return every item");
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void aPanelSetsEveryLampOnItsNetwork(TestContext context) {
    context.setBlockState(standing(1, 1), floorSwitch(true));
    context.setBlockState(standing(4, 1), ModBlocks.COPPER_LAMP);
    context.setBlockState(standing(6, 1), ModBlocks.COPPER_LAMP);
    BlockPos panelPos = standing(3, 1);
    context.setBlockState(
        panelPos,
        ModBlocks.CONTROL_PANEL.getDefaultState().with(WallMountedBlock.FACE, BlockFace.FLOOR));
    layRun(context, 1, 6, 1, Hue.PLAIN);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          LampLook look = new LampLook(Hue.CYAN, 9);
          engine(context).applyLook(context.getAbsolutePos(panelPos), look);
          context.waitAndRun(
              SETTLE_TICKS + 8,
              () -> {
                for (int x : new int[] {4, 6}) {
                  LampBlockEntity lamp = context.getBlockEntity(standing(x, 1));
                  context.assertTrue(lamp.color() == Hue.CYAN, "lamp colour");
                  context.expectBlockProperty(standing(x, 1), AbstractLampBlock.LEVEL, 9);
                }
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH)
  public void aJunctionNotInvertsItsInput(TestContext context) {
    context.setBlockState(standing(1, 1), floorSwitch(false));
    context.setBlockState(
        standing(3, 1),
        ModBlocks.JUNCTION
            .getDefaultState()
            .with(JunctionBlock.RULE, JunctionRule.NOT)
            .with(JunctionBlock.OUTPUT, Hue.BLUE));
    context.setBlockState(standing(5, 1), ModBlocks.COPPER_LAMP);
    layRun(context, 1, 3, 1, Hue.RED);
    layRun(context, 3, 5, 1, Hue.BLUE);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.expectBlockProperty(standing(5, 1), AbstractLampBlock.LEVEL, 15);
          context.setBlockState(standing(1, 1), floorSwitch(true));
          context.waitAndRun(
              SETTLE_TICKS + 8,
              () -> {
                context.expectBlockProperty(standing(5, 1), AbstractLampBlock.LEVEL, 0);
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 200)
  public void aRapidlyFlippingNetworkTrips(TestContext context) {
    layRun(context, 1, 3, 1, Hue.PLAIN);
    for (int tick = 2; tick < 40; tick++) {
      boolean on = tick % 2 == 0;
      context.runAtTick(tick, () -> context.setBlockState(standing(1, 1), floorSwitch(on)));
    }
    context.runAtTick(
        60,
        () -> {
          context.assertTrue(
              engine(context).grid().networkOf(floorCell(context, 2, 1, Hue.PLAIN)).isTripped(),
              "network must trip");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void savedWireReloadsItsNetworks(TestContext context) {
    layRun(context, 1, 4, 1, Hue.GREEN);
    var reloaded = WireGridNbt.read(WireGridNbt.write(engine(context).grid()));
    context.assertTrue(
        reloaded.networkOf(floorCell(context, 2, 1, Hue.GREEN)).size() == 4, "all four faces");
    context.complete();
  }
}
