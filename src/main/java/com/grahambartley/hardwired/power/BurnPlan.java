package com.grahambartley.hardwired.power;

import java.util.List;

public final class BurnPlan {

  private static final double SECONDS_PER_MINUTE = 60D;

  public record Source(SourceKind kind, int rating) {}

  private final double[] burns;
  private final double earned;

  private BurnPlan(double[] burns, double earned) {
    this.burns = burns;
    this.earned = earned;
  }

  public static BurnPlan forSecond(List<Source> sources, int load, int chargePerLoadMinute) {
    int free = sources.stream().filter(s -> !s.kind().burnsFuel()).mapToInt(Source::rating).sum();
    int fuelled = sources.stream().filter(s -> s.kind().burnsFuel()).mapToInt(Source::rating).sum();
    double perLoad = chargePerLoadMinute / SECONDS_PER_MINUTE;
    int left = Math.max(load - free, 0);
    double[] burns = new double[sources.size()];
    if (fuelled > 0) {
      for (int i = 0; i < burns.length; i++) {
        Source source = sources.get(i);
        if (source.kind().burnsFuel()) {
          double share = (double) left * source.rating() / fuelled;
          burns[i] = share * perLoad / source.kind().burnDivisor();
        }
      }
    }
    return new BurnPlan(burns, Math.max(free - load, 0) * perLoad);
  }

  public double burnFor(int index) {
    return burns[index];
  }

  public double earned() {
    return earned;
  }
}
