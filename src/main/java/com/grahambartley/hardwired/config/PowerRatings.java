package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record PowerRatings(int cellRating, int generatorRating, int solarRating, int dynamoRating) {

  public static final int CELL_RATING_MIN = 0;
  public static final int CELL_RATING_MAX = 1024;
  public static final int GENERATOR_RATING_MIN = 0;
  public static final int GENERATOR_RATING_MAX = 1024;
  public static final int SOLAR_RATING_MIN = 0;
  public static final int SOLAR_RATING_MAX = 1024;
  public static final int DYNAMO_RATING_MIN = 0;
  public static final int DYNAMO_RATING_MAX = 1024;

  public static final PowerRatings DEFAULTS = new PowerRatings(16, 64, 6, 24);

  static final String KEY_CELL_RATING = "cellRating";
  static final String KEY_GENERATOR_RATING = "generatorRating";
  static final String KEY_SOLAR_RATING = "solarRating";
  static final String KEY_DYNAMO_RATING = "dynamoRating";

  public PowerRatings {
    cellRating = ConfigBounds.clamp(cellRating, CELL_RATING_MIN, CELL_RATING_MAX);
    generatorRating =
        ConfigBounds.clamp(generatorRating, GENERATOR_RATING_MIN, GENERATOR_RATING_MAX);
    solarRating = ConfigBounds.clamp(solarRating, SOLAR_RATING_MIN, SOLAR_RATING_MAX);
    dynamoRating = ConfigBounds.clamp(dynamoRating, DYNAMO_RATING_MIN, DYNAMO_RATING_MAX);
  }

  public PowerRatings withCellRating(final int value) {
    return new PowerRatings(value, generatorRating, solarRating, dynamoRating);
  }

  public PowerRatings withGeneratorRating(final int value) {
    return new PowerRatings(cellRating, value, solarRating, dynamoRating);
  }

  public PowerRatings withSolarRating(final int value) {
    return new PowerRatings(cellRating, generatorRating, value, dynamoRating);
  }

  public PowerRatings withDynamoRating(final int value) {
    return new PowerRatings(cellRating, generatorRating, solarRating, value);
  }

  static PowerRatings fromJson(final ConfigJsonFields fields) {
    return new PowerRatings(
        fields.readInt(KEY_CELL_RATING, DEFAULTS.cellRating, CELL_RATING_MIN, CELL_RATING_MAX),
        fields.readInt(
            KEY_GENERATOR_RATING,
            DEFAULTS.generatorRating,
            GENERATOR_RATING_MIN,
            GENERATOR_RATING_MAX),
        fields.readInt(KEY_SOLAR_RATING, DEFAULTS.solarRating, SOLAR_RATING_MIN, SOLAR_RATING_MAX),
        fields.readInt(
            KEY_DYNAMO_RATING, DEFAULTS.dynamoRating, DYNAMO_RATING_MIN, DYNAMO_RATING_MAX));
  }

  void writeJson(final JsonObject root) {
    root.addProperty(KEY_CELL_RATING, cellRating);
    root.addProperty(KEY_GENERATOR_RATING, generatorRating);
    root.addProperty(KEY_SOLAR_RATING, solarRating);
    root.addProperty(KEY_DYNAMO_RATING, dynamoRating);
  }
}
