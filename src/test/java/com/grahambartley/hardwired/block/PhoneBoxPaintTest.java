package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.DyeColor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class PhoneBoxPaintTest {

  private static final String ITEM_MODEL =
      "src/main/resources/assets/hardwired/models/item/phone_box.json";

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @ParameterizedTest
  @EnumSource(DyeColor.class)
  void everyDyeHasAPaintNamedLikeIt(DyeColor dye) {
    assertEquals(dye.asString(), PhoneBoxPaint.of(dye).asString());
  }

  @ParameterizedTest(name = "{0} shades {1}")
  @CsvSource({"OAK,0", "WHITE,0.0625", "RED,0.9375", "BLACK,1"})
  void eachPaintPicksItsItemModel(PhoneBoxPaint paint, float shade) {
    assertEquals(shade, paint.shade(), 1e-6);
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(PhoneBoxPaint.class)
  void aDroppedBoxKeepsItsPaint(PhoneBoxPaint paint) {
    ItemStack stack = new ItemStack(Items.PAPER);
    stack.set(
        DataComponentTypes.BLOCK_STATE,
        BlockStateComponent.DEFAULT.with(PhoneBoxBlock.PAINT, paint));
    assertEquals(paint, PhoneBoxPaint.on(stack));
  }

  @ParameterizedTest(name = "a plain box is oak")
  @CsvSource("x")
  void aBoxWithNoPaintIsOak(String unused) {
    assertEquals(PhoneBoxPaint.OAK, PhoneBoxPaint.on(new ItemStack(Items.PAPER)));
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(PhoneBoxPaint.class)
  void eachPaintShowsItsOwnItemModel(PhoneBoxPaint paint) throws IOException {
    JsonObject model =
        JsonParser.parseString(Files.readString(Path.of(ITEM_MODEL))).getAsJsonObject();
    String chosen = "hardwired:block/phone_box_item";
    for (JsonElement override : model.getAsJsonArray("overrides")) {
      JsonObject entry = override.getAsJsonObject();
      if (paint.shade() >= entry.getAsJsonObject("predicate").get("hardwired:paint").getAsFloat()) {
        chosen = entry.get("model").getAsString();
      }
    }
    String expected =
        paint == PhoneBoxPaint.OAK
            ? "hardwired:block/phone_box_item"
            : "hardwired:item/phone_box_" + paint.asString();
    assertEquals(expected, chosen);
  }
}
