package com.grahambartley.hardwired.screen;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DialFace {

  private static final double SWEEP_START = Math.toRadians(135);
  private static final double SWEEP = Math.toRadians(270);
  private static final int RING_SAMPLES = 48;

  public record Pixel(int x, int y) {}

  private DialFace() {}

  public static List<Pixel> ring(int radius) {
    Set<Pixel> pixels = new LinkedHashSet<>();
    for (int sample = 0; sample < RING_SAMPLES; sample++) {
      pixels.add(onCircle(2 * Math.PI * sample / RING_SAMPLES, radius));
    }
    return new ArrayList<>(pixels);
  }

  public static List<Pixel> needle(int value, int max, int radius) {
    double fraction = max <= 0 ? 0 : Math.clamp(value / (double) max, 0, 1);
    double angle = SWEEP_START + fraction * SWEEP;
    Set<Pixel> pixels = new LinkedHashSet<>();
    for (int length = 0; length < radius; length++) {
      pixels.add(onCircle(angle, length));
    }
    return new ArrayList<>(pixels);
  }

  private static Pixel onCircle(double angle, int radius) {
    return new Pixel(
        (int) Math.round(Math.cos(angle) * radius), (int) Math.round(Math.sin(angle) * radius));
  }
}
