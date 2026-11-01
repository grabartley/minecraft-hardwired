package com.grahambartley.hardwired.registry;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.item.WireItem;
import com.grahambartley.hardwired.wire.Hue;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.TallBlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItems {

  private static final Map<Hue, Item> WIRES = new EnumMap<>(Hue.class);

  static {
    for (Hue color : Hue.values()) {
      String name = color == Hue.PLAIN ? "copper_wire" : color.asString() + "_copper_wire";
      WIRES.put(color, register(name, new WireItem(color, new Item.Settings())));
    }
  }

  private static final List<Block> BLOCK_ITEMS =
      List.of(
          ModBlocks.COPPER_CEILING_LAMP,
          ModBlocks.COPPER_WALL_LAMP,
          ModBlocks.LIGHT_STRIP,
          ModBlocks.STREETLIGHT,
          ModBlocks.CONTROL_PANEL,
          ModBlocks.JUNCTION,
          ModBlocks.WALL_SWITCH,
          ModBlocks.PUSH_BUTTON,
          ModBlocks.MOTION_SENSOR,
          ModBlocks.DUSK_SENSOR,
          ModBlocks.CONTROLLER,
          ModBlocks.MAIN_PANEL,
          ModBlocks.CIRCUIT_BOARD,
          ModBlocks.BLANK_CIRCUIT_BLOCK,
          ModBlocks.CIRCUIT_BLOCK,
          ModBlocks.REDSTONE_CELL,
          ModBlocks.SOLAR_PANEL,
          ModBlocks.REDSTONE_GENERATOR,
          ModBlocks.LODESTONE_DYNAMO,
          ModBlocks.MASTER_SWITCH,
          ModBlocks.ALARM,
          ModBlocks.RADIO,
          ModBlocks.SPEAKER,
          ModBlocks.INTERCOM,
          ModBlocks.LIFT,
          ModBlocks.RADIO_MAST,
          ModBlocks.BROADCAST_DESK);

  private ModItems() {}

  public static Item wire(Hue color) {
    return WIRES.get(color);
  }

  public static void register() {
    BLOCK_ITEMS.forEach(ModItems::registerBlockItem);
    Item liftFloor =
        register("lift_floor", new TallBlockItem(ModBlocks.LIFT_FLOOR, new Item.Settings()));
    RegistryKey<ItemGroup> key =
        RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(Hardwired.MOD_ID, "hardwired"));
    Registry.register(
        Registries.ITEM_GROUP,
        key,
        FabricItemGroup.builder()
            .icon(() -> wire(Hue.PLAIN).getDefaultStack())
            .displayName(Text.translatable("itemGroup.hardwired"))
            .entries(
                (context, entries) -> {
                  WIRES.values().forEach(entries::add);
                  BLOCK_ITEMS.forEach(entries::add);
                  entries.add(liftFloor);
                })
            .build());
  }

  private static void registerBlockItem(Block block) {
    Registry.register(
        Registries.ITEM, Registries.BLOCK.getId(block), new BlockItem(block, new Item.Settings()));
  }

  private static Item register(String name, Item item) {
    return Registry.register(Registries.ITEM, Identifier.of(Hardwired.MOD_ID, name), item);
  }
}
