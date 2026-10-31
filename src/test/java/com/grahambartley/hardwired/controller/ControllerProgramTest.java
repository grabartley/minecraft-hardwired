package com.grahambartley.hardwired.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.wire.Hue;
import io.netty.buffer.Unpooled;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ControllerProgramTest {

  private static ControllerWiring wiring() {
    return new ControllerWiring(
        Hue.GREEN,
        Hue.ORANGE,
        List.of(Optional.of(Hue.RED), Optional.empty(), Optional.of(Hue.BLACK)));
  }

  private static Stream<ControllerProgram> programs() {
    return Stream.of(
        ControllerProgram.DEFAULT,
        new ControllerProgram(
            wiring(),
            new Timeline(List.of(new TimelineStep(5, 8), new TimelineStep(0xFF, 1200)), 400)));
  }

  private static RegistryByteBuf buffer() {
    return new RegistryByteBuf(Unpooled.buffer(), DynamicRegistryManager.EMPTY);
  }

  @ParameterizedTest
  @MethodSource("programs")
  void aProgramSurvivesTheSave(ControllerProgram program) {
    NbtElement saved = ControllerProgram.CODEC.encodeStart(NbtOps.INSTANCE, program).getOrThrow();
    assertEquals(program, ControllerProgram.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow());
  }

  @ParameterizedTest
  @MethodSource("programs")
  void aProgramSurvivesTheWire(ControllerProgram program) {
    RegistryByteBuf buffer = buffer();
    ControllerProgram.PACKET_CODEC.encode(buffer, program);
    assertEquals(program, ControllerProgram.PACKET_CODEC.decode(buffer));
  }

  @Test
  void anOversizedTimelineIsRefusedOnTheWire() {
    RegistryByteBuf buffer = buffer();
    new Oversized(Collections.nCopies(Timeline.MAX_STEPS + 1, new TimelineStep(1, 2)))
        .write(buffer);
    assertThrows(RuntimeException.class, () -> ControllerProgram.PACKET_CODEC.decode(buffer));
  }

  private record Oversized(List<TimelineStep> steps) {

    void write(RegistryByteBuf buffer) {
      buffer.writeVarInt(Hue.RED.ordinal());
      buffer.writeVarInt(Hue.RED.ordinal());
      buffer.writeVarInt(0);
      buffer.writeVarInt(steps.size());
      for (TimelineStep step : steps) {
        buffer.writeVarInt(step.outputs());
        buffer.writeVarInt(step.waitTicks());
      }
      buffer.writeVarInt(0);
    }
  }

  @Test
  void anOversizedTimelineIsRefusedFromASave() {
    var tooMany =
        ControllerProgram.CODEC
            .encodeStart(NbtOps.INSTANCE, ControllerProgram.DEFAULT)
            .getOrThrow()
            .copy();
    var compound = (net.minecraft.nbt.NbtCompound) tooMany;
    var steps = new net.minecraft.nbt.NbtList();
    for (int step = 0; step <= Timeline.MAX_STEPS; step++) {
      var saved = new net.minecraft.nbt.NbtCompound();
      saved.putInt("outputs", 1);
      saved.putInt("wait_ticks", 4);
      steps.add(saved);
    }
    compound.getCompound("timeline").put("steps", steps);
    assertTrue(ControllerProgram.CODEC.parse(NbtOps.INSTANCE, compound).error().isPresent());
  }
}
