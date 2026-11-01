package com.grahambartley.hardwired.client.glow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.grahambartley.hardwired.light.Glow;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GlowMeshTest {

  private static final int RED = 0xFF0000;
  private static final int PER_FACE = GlowMesh.FLOATS_PER_VERTEX * 4;

  private record Surfaces(Predicate<BlockPos> solid, List<Box> parts, float daylight)
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
    public float daylight(BlockPos pos) {
      return daylight;
    }
  }

  private static Surfaces floorAtMinusOne(float daylight) {
    return new Surfaces(pos -> pos.getY() < 0, List.of(), daylight);
  }

  private static int faces(GlowMesh mesh) {
    return mesh.vertices().size() / PER_FACE;
  }

  private static float strongest(GlowMesh mesh) {
    FloatArrayList vertices = mesh.vertices();
    float top = 0F;
    for (int i = GlowMesh.STRENGTH_OFFSET; i < vertices.size(); i += GlowMesh.FLOATS_PER_VERTEX) {
      top = Math.max(top, vertices.getFloat(i));
    }
    return top;
  }

  @Test
  void noGlowsMeansTheSharedEmptyMesh() {
    assertSame(GlowMesh.EMPTY, GlowMesh.build(List.of(), floorAtMinusOne(0F)));
  }

  @Test
  void openAirHasNothingToTint() {
    Surfaces air = new Surfaces(pos -> false, List.of(), 0F);
    assertEquals(0, faces(GlowMesh.build(List.of(new Glow(BlockPos.ORIGIN, 15, RED)), air)));
  }

  @ParameterizedTest(name = "level {0} tints {1} faces of one block")
  @CsvSource({"2,1", "1,1"})
  void lightOnlyTintsTheSideItCanReach(int level, int expected) {
    Surfaces block = new Surfaces(pos -> pos.equals(new BlockPos(0, 0, 1)), List.of(), 0F);
    GlowMesh mesh = GlowMesh.build(List.of(new Glow(BlockPos.ORIGIN, level, RED)), block);
    assertEquals(expected, faces(mesh));
  }

  @Test
  void faceSitsJustOffTheBlockTowardsTheLight() {
    Surfaces block = new Surfaces(pos -> pos.equals(new BlockPos(0, 0, 1)), List.of(), 0F);
    GlowMesh mesh = GlowMesh.build(List.of(new Glow(BlockPos.ORIGIN, 2, RED)), block);
    for (int i = 2; i < mesh.vertices().size(); i += GlowMesh.FLOATS_PER_VERTEX) {
      assertEquals(0.998F, mesh.vertices().getFloat(i), 0.0001F);
    }
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
    assertEquals(1F, mesh.vertices().getFloat(3), 0.001F);
    assertEquals(0F, mesh.vertices().getFloat(4), 0.001F);
    assertEquals(0F, mesh.vertices().getFloat(5), 0.001F);
  }

  @Test
  void partBlockAddsEveryFaceExceptTheOneOnTheFloor() {
    Predicate<BlockPos> floor = pos -> pos.getY() < -1;
    Box carpet = new Box(0, 0, 0, 1, 0.0625, 1);
    Glow glow = new Glow(BlockPos.ORIGIN, 15, RED);
    int without = faces(GlowMesh.build(List.of(glow), new Surfaces(floor, List.of(), 0F)));
    int with = faces(GlowMesh.build(List.of(glow), new Surfaces(floor, List.of(carpet), 0F)));
    assertEquals(without + 5, with);
  }
}
