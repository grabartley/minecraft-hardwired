package com.grahambartley.hardwired.block;

import java.util.Arrays;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.StringIdentifiable;

public enum PhoneBoxPaint implements StringIdentifiable {
  OAK(null),
  WHITE(DyeColor.WHITE),
  ORANGE(DyeColor.ORANGE),
  MAGENTA(DyeColor.MAGENTA),
  LIGHT_BLUE(DyeColor.LIGHT_BLUE),
  YELLOW(DyeColor.YELLOW),
  LIME(DyeColor.LIME),
  PINK(DyeColor.PINK),
  GRAY(DyeColor.GRAY),
  LIGHT_GRAY(DyeColor.LIGHT_GRAY),
  CYAN(DyeColor.CYAN),
  PURPLE(DyeColor.PURPLE),
  BLUE(DyeColor.BLUE),
  BROWN(DyeColor.BROWN),
  GREEN(DyeColor.GREEN),
  RED(DyeColor.RED),
  BLACK(DyeColor.BLACK);

  private final DyeColor dye;

  PhoneBoxPaint(DyeColor dye) {
    this.dye = dye;
  }

  public static PhoneBoxPaint of(DyeColor dye) {
    return Arrays.stream(values()).filter(paint -> paint.dye == dye).findFirst().orElseThrow();
  }

  public static PhoneBoxPaint on(ItemStack stack) {
    PhoneBoxPaint paint =
        stack
            .getOrDefault(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT)
            .getValue(PhoneBoxBlock.PAINT);
    return paint == null ? OAK : paint;
  }

  public float shade() {
    return ordinal() / (float) (values().length - 1);
  }

  @Override
  public String asString() {
    return dye == null ? "oak" : dye.asString();
  }
}
