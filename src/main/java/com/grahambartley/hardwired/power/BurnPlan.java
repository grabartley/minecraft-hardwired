package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.config.BurnRates;
import java.util.List;

public final class BurnPlan {

  private static final double SECONDS_PER_MINUTE = 60D;

  public record Source(SourceKind kind, int rating) {}

  public record Away(int free, int fuelled) {}

  private final double[] burns;
  private final double earned;

  private BurnPlan(double[] burns, double earned) {
    this.burns = burns;
    this.earned = earned;
  }

  public static BurnPlan forSecond(List<Source> sources, Away away, int load, BurnRates rates) {
    int loadedFree =
        sources.stream().filter(s -> !s.kind().burnsFuel()).mapToInt(Source::rating).sum();
    int free = loadedFree + away.free();
    int fuelled =
        sources.stream().filter(s -> s.kind().burnsFuel()).mapToInt(Source::rating).sum()
            + away.fuelled();
    int left = Math.max(load - free, 0);
    double[] burns = new double[sources.size()];
    if (fuelled > 0) {
      for (int i = 0; i < burns.length; i++) {
        Source source = sources.get(i);
        if (source.kind().burnsFuel()) {
          double share = (double) left * source.rating() / fuelled;
          burns[i] = share * source.kind().chargePerLoadMinute(rates) / SECONDS_PER_MINUTE;
        }
      }
    }
    double spare = Math.clamp(free - load, 0, loadedFree);
    return new BurnPlan(burns, spare * rates.spareChargePerLoadMinute() / SECONDS_PER_MINUTE);
  }

  public double burnFor(int index) {
    return burns[index];
  }

  public double earned() {
    return earned;
  }
}
