package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.config.PowerRatings;
import com.grahambartley.hardwired.config.PowerSettings;
import java.util.function.ToIntFunction;

public enum SourceKind {
  CELL(PowerRatings::cellRating, 1, 0.5F),
  GENERATOR(PowerRatings::generatorRating, 2, 1F),
  SOLAR(PowerRatings::solarRating, 0, 0.3F),
  DYNAMO(PowerRatings::dynamoRating, 0, 0.8F);

  private final ToIntFunction<PowerRatings> rating;
  private final int burnDivisor;
  private final float soundVolume;

  SourceKind(ToIntFunction<PowerRatings> rating, int burnDivisor, float soundVolume) {
    this.rating = rating;
    this.burnDivisor = burnDivisor;
    this.soundVolume = soundVolume;
  }

  public int rating(PowerSettings settings) {
    return rating.applyAsInt(settings.ratings());
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
