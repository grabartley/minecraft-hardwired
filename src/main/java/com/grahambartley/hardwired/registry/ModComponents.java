package com.grahambartley.hardwired.registry;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.controller.ControllerProgram;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.Scene;
import java.util.List;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModComponents {

  public static final ComponentType<Hue> LAMP_COLOR =
      Registry.register(
          Registries.DATA_COMPONENT_TYPE,
          Identifier.of(Hardwired.MOD_ID, "lamp_color"),
          ComponentType.<Hue>builder().codec(Hue.CODEC).packetCodec(Hue.PACKET_CODEC).build());

  public static final ComponentType<List<Scene>> SCENES =
      Registry.register(
          Registries.DATA_COMPONENT_TYPE,
          Identifier.of(Hardwired.MOD_ID, "scenes"),
          ComponentType.<List<Scene>>builder()
              .codec(Scene.LIST_CODEC)
              .packetCodec(Scene.LIST_PACKET_CODEC)
              .build());

  public static final ComponentType<ControllerProgram> CONTROLLER_PROGRAM =
      Registry.register(
          Registries.DATA_COMPONENT_TYPE,
          Identifier.of(Hardwired.MOD_ID, "controller_program"),
          ComponentType.<ControllerProgram>builder()
              .codec(ControllerProgram.CODEC)
              .packetCodec(ControllerProgram.PACKET_CODEC)
              .build());

  private ModComponents() {}

  public static void register() {}
}
