package com.grahambartley.hardwired.client.line;

import com.grahambartley.hardwired.wire.WireViewRange;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class SpanRenderer {

  private static final int SEGMENTS = 24;
  private static final double HALF_WIDTH = 0.03;
  private static final double HALF_PIXEL_PER_BLOCK = 0.0008;
  private static final float SIDE_SHADE = 0.75F;

  private SpanRenderer() {}

  public static void render(WorldRenderContext context) {
    ClientWorld world = context.world();
    if (world == null || ClientSpans.drawn().isEmpty() || context.frustum() == null) {
      return;
    }
    Vec3d camera = context.camera().getPos();
    double range =
        WireViewRange.blocksIn(MinecraftClient.getInstance().options.getClampedViewDistance());
    BufferBuilder buffer =
        Tessellator.getInstance()
            .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_LIGHT);
    for (ClientSpans.Drawn span : ClientSpans.drawn()) {
      Box box = new Box(span.curve().low(), span.curve().high()).expand(HALF_WIDTH);
      if (box.squaredMagnitude(camera) <= range * range && context.frustum().isVisible(box)) {
        draw(buffer, world, span, camera);
      }
    }
    BuiltBuffer built = buffer.endNullable();
    if (built == null) {
      return;
    }
    MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
    RenderSystem.enableDepthTest();
    RenderSystem.disableCull();
    RenderSystem.setShader(GameRenderer::getPositionColorLightmapProgram);
    BufferRenderer.drawWithGlobalProgram(built);
    RenderSystem.enableCull();
    MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
  }

  private static void draw(
      BufferBuilder buffer, ClientWorld world, ClientSpans.Drawn span, Vec3d camera) {
    int rgb = span.rgb();
    SpanCurve curve = span.curve();
    Vec3d along = curve.to().subtract(curve.from());
    Vec3d across = new Vec3d(-along.z, 0, along.x).normalize();
    for (int step = 0; step < SEGMENTS; step++) {
      Vec3d start = curve.at((double) step / SEGMENTS).subtract(camera);
      Vec3d end = curve.at((double) (step + 1) / SEGMENTS).subtract(camera);
      double half = halfWidth(start.add(end).multiply(0.5).length());
      Vec3d flat = across.multiply(half);
      Vec3d up = new Vec3d(0, half, 0);
      int light =
          WorldRenderer.getLightmapCoordinates(
              world, BlockPos.ofFloored(curve.at((step + 0.5) / SEGMENTS)));
      ribbon(buffer, start, end, flat, rgb, 1F, light);
      ribbon(buffer, start, end, up, rgb, SIDE_SHADE, light);
    }
  }

  static double halfWidth(double distance) {
    return Math.max(HALF_WIDTH, distance * HALF_PIXEL_PER_BLOCK);
  }

  private static void ribbon(
      BufferBuilder buffer, Vec3d start, Vec3d end, Vec3d half, int rgb, float shade, int light) {
    float red = (rgb >> 16 & 0xFF) / 255F * shade;
    float green = (rgb >> 8 & 0xFF) / 255F * shade;
    float blue = (rgb & 0xFF) / 255F * shade;
    vertex(buffer, start.add(half), red, green, blue, light);
    vertex(buffer, start.subtract(half), red, green, blue, light);
    vertex(buffer, end.subtract(half), red, green, blue, light);
    vertex(buffer, end.add(half), red, green, blue, light);
  }

  private static void vertex(
      BufferBuilder buffer, Vec3d at, float red, float green, float blue, int light) {
    buffer
        .vertex((float) at.x, (float) at.y, (float) at.z)
        .color(red, green, blue, 1F)
        .light(light);
  }
}
