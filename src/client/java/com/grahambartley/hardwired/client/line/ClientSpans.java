package com.grahambartley.hardwired.client.line;

import com.grahambartley.hardwired.network.SpanViewPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.util.math.BlockPos;

public final class ClientSpans {

  public record Drawn(SpanCurve curve, int rgb) {}

  private static List<Drawn> drawn = List.of();

  private ClientSpans() {}

  public static void receive(SpanViewPayload payload) {
    drawn = lay(payload.spans());
  }

  public static void clear() {
    drawn = List.of();
  }

  public static List<Drawn> drawn() {
    return drawn;
  }

  static List<Drawn> lay(List<SpanViewPayload.SpanView> spans) {
    Map<Long, TreeSet<Integer>> coloursAt = new HashMap<>();
    for (SpanViewPayload.SpanView span : spans) {
      coloursAt.computeIfAbsent(span.from(), pole -> new TreeSet<>()).add(span.color());
      coloursAt.computeIfAbsent(span.to(), pole -> new TreeSet<>()).add(span.color());
    }
    List<Drawn> laid = new ArrayList<>();
    for (SpanViewPayload.SpanView span : spans) {
      TreeSet<Integer> colours = coloursAt.get(span.from());
      int lane = colours.headSet(span.color()).size();
      laid.add(
          new Drawn(
              SpanCurve.between(
                  BlockPos.fromLong(span.from()),
                  BlockPos.fromLong(span.to()),
                  lane,
                  colours.size()),
              SpanColours.rgb(span.color(), span.age(), span.on())));
    }
    return List.copyOf(laid);
  }
}
