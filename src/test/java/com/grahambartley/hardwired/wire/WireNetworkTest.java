package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WireNetworkTest {

  private static WireNetwork network() {
    return new WireNetwork(Hue.PLAIN, null);
  }

  private static BlockPos at(int y) {
    return new BlockPos(1, y, 1);
  }

  @ParameterizedTest(name = "touched at y {0}")
  @ValueSource(ints = {5, 6})
  void linkedCellsSpanTheWholeDevice(int touched) {
    WireNetwork network = network();
    network.link(at(touched).asLong(), List.of(at(5), at(6)));
    assertEquals(
        new LongOpenHashSet(new long[] {at(5).asLong(), at(6).asLong()}), network.linkedCells());
  }

  @Test
  void droppingTheTouchDropsItsCells() {
    WireNetwork network = network();
    network.link(at(6).asLong(), List.of(at(5), at(6)));
    network.link(at(9).asLong(), List.of(at(9)));
    network.links().remove(at(6).asLong());
    assertEquals(new LongOpenHashSet(new long[] {at(9).asLong()}), network.linkedCells());
  }

  @ParameterizedTest(name = "{0} links")
  @ValueSource(ints = {0, 1, 3})
  void linksSurviveBeingPacked(int count) {
    WireNetwork network = network();
    for (int i = 0; i < count; i++) {
      network.link(at(i * 10).asLong(), List.of(at(i * 10), at(i * 10 + 1)));
    }
    WireNetwork restored = network();
    restored.unpackLinks(network.packedLinks());
    assertEquals(network.linkedCells(), restored.linkedCells());
    assertEquals(network.links().keySet(), restored.links().keySet());
  }

  @Test
  void aSplitPartInheritsEveryLink() {
    WireNetwork whole = network();
    long[] cells = {at(5).asLong()};
    whole.link(at(5).asLong(), List.of(at(5)));
    WireNetwork part = network();
    part.inheritPower(whole);
    assertArrayEquals(cells, part.links().get(at(5).asLong()));
  }
}
