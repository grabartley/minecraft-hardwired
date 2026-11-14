package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.config.BurnRates;
import com.grahambartley.hardwired.config.CircuitSettings;
import com.grahambartley.hardwired.config.DeviceSettings;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.config.HumSettings;
import com.grahambartley.hardwired.config.LampSettings;
import com.grahambartley.hardwired.config.LiftSettings;
import com.grahambartley.hardwired.config.LineSettings;
import com.grahambartley.hardwired.config.PhoneSettings;
import com.grahambartley.hardwired.config.PowerLoads;
import com.grahambartley.hardwired.config.PowerRatings;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.config.RadioSettings;
import com.grahambartley.hardwired.config.WireSettings;
import com.grahambartley.hardwired.network.ServerConfigPayloads.EditServerConfigC2SPayload;
import com.grahambartley.hardwired.network.ServerConfigPayloads.SyncServerConfigS2CPayload;
import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.network.RegistryByteBuf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ServerConfigPayloadsTest {

  private static final int FIELDS = 18;

  private static List<Integer> fields(HardwiredConfig config) {
    return List.of(
        config.wire().viewRangeBlocks(),
        config.wire().maxNetworkSize(),
        config.lamp().ageRateMultiplierPercent(),
        config.device().motionSensorRangeBlocks(),
        config.device().buttonHoldTicks(),
        config.device().intercomRangeBlocks(),
        config.power().hashCode(),
        config.lift().ticksPerBlock(),
        config.circuit().maxParts(),
        config.circuit().partsPerRedstone(),
        config.radio().towerMinMasts(),
        config.radio().towerMaxMasts(),
        config.radio().towerBaseReach(),
        config.radio().towerReachPerMast(),
        config.radio().radioHearing(),
        config.radio().speakerHearing(),
        config.radio().undergroundFreeDepth(),
        config.radio().undergroundLossDepth());
  }

  private static Stream<Arguments> configs() {
    return Stream.of(
        Arguments.of(
            new HardwiredConfig(
                new WireSettings(9, 65),
                new LampSettings(150),
                new DeviceSettings(2, 3, 4),
                PowerSettings.DEFAULTS
                    .withRatings(PowerRatings.DEFAULTS.withCellRating(40))
                    .withLoads(PowerLoads.DEFAULTS.withLiftLoad(16)),
                new LiftSettings(13),
                new CircuitSettings(200, 15),
                new RadioSettings(17, 18, 19, 20, 21, 22, 23, 24, false),
                new PhoneSettings(12, 3, false),
                new LineSettings(20, 2))),
        Arguments.of(
            new HardwiredConfig(
                new WireSettings(96, 65536),
                new LampSettings(1000),
                new DeviceSettings(16, 200, 128),
                PowerSettings.DEFAULTS
                    .withHum(HumSettings.DEFAULTS.withHumFullLoad(900))
                    .withBurn(new BurnRates(9, 8, 7)),
                new LiftSettings(40),
                new CircuitSettings(256, 64),
                new RadioSettings(63, 127, 4096, 512, 126, 255, 254, 253, true),
                new PhoneSettings(300, 12, true),
                new LineSettings(64, 32))));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("configs")
  void everySettingKeepsItsPlaceInTheSyncPayload(HardwiredConfig config) {
    assertEquals(FIELDS, new HashSet<>(fields(config)).size());
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    SyncServerConfigS2CPayload.CODEC.encode(buffer, new SyncServerConfigS2CPayload(config));
    assertEquals(fields(config), fields(SyncServerConfigS2CPayload.CODEC.decode(buffer).config()));
    assertEquals(0, buffer.readableBytes());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("configs")
  void everySettingKeepsItsPlaceInTheEditPayload(HardwiredConfig config) {
    assertEquals(FIELDS, new HashSet<>(fields(config)).size());
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    EditServerConfigC2SPayload.CODEC.encode(buffer, new EditServerConfigC2SPayload(config));
    assertEquals(config, EditServerConfigC2SPayload.CODEC.decode(buffer).config());
    assertEquals(0, buffer.readableBytes());
  }
}
