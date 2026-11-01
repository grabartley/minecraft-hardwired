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

  private ModSounds() {}

  public static void register() {}

  private static SoundEvent register(String path) {
    Identifier id = Identifier.of(Hardwired.MOD_ID, path);
    return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
  }
}
