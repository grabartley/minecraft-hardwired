package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.AbstractLampBlock;
import com.grahambartley.hardwired.block.ControlPanelBlockEntity;
import com.grahambartley.hardwired.block.DuskClock;
import com.grahambartley.hardwired.block.JunctionBlock;
import com.grahambartley.hardwired.block.JunctionRule;
import com.grahambartley.hardwired.block.LampBlockEntity;
import com.grahambartley.hardwired.block.MountedDeviceBlock;
import com.grahambartley.hardwired.block.StreetlightBlock;
import com.grahambartley.hardwired.block.SwitchBlock;
import com.grahambartley.hardwired.device.DeviceActions;
import com.grahambartley.hardwired.network.JunctionSetPayload;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.registry.ModComponents;
import com.grahambartley.hardwired.server.ServerConfigEditPermission;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.LampLook;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import com.grahambartley.hardwired.wire.WireLaying;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;

public class DeviceGameTest implements FabricGameTest {

  private static final String BENCH = "hardwired:bench";
  private static final int FLOOR_Y = -1;
  private static final int SETTLE_TICKS = 4;
  private static final int LAMP_SETTLE_TICKS = 12;

  private static WireEngine engine(TestContext context) {
    return WireEngine.of(context.getWorld());
  }

  private static BlockPos at(int x, int z) {
    return new BlockPos(x, FLOOR_Y + 1, z);
  }

  private static void run(TestContext context, int fromX, int toX, int z, Hue hue) {
    for (int x = fromX; x <= toX; x++) {
      engine(context)
          .lay(
              new WireCell(
                  new WireFace(context.getAbsolutePos(new BlockPos(x, FLOOR_Y, z)), Direction.UP),
                  hue));
    }
  }

  private static BlockState floor(net.minecraft.block.Block block) {
    return block.getDefaultState().with(WallMountedBlock.FACE, BlockFace.FLOOR);
  }

  private static ServerPlayerEntity playerAt(TestContext context, BlockPos pos) {
    ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
    player.refreshPositionAfterTeleport(Vec3d.ofCenter(context.getAbsolutePos(pos)));
    return player;
  }

  private static void useItem(
      TestContext context, ServerPlayerEntity player, Item item, BlockPos pos) {
    BlockPos absolute = context.getAbsolutePos(pos);
    context
        .getBlockState(pos)
        .onUseWithItem(
            new ItemStack(item),
            context.getWorld(),
            player,
            Hand.MAIN_HAND,
            new BlockHitResult(Vec3d.ofCenter(absolute), Direction.WEST, absolute, false));
  }

