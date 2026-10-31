package com.grahambartley.hardwired.advancement;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.wire.Hue;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ModCriteria {

  public static final String LAMP_LIT = "lamp_lit";
  public static final String STREETLIGHT_LIT = "streetlight_lit";
  public static final String LAMP_AGED = "lamp_aged";
  public static final String PANEL_COLOR = "panel_color";
  public static final String SCENE_SAVED = "scene_saved";
  public static final String SHORT_CIRCUIT = "short_circuit";
  public static final DeviceCriterion DEVICE =
      Criteria.register(Hardwired.MOD_ID + ":device", new DeviceCriterion());

  private ModCriteria() {}

  public static void register() {}

  public static String colorEvent(Hue hue) {
    return "color_" + hue.asString();
  }

  public static void triggerAll(ServerPlayerEntity player, Hue hue) {
    DEVICE.trigger(player, PANEL_COLOR);
    DEVICE.trigger(player, colorEvent(hue));
  }
}
