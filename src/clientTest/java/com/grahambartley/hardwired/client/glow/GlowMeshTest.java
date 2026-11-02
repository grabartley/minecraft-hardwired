package com.grahambartley.hardwired.client.glow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.grahambartley.hardwired.light.Glow;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GlowMeshTest {

  private static final int RED = 0xFF0000;

  private record Surfaces(
      Predicate<BlockPos> solid, List<Box> parts, float daylight, Direction hidden)
      implements GlowSurfaces {
    @Override
    public boolean isSolid(BlockPos pos) {
      return solid.test(pos);
    }

    @Override
    public List<Box> partBoxes(BlockPos pos) {
      return pos.equals(new BlockPos(0, -1, 0)) ? parts : List.of();
    }

    @Override
    public boolean showsSide(BlockPos pos, Direction side) {
      return side != hidden;
    }

    @Override
    public int cost(BlockPos from, Direction side) {
      return solid.test(from.offset(side)) ? Glow.MAX_LEVEL + 1 : 1;
    }

    @Override
    public float daylight(BlockPos pos) {
      return daylight;
    }
  }

  private static Surfaces floorAtMinusOne(float daylight) {
    return new Surfaces(pos -> pos.getY() < 0, List.of(), daylight, null);
  }

  private record Vertex(
      float x, float y, float z, float red, float green, float blue, float strength) {}

  private static List<Vertex> vertices(GlowMesh mesh) {
    List<Vertex> out = new ArrayList<>();
    mesh.forEachVertex(
        (x, y, z, red, green, blue, strength) ->
            out.add(new Vertex(x, y, z, red, green, blue, strength)));
    return out;
  }

  private static int faces(GlowMesh mesh) {
    return vertices(mesh).size() / 4;
  }

  private static float strongest(GlowMesh mesh) {
    return (float) vertices(mesh).stream().mapToDouble(Vertex::strength).max().orElse(0D);
  }

  @Test
  void noGlowsMeansTheSharedEmptyMesh() {
    assertSame(GlowMesh.EMPTY, GlowMesh.build(List.of(), floorAtMinusOne(0F)));
  }

  @Test
  void openAirHasNothingToTint() {
    Surfaces air = new Surfaces(pos -> false, List.of(), 0F, null);
    assertEquals(0, faces(GlowMesh.build(List.of(new Glow(BlockPos.ORIGIN, 15, RED)), air)));
  }

  @ParameterizedTest(name = "level {0} tints {1} faces of one block")
  @CsvSource({"2,1", "1,1"})
  void lightOnlyTintsTheSideItCanReach(int level, int expected) {
    Surfaces block = new Surfaces(pos -> pos.equals(new BlockPos(0, 0, 1)), List.of(), 0F, null);
    GlowMesh mesh = GlowMesh.build(List.of(new Glow(BlockPos.ORIGIN, level, RED)), block);
    assertEquals(expected, faces(mesh));
  }

  @Test
  void faceSitsJustOffTheBlockTowardsTheLight() {
    Surfaces block = new Surfaces(pos -> pos.equals(new BlockPos(0, 0, 1)), List.of(), 0F, null);
    GlowMesh mesh = GlowMesh.build(List.of(new Glow(BlockPos.ORIGIN, 2, RED)), block);
    assertFalse(vertices(mesh).isEmpty());
    vertices(mesh).forEach(vertex -> assertEquals(0.99F, vertex.z(), 0.0001F));
  }

  @ParameterizedTest(name = "daylight {0} keeps strength at most {1}")
  @CsvSource({"0,1", "0.5,0.5", "1,0"})
  void daylightWashesTheColourOut(float daylight, float limit) {
    GlowMesh mesh =
        GlowMesh.build(List.of(new Glow(BlockPos.ORIGIN, 15, RED)), floorAtMinusOne(daylight));
    assertEquals(limit, strongest(mesh), 0.02F);
  }

  @Test
  void floorColourIsTheLightsColour() {
    GlowMesh mesh =
        GlowMesh.build(List.of(new Glow(BlockPos.ORIGIN, 15, RED)), floorAtMinusOne(0F));
    Vertex first = vertices(mesh).get(0);
    assertEquals(1F, first.red(), 0.001F);
    assertEquals(0F, first.green(), 0.001F);
    assertEquals(0F, first.blue(), 0.001F);
  }

  @Test
  void partBlockAddsEveryFaceExceptTheOneOnTheFloor() {
    Predicate<BlockPos> floor = pos -> pos.getY() < -1;
    Box carpet = new Box(0, 0, 0, 1, 0.0625, 1);
    Glow glow = new Glow(BlockPos.ORIGIN, 15, RED);
    int without = faces(GlowMesh.build(List.of(glow), new Surfaces(floor, List.of(), 0F, null)));
    int with = faces(GlowMesh.build(List.of(glow), new Surfaces(floor, List.of(carpet), 0F, null)));
    assertEquals(without + 5, with);
  }

  @Test
  void hiddenSideOfAPartBlockIsSkipped() {
    Predicate<BlockPos> floor = pos -> pos.getY() < -1;
    Box carpet = new Box(0, 0, 0, 1, 0.0625, 1);
    Glow glow = new Glow(BlockPos.ORIGIN, 15, RED);
    int shown =
        faces(GlowMesh.build(List.of(glow), new Surfaces(floor, List.of(carpet), 0F, null)));
    int hidden =
        faces(
            GlowMesh.build(
                List.of(glow), new Surfaces(floor, List.of(carpet), 0F, Direction.NORTH)));
    assertEquals(shown - 1, hidden);
  }

  @Test
  void faceInsideTheCellStaysEvenWhenTheNeighbourHidesThatSide() {
    Predicate<BlockPos> floor = pos -> pos.getY() < -1;
    Box lowerStep = new Box(0, 0, 0, 1, 0.5, 1);
    Glow glow = new Glow(BlockPos.ORIGIN, 15, RED);
    int shown =
        faces(GlowMesh.build(List.of(glow), new Surfaces(floor, List.of(lowerStep), 0F, null)));
    int upHidden =
        faces(
            GlowMesh.build(
                List.of(glow), new Surfaces(floor, List.of(lowerStep), 0F, Direction.UP)));
    assertEquals(shown, upHidden);
  }
}
