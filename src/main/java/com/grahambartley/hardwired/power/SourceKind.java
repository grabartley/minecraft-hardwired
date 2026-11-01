package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.config.PowerSettings;
import java.util.function.ToIntFunction;

public enum SourceKind {
  CELL(PowerSettings::cellRating, 1),
  GENERATOR(PowerSettings::generatorRating, 2),
  SOLAR(PowerSettings::solarRating, 0),
  DYNAMO(PowerSettings::dynamoRating, 0);

  private final ToIntFunction<PowerSettings> rating;
  private final int burnDivisor;

  SourceKind(ToIntFunction<PowerSettings> rating, int burnDivisor) {
    this.rating = rating;
    this.burnDivisor = burnDivisor;
  }

  public int rating(PowerSettings settings) {
    return rating.applyAsInt(settings);
  }

  public boolean burnsFuel() {
    return burnDivisor > 0;
  }

  public int burnDivisor() {
    return burnDivisor;
  }
}
