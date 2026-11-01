package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.MountedDeviceBlock;
import com.grahambartley.hardwired.block.RedstoneCellBlockEntity;
import com.grahambartley.hardwired.block.SourceBlockEntity;
import com.grahambartley.hardwired.device.DeviceActions;
import com.grahambartley.hardwired.network.PanelActionPayload;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.LampLook;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AdvancementTriggerGameTest implements FabricGameTest {

  private static final String BENCH = "hardwired:bench";
  private static final int FLOOR_Y = -1;
  private static final int SETTLE = 12;
  private static final BlockPos SOURCE = at(1, 1);
  private static final BlockPos INPUT = at(3, 1);
  private static final BlockPos LAMP = at(5, 1);
  private static final BlockPos NEARBY = at(3, 4);
  private static final Identifier LET_THERE_BE_LIGHT = id("let_there_be_light");
  private static final Identifier MOOD_LIGHTING = id("mood_lighting");
  private static final Identifier CHARGED_UP = id("charged_up");
  private static final Identifier LIGHTS_OUT = id("lights_out");

  private static Identifier id(String path) {
    return Identifier.of(Hardwired.MOD_ID, path);
  }

  private static BlockPos at(int x, int z) {
    return new BlockPos(x, FLOOR_Y + 1, z);
  }

  private static void layRun(TestContext context) {
    WireEngine engine = WireEngine.of(context.getWorld());
    for (int x = 1; x <= 5; x++) {
      engine.lay(
          new WireCell(
              new WireFace(context.getAbsolutePos(new BlockPos(x, FLOOR_Y, 1)), Direction.UP),
              Hue.PLAIN));
    }
  }

  private static BlockState floorSwitch() {
    return ModBlocks.WALL_SWITCH
        .getDefaultState()
        .with(WallMountedBlock.FACE, BlockFace.FLOOR)
        .with(MountedDeviceBlock.POWERED, true);
  }

  private static void poweredLampRun(TestContext context, BlockState input) {
    context.setBlockState(SOURCE, ModBlocks.LODESTONE_DYNAMO);
    context.setBlockState(INPUT, input);
    context.setBlockState(LAMP, ModBlocks.LIGHT_STRIP);
    layRun(context);
  }

  private static void expectAfterSettling(
      TestContext context, ConnectedTestPlayer player, Identifier advancement, boolean earned) {
    context.waitAndRun(
        SETTLE,
        () -> {
          context.assertTrue(
              player.hasEarned(advancement) == earned, advancement + " earned must be " + earned);
          player.leave();
          context.complete();
        });
  }

  @GameTest(templateName = BENCH)
  public void aLampLitFromASwitchEarnsLetThereBeLight(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, NEARBY);
    poweredLampRun(context, floorSwitch());
    expectAfterSettling(context, player, LET_THERE_BE_LIGHT, true);
  }

  @GameTest(templateName = BENCH, batchId = "no-switch-nearby")
  public void aLampLitByRedstoneEarnsNothing(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, NEARBY);
    poweredLampRun(context, Blocks.REDSTONE_BLOCK.getDefaultState());
    expectAfterSettling(context, player, LET_THERE_BE_LIGHT, false);
  }

  @GameTest(templateName = BENCH)
  public void aPanelWithNoLampEarnsNothing(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, NEARBY);
    context.setBlockState(SOURCE, ModBlocks.LODESTONE_DYNAMO);
    context.setBlockState(
        INPUT,
        ModBlocks.CONTROL_PANEL.getDefaultState().with(WallMountedBlock.FACE, BlockFace.FLOOR));
    layRun(context);
    context.waitAndRun(
        SETTLE / 2L,
        () ->
            DeviceActions.panel(
                player.player(),
                new PanelActionPayload(
                    context.getAbsolutePos(INPUT),
                    new LampLook(Hue.RED, 9),
                    PanelActionPayload.NO_SLOT,
                    "")));
    expectAfterSettling(context, player, MOOD_LIGHTING, false);
  }

  @GameTest(templateName = BENCH)
  public void fillingAGeneratorDoesNotEarnChargedUp(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, NEARBY);
    context.setBlockState(SOURCE, ModBlocks.REDSTONE_GENERATOR);
    player.hold(new ItemStack(Items.REDSTONE, 1), ItemStack.EMPTY);
    player.useOn(new WireFace(context.getAbsolutePos(SOURCE), Direction.UP));
    context.assertTrue(player.player().getMainHandStack().isEmpty(), "the dust must be used");
    expectAfterSettling(context, player, CHARGED_UP, false);
  }

  @GameTest(templateName = BENCH, tickLimit = 120)
  public void aLampGoingDarkEarnsLightsOut(TestContext context) {
    ConnectedTestPlayer player = ConnectedTestPlayer.joinAt(context, NEARBY);
    context.setBlockState(SOURCE, ModBlocks.REDSTONE_CELL);
    context.setBlockState(INPUT, floorSwitch());
    context.setBlockState(LAMP, ModBlocks.LIGHT_STRIP);
    layRun(context);
    RedstoneCellBlockEntity cell = context.getBlockEntity(SOURCE);
    cell.setCharge(100);
    context.waitAndRun(
        SETTLE,
        () -> {
          SourceBlockEntity source = context.getBlockEntity(SOURCE);
          source.toggleSwitch(context.getWorld());
        });
    context.waitAndRun(
        3L * SETTLE,
        () -> {
          context.assertTrue(player.hasEarned(LIGHTS_OUT), "a lamp can be the last device");
          player.leave();
          context.complete();
        });
  }
}
