package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WireCellTest {

  private static final WireFace FACE = new WireFace(new BlockPos(1, 2, 3), Direction.UP);
  private static final WireFace OTHER = new WireFace(new BlockPos(4, 2, 3), Direction.UP);

  private static Stream<Arguments> owners() {
    return Stream.of(Arguments.of(null, false), Arguments.of(new UUID(1, 2), true));
  }

  @ParameterizedTest(name = "owner {0}")
  @MethodSource("owners")
  void movingToAnotherFaceKeepsColourAndOwner(UUID owner, boolean sealed) {
    WireCell moved = new WireCell(FACE, Hue.RED, owner).onFace(OTHER);
    assertEquals(new WireCell(OTHER, Hue.RED, owner), moved);
    assertEquals(sealed, moved.isSealed());
  }

  @ParameterizedTest(name = "owner {0}")
  @MethodSource("owners")
  void aSealedAndAnOpenCellOnOneFaceAreDifferentCells(UUID owner, boolean sealed) {
    WireCell open = new WireCell(FACE, Hue.RED);
    WireCell owned = open.ownedBy(owner);
    if (sealed) {
      assertNotEquals(open, owned);
    } else {
      assertEquals(open, owned);
    }
  }
}
