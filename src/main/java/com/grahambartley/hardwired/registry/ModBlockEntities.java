package com.grahambartley.hardwired.registry;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.ControlPanelBlockEntity;
import com.grahambartley.hardwired.block.LampBlockEntity;
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

  private ModBlockEntities() {}

  public static void register() {}
}
