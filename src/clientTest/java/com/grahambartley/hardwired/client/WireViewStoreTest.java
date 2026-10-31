package com.grahambartley.hardwired.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.network.WireChunkPayload;
import com.grahambartley.hardwired.network.WireChunkPayload.CellView;
import com.grahambartley.hardwired.wire.WireFace;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class WireViewStoreTest {

  private static final BlockPos HOST = new BlockPos(3, 64, 3);

  private static void store() {
    WireViewStore.put(
        new WireChunkPayload(
            0L, List.of(new CellView(HOST.asLong(), Direction.NORTH.getId(), 0, 0)), List.of()));
  }

  @AfterEach
  void clear() {
    WireViewStore.clear();
  }

  @Test
  void aWireOnTheHitFaceIsFound() {
    store();
    assertEquals(
        Optional.of(new WireFace(HOST, Direction.NORTH)),
        WireViewStore.wiredFaceAt(HOST, Direction.NORTH));
  }

  @Test
  void aWireHiddenBehindAPlacedBlockIsFound() {
    store();
    assertEquals(
        Optional.of(new WireFace(HOST, Direction.NORTH)),
        WireViewStore.wiredFaceAt(HOST.north(), Direction.NORTH));
  }

  @Test
  void anEmptyChunkUpdateRemovesTheChunk() {
    store();
    WireViewStore.put(new WireChunkPayload(0L, List.of(), List.of()));
    assertTrue(WireViewStore.chunks().isEmpty());
  }
}
