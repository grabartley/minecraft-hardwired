package com.grahambartley.hardwired.registry;

import com.grahambartley.hardwired.Hardwired;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {

  public static final SoundEvent RADIO_VOICE = register("radio.voice");
  public static final SoundEvent RADIO_HISS = register("radio.hiss");
  public static final SoundEvent RADIO_CRACKLE = register("radio.crackle");
  public static final SoundEvent RADIO_SWEEP = register("radio.sweep");

  public static final SoundEvent SOURCE_UP = register("power.source_up");
  public static final SoundEvent SOURCE_DOWN = register("power.source_down");
  public static final SoundEvent FUSE_POP = register("power.fuse_pop");
  public static final SoundEvent MASTER_PULL = register("power.master_pull");
  public static final SoundEvent MASTER_OPEN = register("power.master_open");
  public static final SoundEvent GENERATOR_LOOP = register("power.generator_loop");
  public static final SoundEvent CELL_LOOP = register("power.cell_loop");
  public static final SoundEvent DYNAMO_LOOP = register("power.dynamo_loop");
  public static final SoundEvent SOLAR_TICK = register("power.solar_tick");
  public static final SoundEvent ELECTRIC_HUM = register("power.electric_hum");

  public static final SoundEvent LIFT_DING = register("lift.ding");
  public static final SoundEvent LIFT_DING_STRIKE = register("lift.ding_strike");

  public static final SoundEvent TELEPHONE_BELL = register("telephone.bell");
  public static final SoundEvent TELEPHONE_DIAL_CLICK = register("telephone.dial_click");
  public static final SoundEvent TELEPHONE_RELAY = register("telephone.relay");
  public static final SoundEvent TELEPHONE_DIAL_TONE = register("telephone.dial_tone");
  public static final SoundEvent TELEPHONE_RINGBACK = register("telephone.ringback");
  public static final SoundEvent TELEPHONE_BUSY = register("telephone.busy");
  public static final SoundEvent TELEPHONE_VOICE = register("telephone.voice");
  public static final SoundEvent PHONE_BOX_DOOR_OPEN = register("phone_box.door_open");
  public static final SoundEvent PHONE_BOX_DOOR_CLOSE = register("phone_box.door_close");

  private ModSounds() {}

  public static void register() {}

  private static SoundEvent register(String path) {
    Identifier id = Identifier.of(Hardwired.MOD_ID, path);
    return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
  }
}
