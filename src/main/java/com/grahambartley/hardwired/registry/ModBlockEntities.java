package com.grahambartley.hardwired.registry;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.AnimatedDeviceBlockEntity;
import com.grahambartley.hardwired.block.ControlPanelBlockEntity;
import com.grahambartley.hardwired.block.ControllerBlockEntity;
import com.grahambartley.hardwired.block.LampBlockEntity;
import com.grahambartley.hardwired.block.MainPanelBlockEntity;
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

  private ModBlockEntities() {}

  public static void register() {}
}
