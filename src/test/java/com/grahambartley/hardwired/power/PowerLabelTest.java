package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.registry.ModBlocks;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.block.Block;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PowerLabelTest {

  private static final String GIVES = "tooltip.hardwired.gives_power";
  private static final String DRAWS = "tooltip.hardwired.draws_power";

  private static Stream<Arguments> labels() {
    return Stream.of(
        Arguments.of(ModBlocks.REDSTONE_CELL, GIVES, 16),
        Arguments.of(ModBlocks.REDSTONE_GENERATOR, GIVES, 64),
        Arguments.of(ModBlocks.SOLAR_PANEL, GIVES, 6),
        Arguments.of(ModBlocks.LODESTONE_DYNAMO, GIVES, 24),
        Arguments.of(ModBlocks.LIGHT_STRIP, DRAWS, 1),
        Arguments.of(ModBlocks.STREETLIGHT, DRAWS, 2),
        Arguments.of(ModBlocks.CONTROLLER, DRAWS, 2),
        Arguments.of(ModBlocks.CIRCUIT_BLOCK, DRAWS, 2),
        Arguments.of(ModBlocks.MAIN_PANEL, DRAWS, 2),
        Arguments.of(ModBlocks.RADIO, DRAWS, 4),
        Arguments.of(ModBlocks.SPEAKER, DRAWS, 4),
        Arguments.of(ModBlocks.INTERCOM, DRAWS, 4),
        Arguments.of(ModBlocks.ALARM, DRAWS, 8),
        Arguments.of(ModBlocks.BROADCAST_DESK, DRAWS, 16),
        Arguments.of(ModBlocks.LIFT, "tooltip.hardwired.draws_power_moving", 32));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("labels")
  void everyPowerBlockSaysItsNumber(Block block, String key, int amount) {
    assertEquals(
        Optional.of(new PowerLabel(key, amount)), PowerLabel.of(block, PowerSettings.DEFAULTS));
  }

  @Test
  void aBlockWithNoLoadSaysNothing() {
    assertEquals(Optional.empty(), PowerLabel.of(ModBlocks.WALL_SWITCH, PowerSettings.DEFAULTS));
    assertEquals(Optional.empty(), PowerLabel.of(ModBlocks.MASTER_SWITCH, PowerSettings.DEFAULTS));
  }

  @Test
  void aChangedSettingChangesTheLabel() {
    assertEquals(
        Optional.of(new PowerLabel(DRAWS, 9)),
        PowerLabel.of(ModBlocks.ALARM, PowerSettings.DEFAULTS.withAlarmLoad(9)));
  }
}