  @GameTest(templateName = BENCH)
  public void removingTheOnlyPanelReturnsLampsToFullBrightness(TestContext context) {
    context.setBlockState(
        at(1, 1), floor(ModBlocks.WALL_SWITCH).with(MountedDeviceBlock.POWERED, true));
    context.setBlockState(at(3, 1), floor(ModBlocks.CONTROL_PANEL));
    context.setBlockState(at(5, 1), ModBlocks.LIGHT_STRIP);
    run(context, 1, 5, 1, Hue.PLAIN);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          engine(context).applyLook(context.getAbsolutePos(at(3, 1)), new LampLook(Hue.RED, 5));
          context.waitAndRun(
              LAMP_SETTLE_TICKS,
              () -> {
                context.expectBlockProperty(at(5, 1), AbstractLampBlock.LEVEL, 5);
                context.setBlockState(at(3, 1), Blocks.AIR);
                context.waitAndRun(
                    LAMP_SETTLE_TICKS,
                    () -> {
                      context.expectBlockProperty(at(5, 1), AbstractLampBlock.LEVEL, 15);
                      context.complete();
                    });
              });
        });
  }

  @GameTest(templateName = BENCH)
  public void aSwitchRecallsItsScene(TestContext context) {
    BlockPos switchPos = at(1, 1);
    context.setBlockState(switchPos, floor(ModBlocks.WALL_SWITCH).with(SwitchBlock.SCENE, 1));
    context.setBlockState(at(3, 1), floor(ModBlocks.CONTROL_PANEL));
    context.setBlockState(at(5, 1), ModBlocks.LIGHT_STRIP);
    run(context, 1, 5, 1, Hue.PLAIN);
    ControlPanelBlockEntity panel = context.getBlockEntity(at(3, 1));
    panel.saveScene(0, "Dinner", new LampLook(Hue.BLUE, 6));
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          context.useBlock(switchPos);
          context.waitAndRun(
              LAMP_SETTLE_TICKS,
              () -> {
                context.expectBlockProperty(at(5, 1), AbstractLampBlock.LEVEL, 6);
                LampBlockEntity lamp = context.getBlockEntity(at(5, 1));
                context.assertTrue(lamp.color() == Hue.BLUE, "lamp must take the scene colour");
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH)
  public void aPushButtonLetsGo(TestContext context) {
    context.setBlockState(at(2, 2), floor(ModBlocks.PUSH_BUTTON));
    context.useBlock(at(2, 2));
    context.expectBlockProperty(at(2, 2), MountedDeviceBlock.POWERED, true);
    context.waitAndRun(
        Hardwired.SERVER_CONFIG.device().buttonHoldTicks() + 2,
        () -> {
          context.expectBlockProperty(at(2, 2), MountedDeviceBlock.POWERED, false);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH, tickLimit = 80)
  public void aMotionSensorNoticesACreature(TestContext context) {
    context.setBlockState(at(4, 4), floor(ModBlocks.MOTION_SENSOR));
    context.waitAndRun(
        15,
        () -> {
          context.expectBlockProperty(at(4, 4), MountedDeviceBlock.POWERED, false);
          context.spawnEntity(EntityType.PIG, at(5, 4));
          context.waitAndRun(
              15,
              () -> {
                context.expectBlockProperty(at(4, 4), MountedDeviceBlock.POWERED, true);
                context.complete();
              });
        });
  }

  @GameTest(templateName = BENCH, batchId = "dusk", tickLimit = 60)
  public void aDuskSensorFollowsTheClock(TestContext context) {
    context.setTime(DuskClock.DUSK + 100);
    context.getWorld().getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, null);
    context.setBlockState(at(4, 4), floor(ModBlocks.DUSK_SENSOR));
    context.waitAndRun(
        15,
        () -> {
          context.expectBlockProperty(at(4, 4), MountedDeviceBlock.POWERED, true);
          context.getWorld().getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(true, null);
          context.complete();
        });
  }

  private static void placeStreetlight(TestContext context, BlockPos base) {
    context.setBlockState(base, ModBlocks.STREETLIGHT);
    context.setBlockState(
        base.up(), ModBlocks.STREETLIGHT.getDefaultState().with(StreetlightBlock.PART, 1));
    context.setBlockState(
        base.up(StreetlightBlock.HEAD),
        ModBlocks.STREETLIGHT.getDefaultState().with(StreetlightBlock.PART, StreetlightBlock.HEAD));
  }

  @GameTest(templateName = BENCH)
  public void wireOnAStreetlightPostLightsItsHead(TestContext context) {
    BlockPos base = at(4, 2);
    placeStreetlight(context, base);
    context.setBlockState(at(2, 2).up(), Blocks.STONE);
    context.setBlockState(at(3, 2).up(), Blocks.STONE);
    context.setBlockState(
        at(2, 2).up(2), floor(ModBlocks.WALL_SWITCH).with(MountedDeviceBlock.POWERED, true));
    for (int x = 2; x <= 3; x++) {
      engine(context)
          .lay(
              new WireCell(
                  new WireFace(context.getAbsolutePos(at(x, 2).up()), Direction.UP), Hue.PLAIN));
    }
    context.waitAndRun(
        LAMP_SETTLE_TICKS,
        () -> {
          context.expectBlockProperty(base.up(2), AbstractLampBlock.LEVEL, 15);
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void aDyedStreetlightDropsOnceWithItsColour(TestContext context) {
    BlockPos base = at(4, 2);
    placeStreetlight(context, base);
    LampBlockEntity lamp = context.getBlockEntity(base);
    lamp.setLook(Hue.LIME, 15);
    context.getWorld().breakBlock(context.getAbsolutePos(base.up()), true);
    context.waitAndRun(
        SETTLE_TICKS,
        () -> {
          List<ItemEntity> drops =
              context
                  .getWorld()
                  .getEntitiesByClass(
                      ItemEntity.class,
                      new Box(context.getAbsolutePos(base)).expand(3),
                      item -> item.getStack().isOf(ModBlocks.STREETLIGHT.asItem()));
          context.assertTrue(drops.size() == 1, "one item must drop, got " + drops.size());
          context.assertTrue(
              drops.get(0).getStack().get(ModComponents.LAMP_COLOR) == Hue.LIME,
              "the drop must keep its colour");
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void honeycombWaxesALampAndAnAxeScrapesIt(TestContext context) {
    context.setBlockState(at(3, 3), ModBlocks.LIGHT_STRIP);
    ServerPlayerEntity player = playerAt(context, at(2, 3));
    useItem(context, player, Items.HONEYCOMB, at(3, 3));
    context.expectBlockProperty(at(3, 3), AbstractLampBlock.WAXED, true);
    useItem(context, player, Items.IRON_AXE, at(3, 3));
    context.expectBlockProperty(at(3, 3), AbstractLampBlock.WAXED, false);
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void aDistantJunctionRequestLoadsNoChunk(TestContext context) {
    ServerPlayerEntity player = playerAt(context, at(2, 2));
    BlockPos far = context.getAbsolutePos(at(2, 2)).add(200_000, 0, 200_000);
    DeviceActions.junction(player, new JunctionSetPayload(far, 0, 0));
    context.assertFalse(
        context.getWorld().getChunkManager().isChunkLoaded(far.getX() >> 4, far.getZ() >> 4),
        "a far request must not load the chunk at " + new ChunkPos(far));
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void junctionRulesChangeWithinReach(TestContext context) {
    context.setBlockState(at(3, 3), ModBlocks.JUNCTION);
    ServerPlayerEntity player = playerAt(context, at(2, 3));
    DeviceActions.junction(
        player,
        new JunctionSetPayload(
            context.getAbsolutePos(at(3, 3)), JunctionRule.AND.ordinal(), Hue.RED.ordinal()));
    context.expectBlockProperty(at(3, 3), JunctionBlock.RULE, JunctionRule.AND);
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void wireRefusesAPlayerWhoCannotBuild(TestContext context) {
    ServerPlayerEntity player = playerAt(context, at(3, 3));
    player.getAbilities().allowModifyWorld = false;
    player.setStackInHand(
        Hand.MAIN_HAND,
        com.grahambartley.hardwired.registry.ModItems.wire(Hue.PLAIN).getDefaultStack());
    WireFace face = new WireFace(context.getAbsolutePos(new BlockPos(3, FLOOR_Y, 3)), Direction.UP);
    WireLaying.lay(player, List.of(face));
    context.assertFalse(
        engine(context).grid().contains(new WireCell(face, Hue.PLAIN)), "wire must be refused");
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void onlyOperatorsEditSettings(TestContext context) {
    ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
    context.assertTrue(
        ServerConfigEditPermission.canEdit(player, context.getWorld().getServer())
            == player.hasPermissionLevel(2),
        "edit rights follow operator level on a dedicated server");
    context.complete();
  }

  @GameTest(templateName = BENCH)
  public void theConfigCommandChangesASetting(TestContext context) {
    var server = context.getWorld().getServer();
    int before = Hardwired.SERVER_CONFIG.device().buttonHoldTicks();
    server
        .getCommandManager()
        .executeWithPrefix(server.getCommandSource(), "hardwired config buttonholdticks 33");
    context.assertTrue(
        Hardwired.SERVER_CONFIG.device().buttonHoldTicks() == 33, "the command must apply");
    server
        .getCommandManager()
        .executeWithPrefix(server.getCommandSource(), "hardwired config buttonholdticks " + before);
    context.complete();
  }
}
