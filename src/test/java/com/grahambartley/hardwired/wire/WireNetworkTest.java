package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

  @ParameterizedTest(name = "keep top touch {0}, keep other touch {1}")
  @CsvSource({"true,true,3,2", "false,true,1,1", "true,false,2,1", "false,false,0,0"})
  void factsLiveAndDieWithTheirLinks(boolean keepTop, boolean keepOther, int linked, int loads) {
    long top = at(6).asLong();
    long other = at(9).asLong();
    WireNetwork network = network();
    network.link(top, List.of(at(5), at(6)));
    network.link(other, List.of(at(9)));
    network.lastLoads().put(at(5).asLong(), 4);
    network.lastLoads().put(other, 2);
    network.lastRatings().put(other, 16);
    network.lastSources().put(other, 1);
    network.masters().add(top);
    network.keepFacts(pos -> pos == top ? keepTop : keepOther);
    assertEquals(linked, network.linkedCells().size());
    assertEquals(loads, network.lastLoads().size());
    assertEquals(keepOther ? 1 : 0, network.lastRatings().size());
    assertEquals(keepOther ? 1 : 0, network.lastSources().size());
    assertEquals(keepTop ? 1 : 0, network.masters().size());
  }

  @Test
  void aLinkKeptForOneTouchHoldsACellSharedWithACutTouch() {
    long lower = at(5).asLong();
    long upper = at(6).asLong();
    WireNetwork network = network();
    network.link(lower, List.of(at(5), at(6)));
    network.link(upper, List.of(at(5), at(6)));
    network.lastLoads().put(lower, 2);
    network.keepFacts(pos -> pos == upper);
    assertEquals(2, network.lastLoads().get(lower));
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
    assertEquals(network.links().keySet(), restored.links().keySet());
    network
        .links()
        .forEach(
            (touched, cells) -> assertArrayEquals(cells, restored.links().get((long) touched)));
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
