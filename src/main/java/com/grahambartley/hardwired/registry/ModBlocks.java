package com.grahambartley.hardwired.registry;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.AbstractLampBlock;
import com.grahambartley.hardwired.block.AlarmBlock;
import com.grahambartley.hardwired.block.BroadcastDeskBlock;
import com.grahambartley.hardwired.block.CircuitBlock;
import com.grahambartley.hardwired.block.CircuitBoardBlock;
import com.grahambartley.hardwired.block.ControlPanelBlock;
import com.grahambartley.hardwired.block.ControllerBlock;
import com.grahambartley.hardwired.block.DuskClock;
import com.grahambartley.hardwired.block.FuelledSourceBlock;
import com.grahambartley.hardwired.block.IntercomBlock;
import com.grahambartley.hardwired.block.JunctionBlock;
import com.grahambartley.hardwired.block.LiftBlock;
import com.grahambartley.hardwired.block.LiftFloorBlock;
import com.grahambartley.hardwired.block.LodestoneDynamoBlock;
import com.grahambartley.hardwired.block.MainPanelBlock;
import com.grahambartley.hardwired.block.MasterSwitchBlock;
import com.grahambartley.hardwired.block.MountedLampBlock;
import com.grahambartley.hardwired.block.RadioBlock;
import com.grahambartley.hardwired.block.RadioMastBlock;
import com.grahambartley.hardwired.block.RedstoneCellBlock;
import com.grahambartley.hardwired.block.RedstoneGeneratorBlock;
import com.grahambartley.hardwired.block.SensorBlock;
import com.grahambartley.hardwired.block.SolarPanelBlock;
import com.grahambartley.hardwired.block.SpeakerBlock;
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

  private static final double[] CEILING_LAMP_BOX = {4, 0, 4, 12, 16, 12};
  private static final double[] WALL_LAMP_BOX = {4, 0, 2, 12, 6, 15};
  private static final double[] STRIP_BOX = {0, 0, 5, 16, 4, 11};
  private static final double[] PANEL_BOX = {2, 0, 1, 14, 2, 15};
  private static final double[] MAIN_PANEL_BOX = {0, 0, 0, 16, 3, 16};
  private static final double[] SWITCH_BOX = {5, 0, 5, 11, 4, 11};
  private static final double[] BUTTON_BOX = {5, 0, 5, 11, 3, 11};
  private static final double[] MOTION_SENSOR_BOX = {5, 0, 5, 11, 4, 11};
  private static final double[] DUSK_SENSOR_BOX = {4, 0, 4, 12, 3, 12};
  private static final double[] ALARM_BOX = {3, 0, 3, 13, 6, 13};
  private static final double[] RADIO_BOX = {1, 0, 2, 15, 14, 14};
  private static final double[] SPEAKER_BOX = {3, 0, 3, 13, 13, 13};
  private static final double[] INTERCOM_BOX = {5, 0, 5, 11, 12, 11};
  private static final int RADIO_GLOW = 7;
  private static final int DESK_GLOW = 7;

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

  public static final Block CONTROLLER =
      register(
          "controller",
          new ControllerBlock(
              deviceSettings()
                  .mapColor(MapColor.DEEPSLATE_GRAY)
                  .sounds(BlockSoundGroup.POLISHED_DEEPSLATE)
                  .pistonBehavior(PistonBehavior.BLOCK)));
  public static final Block MAIN_PANEL =
      register("main_panel", new MainPanelBlock(deviceSettings(), MAIN_PANEL_BOX));

  public static final Block CIRCUIT_BOARD =
      register("circuit_board", new CircuitBoardBlock(computerSettings().nonOpaque()));
  public static final Block BLANK_CIRCUIT_BLOCK =
      register("blank_circuit_block", new Block(computerSettings()));
  public static final Block CIRCUIT_BLOCK =
      register("circuit_block", new CircuitBlock(computerSettings()));

  public static final Block REDSTONE_CELL =
      register(
          "redstone_cell",
          new RedstoneCellBlock(
              deviceSettings()
                  .sounds(BlockSoundGroup.GLASS)
                  .pistonBehavior(PistonBehavior.BLOCK)
                  .luminance(state -> state.get(FuelledSourceBlock.LEVEL))
                  .emissiveLighting(
                      (state, world, pos) -> state.get(FuelledSourceBlock.LEVEL) > 0)));
  public static final Block REDSTONE_GENERATOR =
      register(
          "redstone_generator",
          new RedstoneGeneratorBlock(
              deviceSettings()
                  .mapColor(MapColor.IRON_GRAY)
                  .sounds(BlockSoundGroup.METAL)
                  .pistonBehavior(PistonBehavior.BLOCK)
                  .luminance(state -> state.get(FuelledSourceBlock.LEVEL))));
  public static final Block LODESTONE_DYNAMO =
      register(
          "lodestone_dynamo",
          new LodestoneDynamoBlock(
              deviceSettings()
                  .mapColor(MapColor.GOLD)
                  .sounds(BlockSoundGroup.LODESTONE)
                  .pistonBehavior(PistonBehavior.BLOCK)));
  public static final Block MASTER_SWITCH =
      register(
          "master_switch",
          new MasterSwitchBlock(
              deviceSettings()
                  .mapColor(MapColor.DEEPSLATE_GRAY)
                  .sounds(BlockSoundGroup.POLISHED_DEEPSLATE)
                  .pistonBehavior(PistonBehavior.DESTROY)));
  public static final Block SOLAR_PANEL =
      register(
          "solar_panel",
          new SolarPanelBlock(
              deviceSettings()
                  .pistonBehavior(PistonBehavior.BLOCK)
                  .emissiveLighting((state, world, pos) -> state.get(SolarPanelBlock.LIT))));

  public static final Block ALARM = register("alarm", new AlarmBlock(deviceSettings(), ALARM_BOX));
  public static final Block RADIO =
      register(
          "radio",
          new RadioBlock(
              deviceSettings()
                  .mapColor(MapColor.OAK_TAN)
                  .sounds(BlockSoundGroup.WOOD)
                  .pistonBehavior(PistonBehavior.BLOCK)
                  .luminance(state -> state.get(RadioBlock.PLAYING) ? RADIO_GLOW : 0),
              RADIO_BOX));
  public static final Block SPEAKER =
      register("speaker", new SpeakerBlock(deviceSettings(), SPEAKER_BOX));
  public static final Block INTERCOM =
      register("intercom", new IntercomBlock(deviceSettings(), INTERCOM_BOX));

  public static final Block LIFT =
      register(
          "lift",
          new LiftBlock(
              deviceSettings()
                  .mapColor(MapColor.DEEPSLATE_GRAY)
                  .sounds(BlockSoundGroup.POLISHED_DEEPSLATE)
                  .pistonBehavior(PistonBehavior.BLOCK)));
  public static final Block LIFT_FLOOR =
      register("lift_floor", new LiftFloorBlock(deviceSettings()));

  public static final Block RADIO_MAST =
      register(
          "radio_mast",
          new RadioMastBlock(
              AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK)
                  .nonOpaque()
                  .pistonBehavior(PistonBehavior.BLOCK)));
  public static final Block BROADCAST_DESK =
      register(
          "broadcast_desk",
          new BroadcastDeskBlock(
              deviceSettings()
                  .mapColor(MapColor.DEEPSLATE_GRAY)
                  .sounds(BlockSoundGroup.POLISHED_DEEPSLATE)
                  .pistonBehavior(PistonBehavior.BLOCK)
                  .luminance(state -> state.get(BroadcastDeskBlock.ON_AIR) ? DESK_GLOW : 0)));

  private ModBlocks() {}

  public static void register() {}

  public static Block[] lamps() {
    return new Block[] {COPPER_CEILING_LAMP, COPPER_WALL_LAMP, LIGHT_STRIP, STREETLIGHT};
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

  private static AbstractBlock.Settings computerSettings() {
    return AbstractBlock.Settings.create()
        .mapColor(MapColor.TERRACOTTA_WHITE)
        .strength(1.5F, 6F)
        .sounds(BlockSoundGroup.STONE)
        .pistonBehavior(PistonBehavior.BLOCK);
  }

  private static Block register(String name, Block block) {
    return Registry.register(Registries.BLOCK, Identifier.of(Hardwired.MOD_ID, name), block);
  }
}
