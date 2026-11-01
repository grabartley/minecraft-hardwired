package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.config.PowerSettings;
import java.util.function.ToIntFunction;

public enum SourceKind {
  CELL(PowerSettings::cellRating, 1, 0.5F),
  GENERATOR(PowerSettings::generatorRating, 2, 1F),
  SOLAR(PowerSettings::solarRating, 0, 0.3F),
  DYNAMO(PowerSettings::dynamoRating, 0, 0.8F);

  private final ToIntFunction<PowerSettings> rating;
  private final int burnDivisor;
  private final float soundVolume;

  SourceKind(ToIntFunction<PowerSettings> rating, int burnDivisor, float soundVolume) {
    this.rating = rating;
    this.burnDivisor = burnDivisor;
    this.soundVolume = soundVolume;
  }

  public int rating(PowerSettings settings) {
    return rating.applyAsInt(settings);
  }

  public boolean burnsFuel() {
    return burnDivisor > 0;
  }

  public float soundVolume() {
    return soundVolume;
  }

  public int burnDivisor() {
    return burnDivisor;
  }
}
