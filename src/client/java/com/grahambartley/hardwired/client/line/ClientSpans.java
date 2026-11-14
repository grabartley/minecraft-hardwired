package com.grahambartley.hardwired.client.line;

import com.grahambartley.hardwired.block.TelephonePoleBlock;
import com.grahambartley.hardwired.network.SpanViewPayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class ClientSpans {

  public record Drawn(SpanCurve curve, int rgb) {}

  private static final int RELAY_TICKS = 20;
  private static List<SpanViewPayload.SpanView> views = List.of();
  private static List<Drawn> drawn = List.of();
  private static int ticks;

  private ClientSpans() {}

  public static void receive(SpanViewPayload payload) {
    views = payload.spans();
    relay(MinecraftClient.getInstance());
  }

  public static void tick(MinecraftClient client) {
    if (++ticks % RELAY_TICKS == 0) {
      relay(client);
    }
  }

  public static void clear() {
    views = List.of();
    drawn = List.of();
  }

  public static List<Drawn> drawn() {
    return drawn;
  }

  private static void relay(MinecraftClient client) {
    if (client.world == null) {
      drawn = List.of();
      return;
    }
    drawn =
        lay(
            views,
            top -> {
              BlockState state = client.world.getBlockState(top);
              return TelephonePoleBlock.isPole(state)
                  ? state.get(Properties.HORIZONTAL_AXIS)
                  : Direction.Axis.X;
            });
  }

  static List<Drawn> lay(
      List<SpanViewPayload.SpanView> spans, Function<BlockPos, Direction.Axis> armAt) {
    Map<Long, TreeSet<Integer>> coloursAt = new HashMap<>();
    for (SpanViewPayload.SpanView span : spans) {
      coloursAt.computeIfAbsent(span.from(), pole -> new TreeSet<>()).add(span.color());
      coloursAt.computeIfAbsent(span.to(), pole -> new TreeSet<>()).add(span.color());
    }
    return spans.stream()
        .map(
            span ->
                new Drawn(
                    SpanCurve.between(
                        end(span.from(), span.color(), coloursAt, armAt),
                        end(span.to(), span.color(), coloursAt, armAt)),
                    SpanColours.rgb(span.color(), span.age(), span.on())))
        .toList();
  }

  private static SpanCurve.End end(
      long top,
      int color,
      Map<Long, TreeSet<Integer>> coloursAt,
      Function<BlockPos, Direction.Axis> armAt) {
    TreeSet<Integer> colours = coloursAt.get(top);
    BlockPos pos = BlockPos.fromLong(top);
    return new SpanCurve.End(pos, armAt.apply(pos), colours.headSet(color).size(), colours.size());
  }
}
