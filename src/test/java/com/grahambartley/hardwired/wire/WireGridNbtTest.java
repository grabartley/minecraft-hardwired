package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class WireGridNbtTest {

  private static final UUID STRANGER = new UUID(9, 9);

  private static WireCell cell(int x, Hue hue) {
    return new WireCell(new WireFace(new BlockPos(x, 70, -3), Direction.NORTH), hue);
  }

  @ParameterizedTest
  @EnumSource(Hue.class)
  void everyLinkKeepsItsCellsThroughARoundTrip(Hue hue) {
    WireGrid grid = new WireGrid();
    WireNetwork network = grid.add(cell(0, hue));
    BlockPos lower = new BlockPos(1, 60, 1);
    network.link(lower.up().asLong(), List.of(lower, lower.up()));

    WireNetwork restored = WireGridNbt.read(WireGridNbt.write(grid)).networkOf(cell(0, hue));

    assertEquals(network.linkedCells(), restored.linkedCells());
    assertEquals(network.links().keySet(), restored.links().keySet());
  }

  @ParameterizedTest
  @EnumSource(Hue.class)
  void everyFieldSurvivesARoundTrip(Hue hue) {
    WireGrid grid = new WireGrid();
    WireNetwork red = grid.add(cell(0, hue));
    grid.add(cell(1, hue));
    red.setOn(true);
    red.setTripped(true);
    red.setLook(new LampLook(Hue.CYAN, 9));
    red.rememberedInputs().add(1234L);
    grid.add(cell(5, hue == Hue.BLUE ? Hue.RED : Hue.BLUE));

    WireGrid read = WireGridNbt.read(WireGridNbt.write(grid));

    assertEquals(2, read.networks().size());
    WireNetwork restored = read.networkOf(cell(1, hue));
    assertEquals(2, restored.size());
    assertTrue(restored.isOn());
    assertTrue(restored.isTripped());
    assertEquals(new LampLook(Hue.CYAN, 9), restored.look());
    assertTrue(restored.rememberedInputs().contains(1234L));
    assertEquals(List.of(), read.drainDirty());
  }

  @Test
  void lookStampsSurviveARoundTripAndKeepCountingUp() {
    WireGrid grid = new WireGrid();
    WireNetwork network = grid.add(cell(0, Hue.RED));
    network.setLook(new LampLook(Hue.CYAN, 9));
    network.stampLook(grid.nextLookStamp());
    grid.nextLookStamp();

    WireGrid read = WireGridNbt.read(WireGridNbt.write(grid));

    assertEquals(1L, read.networkOf(cell(0, Hue.RED)).lookStamp());
    assertEquals(3L, read.nextLookStamp());
  }

  @ParameterizedTest(name = "owner {0}")
  @MethodSource("owners")
  void ownerSurvivesARoundTrip(UUID owner) {
    WireGrid grid = new WireGrid();
    grid.add(cell(0, Hue.RED).ownedBy(owner));
    grid.add(cell(1, Hue.RED).ownedBy(owner));
    grid.add(cell(0, Hue.RED).ownedBy(STRANGER));

    WireGrid read = WireGridNbt.read(WireGridNbt.write(grid));

    assertEquals(2, read.networks().size());
    WireNetwork restored = read.networkOf(cell(1, Hue.RED).ownedBy(owner));
    assertEquals(2, restored.size());
    assertEquals(owner, restored.owner());
  }

  private static Stream<UUID> owners() {
    return Stream.of(null, new UUID(1, 2));
  }

  @Test
  void aSaveWithoutOwnersLoadsAsOpenWire() {
    NbtCompound network = new NbtCompound();
    network.putString("color", Hue.RED.asString());
    network.putLongArray("positions", new long[] {cell(0, Hue.RED).face().pos().asLong()});
    network.putByteArray("sides", new byte[] {(byte) Direction.NORTH.getId()});
    NbtList networks = new NbtList();
    networks.add(network);
    NbtCompound root = new NbtCompound();
    root.put("networks", networks);

    WireGrid read = WireGridNbt.read(root);

    assertFalse(read.networkOf(cell(0, Hue.RED)).isSealed());
  }
}
