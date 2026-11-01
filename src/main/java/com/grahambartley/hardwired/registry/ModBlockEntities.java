package com.grahambartley.hardwired.registry;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.AnimatedDeviceBlockEntity;
import com.grahambartley.hardwired.block.BroadcastDeskBlockEntity;
import com.grahambartley.hardwired.block.CircuitBlockEntity;
import com.grahambartley.hardwired.block.CircuitBoardBlockEntity;
import com.grahambartley.hardwired.block.ControlPanelBlockEntity;
import com.grahambartley.hardwired.block.ControllerBlockEntity;
import com.grahambartley.hardwired.block.ElectronicBlockEntity;
import com.grahambartley.hardwired.block.LampBlockEntity;
import com.grahambartley.hardwired.block.MainPanelBlockEntity;
import com.grahambartley.hardwired.block.MasterSwitchBlockEntity;
import com.grahambartley.hardwired.block.RadioBlockEntity;
import com.grahambartley.hardwired.block.RedstoneCellBlockEntity;
import com.grahambartley.hardwired.block.RedstoneGeneratorBlockEntity;
import com.grahambartley.hardwired.block.SourceBlockEntity;
import com.grahambartley.hardwired.block.SpeakerBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {

  public static final BlockEntityType<LampBlockEntity> LAMP =
      Registry.register(
          Registries.BLOCK_ENTITY_TYPE,
          Identifier.of(Hardwired.MOD_ID, "lamp"),
          BlockEntityType.Builder.create(LampBlockEntity::new, ModBlocks.lamps()).build(null));

  public static final BlockEntityType<ControlPanelBlockEntity> CONTROL_PANEL =
      Registry.register(
          Registries.BLOCK_ENTITY_TYPE,
          Identifier.of(Hardwired.MOD_ID, "control_panel"),
          BlockEntityType.Builder.create(ControlPanelBlockEntity::new, ModBlocks.CONTROL_PANEL)
              .build(null));

  public static final BlockEntityType<AnimatedDeviceBlockEntity> ANIMATED_DEVICE =
      Registry.register(
          Registries.BLOCK_ENTITY_TYPE,
          Identifier.of(Hardwired.MOD_ID, "animated_device"),
          BlockEntityType.Builder.create(
                  AnimatedDeviceBlockEntity::new, ModBlocks.animatedDevices())
              .build(null));

  public static final BlockEntityType<ControllerBlockEntity> CONTROLLER =
      Registry.register(
          Registries.BLOCK_ENTITY_TYPE,
          Identifier.of(Hardwired.MOD_ID, "controller"),
          BlockEntityType.Builder.create(ControllerBlockEntity::new, ModBlocks.CONTROLLER)
              .build(null));

  public static final BlockEntityType<MainPanelBlockEntity> MAIN_PANEL =
      Registry.register(
          Registries.BLOCK_ENTITY_TYPE,
          Identifier.of(Hardwired.MOD_ID, "main_panel"),
          BlockEntityType.Builder.create(MainPanelBlockEntity::new, ModBlocks.MAIN_PANEL)
              .build(null));

  public static final BlockEntityType<CircuitBoardBlockEntity> CIRCUIT_BOARD =
      Registry.register(
          Registries.BLOCK_ENTITY_TYPE,
          Identifier.of(Hardwired.MOD_ID, "circuit_board"),
          BlockEntityType.Builder.create(CircuitBoardBlockEntity::new, ModBlocks.CIRCUIT_BOARD)
              .build(null));

  public static final BlockEntityType<CircuitBlockEntity> CIRCUIT_BLOCK =
      Registry.register(
          Registries.BLOCK_ENTITY_TYPE,
          Identifier.of(Hardwired.MOD_ID, "circuit_block"),
          BlockEntityType.Builder.create(CircuitBlockEntity::new, ModBlocks.CIRCUIT_BLOCK)
              .build(null));

  public static final BlockEntityType<RedstoneGeneratorBlockEntity> REDSTONE_GENERATOR =
      Registry.register(
          Registries.BLOCK_ENTITY_TYPE,
          Identifier.of(Hardwired.MOD_ID, "redstone_generator"),
          BlockEntityType.Builder.create(
                  RedstoneGeneratorBlockEntity::new, ModBlocks.REDSTONE_GENERATOR)
              .build(null));

  public static final BlockEntityType<SourceBlockEntity> SOLAR_PANEL =
      sourceType("solar_panel", ModBlocks.SOLAR_PANEL);

  public static final BlockEntityType<SourceBlockEntity> LODESTONE_DYNAMO =
      sourceType("lodestone_dynamo", ModBlocks.LODESTONE_DYNAMO);

  public static final BlockEntityType<MasterSwitchBlockEntity> MASTER_SWITCH =
      Registry.register(
          Registries.BLOCK_ENTITY_TYPE,
          Identifier.of(Hardwired.MOD_ID, "master_switch"),
          BlockEntityType.Builder.create(MasterSwitchBlockEntity::new, ModBlocks.MASTER_SWITCH)
              .build(null));

  public static final BlockEntityType<RedstoneCellBlockEntity> REDSTONE_CELL =
      Registry.register(
          Registries.BLOCK_ENTITY_TYPE,
          Identifier.of(Hardwired.MOD_ID, "redstone_cell"),
          BlockEntityType.Builder.create(RedstoneCellBlockEntity::new, ModBlocks.REDSTONE_CELL)
              .build(null));

  public static final BlockEntityType<ElectronicBlockEntity> ELECTRONIC =
      Registry.register(
          Registries.BLOCK_ENTITY_TYPE,
          Identifier.of(Hardwired.MOD_ID, "electronic"),
          BlockEntityType.Builder.create(
                  ElectronicBlockEntity::new,
                  ModBlocks.ALARM,
                  ModBlocks.INTERCOM,
                  ModBlocks.LIFT,
                  ModBlocks.LIFT_FLOOR)
              .build(null));

  public static final BlockEntityType<RadioBlockEntity> RADIO =
      Registry.register(
          Registries.BLOCK_ENTITY_TYPE,
          Identifier.of(Hardwired.MOD_ID, "radio"),
          BlockEntityType.Builder.create(RadioBlockEntity::new, ModBlocks.RADIO).build(null));

  public static final BlockEntityType<SpeakerBlockEntity> SPEAKER =
      Registry.register(
          Registries.BLOCK_ENTITY_TYPE,
          Identifier.of(Hardwired.MOD_ID, "speaker"),
          BlockEntityType.Builder.create(SpeakerBlockEntity::new, ModBlocks.SPEAKER).build(null));

  public static final BlockEntityType<BroadcastDeskBlockEntity> BROADCAST_DESK =
      Registry.register(
          Registries.BLOCK_ENTITY_TYPE,
          Identifier.of(Hardwired.MOD_ID, "broadcast_desk"),
          BlockEntityType.Builder.create(BroadcastDeskBlockEntity::new, ModBlocks.BROADCAST_DESK)
              .build(null));

  private ModBlockEntities() {}

  @SuppressWarnings("unchecked")
  private static BlockEntityType<SourceBlockEntity> sourceType(String name, Block block) {
    BlockEntityType<SourceBlockEntity>[] type = new BlockEntityType[1];
    type[0] =
        Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(Hardwired.MOD_ID, name),
            BlockEntityType.Builder.<SourceBlockEntity>create(
                    (pos, state) -> new SourceBlockEntity(type[0], pos, state), block)
                .build(null));
    return type[0];
  }

  public static void register() {}
}
