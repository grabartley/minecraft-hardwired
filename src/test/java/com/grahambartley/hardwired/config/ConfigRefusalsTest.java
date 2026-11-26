package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Optional;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ConfigRefusalsTest {

  @ParameterizedTest(name = "min {0} and max {1} refused: {2}")
  @CsvSource({"5,32,false", "32,32,false", "1,128,false", "33,32,true", "40,33,true", "64,1,true"})
  void aTowerMinimumAboveTheMaximumIsRefused(int min, int max, boolean refused) {
    HardwiredConfig config =
        HardwiredConfig.defaults()
            .withRadio(RadioSettings.DEFAULTS.withTowerMaxMasts(max).withTowerMinMasts(min));
    Optional<Text> refusal = ConfigRefusals.of(config);
    assertEquals(refused, refusal.isPresent());
    refusal.ifPresent(
        text -> {
          TranslatableTextContent content =
              assertInstanceOf(TranslatableTextContent.class, text.getContent());
          assertEquals(ConfigRefusals.TOWER_MASTS, content.getKey());
          assertArrayEquals(new Object[] {min, max}, content.getArgs());
        });
  }
}
