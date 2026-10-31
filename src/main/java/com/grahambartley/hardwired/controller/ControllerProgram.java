package com.grahambartley.hardwired.controller;

import com.grahambartley.hardwired.wire.Hue;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record ControllerProgram(ControllerWiring wiring, Timeline timeline) {

  public static final ControllerProgram DEFAULT =
      new ControllerProgram(ControllerWiring.DEFAULT, Timeline.EMPTY);

  private static final String UNUSED = "";

  private static final Codec<TimelineStep> STEP_CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.INT.fieldOf("outputs").forGetter(TimelineStep::outputs),
                      Codec.INT.fieldOf("wait_ticks").forGetter(TimelineStep::waitTicks))
                  .apply(instance, TimelineStep::new));
  private static final Codec<Timeline> TIMELINE_CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      STEP_CODEC
                          .sizeLimitedListOf(Timeline.MAX_STEPS)
                          .fieldOf("steps")
                          .forGetter(Timeline::steps),
                      Codec.INT.fieldOf("auto_reverse_ticks").forGetter(Timeline::autoReverseTicks))
                  .apply(instance, Timeline::new));
  private static final Codec<Optional<Hue>> SLOT_CODEC =
      Codec.STRING.xmap(
          name -> name.equals(UNUSED) ? Optional.empty() : Optional.of(Hue.byName(name)),
          slot -> slot.map(Hue::asString).orElse(UNUSED));
  private static final Codec<ControllerWiring> WIRING_CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Hue.CODEC.fieldOf("open").forGetter(ControllerWiring::open),
                      Hue.CODEC.fieldOf("close").forGetter(ControllerWiring::close),
                      SLOT_CODEC
                          .sizeLimitedListOf(TimelineStep.OUTPUT_SLOTS)
                          .fieldOf("outputs")
                          .forGetter(ControllerWiring::outputs))
                  .apply(instance, ControllerWiring::new));
  public static final Codec<ControllerProgram> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      WIRING_CODEC.fieldOf("wiring").forGetter(ControllerProgram::wiring),
                      TIMELINE_CODEC.fieldOf("timeline").forGetter(ControllerProgram::timeline))
                  .apply(instance, ControllerProgram::new));

  private static final PacketCodec<RegistryByteBuf, List<TimelineStep>> STEPS_PACKET_CODEC =
      PacketCodec.tuple(
              PacketCodecs.VAR_INT,
              TimelineStep::outputs,
              PacketCodecs.VAR_INT,
              TimelineStep::waitTicks,
              TimelineStep::new)
          .<RegistryByteBuf>cast()
          .collect(PacketCodecs.toList(Timeline.MAX_STEPS));
  private static final PacketCodec<RegistryByteBuf, Timeline> TIMELINE_PACKET_CODEC =
      PacketCodec.tuple(
          STEPS_PACKET_CODEC,
          Timeline::steps,
          PacketCodecs.VAR_INT,
          Timeline::autoReverseTicks,
          Timeline::new);
  private static final PacketCodec<RegistryByteBuf, ControllerWiring> WIRING_PACKET_CODEC =
      PacketCodec.tuple(
          Hue.PACKET_CODEC,
          ControllerWiring::open,
          Hue.PACKET_CODEC,
          ControllerWiring::close,
          PacketCodecs.optional(Hue.PACKET_CODEC)
              .collect(PacketCodecs.toList(TimelineStep.OUTPUT_SLOTS)),
          ControllerWiring::outputs,
          ControllerWiring::new);
  public static final PacketCodec<RegistryByteBuf, ControllerProgram> PACKET_CODEC =
      PacketCodec.tuple(
          WIRING_PACKET_CODEC,
          ControllerProgram::wiring,
          TIMELINE_PACKET_CODEC,
          ControllerProgram::timeline,
          ControllerProgram::new);
}
