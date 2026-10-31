package com.grahambartley.hardwired.registry;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.AbstractLampBlock;
import com.grahambartley.hardwired.block.ControlPanelBlock;
import com.grahambartley.hardwired.block.CopperLampBlock;
import com.grahambartley.hardwired.block.DuskClock;
import com.grahambartley.hardwired.block.JunctionBlock;
import com.grahambartley.hardwired.block.MountedLampBlock;
import com.grahambartley.hardwired.block.SensorBlock;
import com.grahambartley.hardwired.block.StreetlightBlock;
import com.grahambartley.hardwired.block.SwitchBlock;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {

  private static final double[] CEILING_LAMP_BOX = {3, 0, 3, 13, 13, 13};
  private static final double[] WALL_LAMP_BOX = {5, 0, 5, 11, 10, 11};
  private static final double[] STRIP_BOX = {0, 0, 6, 16, 4, 10};
  private static final double[] PANEL_BOX = {2, 0, 1, 14, 2, 15};
  private static final double[] SWITCH_BOX = {5, 0, 5, 11, 4, 11};
  private static final double[] BUTTON_BOX = {5, 0, 5, 11, 3, 11};
  private static final double[] MOTION_SENSOR_BOX = {5, 0, 5, 11, 4, 11};
  private static final double[] DUSK_SENSOR_BOX = {4, 0, 4, 12, 3, 12};

  public static final Block COPPER_LAMP =
      register(
          "copper_lamp",
          new CopperLampBlock(
              AbstractBlock.Settings.copy(Blocks.COPPER_BULB)
                  .luminance(AbstractLampBlock::lightLevel)
                  .emissiveLighting(AbstractLampBlock::isGlowing)));
  public static final Block COPPER_CEILING_LAMP =
      register("copper_ceiling_lamp", mountedLamp(CEILING_LAMP_BOX, Set.of(BlockFace.CEILING)));
  public static final Block COPPER_WALL_LAMP =
      register("copper_wall_lamp", mountedLamp(WALL_LAMP_BOX, Set.of(BlockFace.WALL)));
  public static final Block LIGHT_STRIP =
      register(
          "light_strip",
          mountedLamp(STRIP_BOX, EnumSet.of(BlockFace.WALL, BlockFace.CEILING, BlockFace.FLOOR)));
  public static final Block STREETLIGHT =
      register(
          "streetlight",
          new StreetlightBlock(
              deviceSettings()
                  .luminance(StreetlightBlock::headLightLevel)
                  .emissiveLighting(StreetlightBlock::isHeadGlowing)));
  public static final Block CONTROL_PANEL =
      register("control_panel", new ControlPanelBlock(deviceSettings(), PANEL_BOX));
  public static final Block JUNCTION =
      register(
          "junction",
          new JunctionBlock(
              AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK)
                  .pistonBehavior(PistonBehavior.BLOCK)));
  public static final Block WALL_SWITCH =
      register("wall_switch", new SwitchBlock(deviceSettings(), SWITCH_BOX, true));
  public static final Block PUSH_BUTTON =
      register("push_button", new SwitchBlock(deviceSettings(), BUTTON_BOX, false));
  public static final Block MOTION_SENSOR =
      register(
          "motion_sensor",
          new SensorBlock(deviceSettings(), MOTION_SENSOR_BOX, SensorBlock::livingEntityNearby));
  public static final Block DUSK_SENSOR =
      register(
          "dusk_sensor",
          new SensorBlock(
              deviceSettings(),
              DUSK_SENSOR_BOX,
              (world, pos) -> DuskClock.isDark(world.getTimeOfDay())));

  private ModBlocks() {}

  public static void register() {}

  public static Block[] lamps() {
    return new Block[] {
      COPPER_LAMP, COPPER_CEILING_LAMP, COPPER_WALL_LAMP, LIGHT_STRIP, STREETLIGHT
    };
  }

  public static Block[] animatedDevices() {
    return new Block[] {};
  }

  private static MountedLampBlock mountedLamp(double[] floorBox, Set<BlockFace> faces) {
    return new MountedLampBlock(
        deviceSettings()
            .luminance(AbstractLampBlock::lightLevel)
            .emissiveLighting(AbstractLampBlock::isGlowing),
        floorBox,
        faces);
  }

  private static AbstractBlock.Settings deviceSettings() {
    return AbstractBlock.Settings.create()
        .mapColor(MapColor.ORANGE)
        .strength(1.5F, 6F)
        .sounds(BlockSoundGroup.COPPER)
        .pistonBehavior(PistonBehavior.DESTROY)
        .nonOpaque();
  }

  private static Block register(String name, Block block) {
    return Registry.register(Registries.BLOCK, Identifier.of(Hardwired.MOD_ID, name), block);
  }
}
