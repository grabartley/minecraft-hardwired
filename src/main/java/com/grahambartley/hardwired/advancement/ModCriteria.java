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
  public static final String CONTROLLER_PISTON = "controller_piston";
  public static final String CIRCUIT_SAVED = "circuit_saved";
  public static final String CIRCUIT_MADE = "circuit_made";
  public static final String CIRCUIT_COPIED = "circuit_copied";
  public static final String CELL_FILLED = "cell_filled";
  public static final String SOLAR_CHARGED = "solar_charged";
  public static final String LIGHTNING_CHARGED = "lightning_charged";
  public static final String LIGHTS_OUT = "lights_out";
  public static final String LIFT_RIDDEN = "lift_ridden";
  public static final DeviceCriterion DEVICE =
      Criteria.register(Hardwired.MOD_ID + ":device", new DeviceCriterion());

  private ModCriteria() {}

  public static void register() {}

  public static void triggerAll(ServerPlayerEntity player, Hue hue) {
    DEVICE.trigger(player, PANEL_COLOR);
    DEVICE.trigger(player, DeviceCriterion.colorEvent(hue));
  }
}
