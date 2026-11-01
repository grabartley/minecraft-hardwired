package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.config.BurnRates;
import com.grahambartley.hardwired.config.PowerRatings;
import com.grahambartley.hardwired.config.PowerSettings;
import java.util.function.ToIntFunction;

public enum SourceKind {
  CELL(PowerRatings::cellRating, BurnRates::cellChargePerLoadMinute, 0.5F),
  GENERATOR(PowerRatings::generatorRating, BurnRates::generatorChargePerLoadMinute, 1F),
  SOLAR(PowerRatings::solarRating, null, 0.3F),
  DYNAMO(PowerRatings::dynamoRating, null, 0.8F);

  private final ToIntFunction<PowerRatings> rating;
  private final ToIntFunction<BurnRates> burn;
  private final float soundVolume;

  SourceKind(ToIntFunction<PowerRatings> rating, ToIntFunction<BurnRates> burn, float soundVolume) {
    this.rating = rating;
    this.burn = burn;
    this.soundVolume = soundVolume;
  }

  public int rating(PowerSettings settings) {
    return rating.applyAsInt(settings.ratings());
  }

  public boolean burnsFuel() {
    return burn != null;
  }

  public float soundVolume() {
    return soundVolume;
  }

  public int chargePerLoadMinute(BurnRates rates) {
    return burn == null ? 0 : burn.applyAsInt(rates);
  }
}
