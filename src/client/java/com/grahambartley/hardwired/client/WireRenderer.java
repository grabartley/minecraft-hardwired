package com.grahambartley.hardwired.client;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.ClientDisplaySettings;
import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.grahambartley.hardwired.item.WireItem;
import com.grahambartley.hardwired.network.WireChunkPayload;
import com.grahambartley.hardwired.network.WireChunkPayload.CellView;
import com.grahambartley.hardwired.network.WireChunkPayload.MarkView;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireFace;
import com.grahambartley.hardwired.wire.WireTopology;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class WireRenderer {

  private static final Identifier ATLAS =
      Identifier.of(Hardwired.MOD_ID, "textures/wire/wire_view.png");
  private static final RenderLayer ON_SURFACE = RenderLayer.getText(ATLAS);
  private static final RenderLayer THROUGH_BLOCKS = RenderLayer.getTextSeeThrough(ATLAS);
  private static final int TILES = 4;
  private static final int NODE_ON = 0;
  private static final int ARM_ON = 1;
  private static final int NODE_OFF = 2;
  private static final int ARM_OFF = 3;
  private static final int LOOSE_END = 4;
  private static final int TRIPPED = 5;
  private static final int MARK_INPUT = 6;
  private static final int MARK_OUTPUT = 7;
  private static final float SURFACE_OFFSET = 0.012F;
  private static final int WHITE = 0xFFFFFF;
  private static final int WAX_AMBER = 0xE8A020;
  private static final float WAX_SCALE = 2.0F;

  private WireRenderer() {}

  public static void render(WorldRenderContext context) {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client.player == null || !WireItem.isHolding(client.player)) {
      return;
    }
    Vec3d camera = context.camera().getPos();
    MatrixStack matrices = new MatrixStack();
    matrices.translate(-camera.x, -camera.y, -camera.z);
    Matrix4f matrix = matrices.peek().getPositionMatrix();
    VertexConsumerProvider.Immediate buffers =
        client.getBufferBuilders().getEntityVertexConsumers();
    VertexConsumer through = buffers.getBuffer(THROUGH_BLOCKS);
    ClientDisplaySettings settings = ClientSettingsStore.get();
    drawAll(through, matrix, settings.throughBlocksAlpha(), true);
    buffers.draw(THROUGH_BLOCKS);
    VertexConsumer surface = buffers.getBuffer(ON_SURFACE);
    drawAll(surface, matrix, settings.onSurfaceAlpha(), false);
    buffers.draw(ON_SURFACE);
  }

  private static void drawAll(
      VertexConsumer consumer, Matrix4f matrix, int alpha, boolean bothSides) {
    for (WireChunkPayload chunk : WireViewStore.chunks()) {
      double[] lanes = WireLanes.offsets(chunk.cells());
      for (int index = 0; index < lanes.length; index++) {
        drawCell(consumer, matrix, chunk.cells().get(index), lanes[index], alpha, bothSides);
      }
      if (ClientSettingsStore.get().showMarkers()) {
        for (MarkView mark : chunk.marks()) {
          drawMark(consumer, matrix, mark, alpha);
        }
      }
    }
  }

  private static void drawCell(
      VertexConsumer consumer,
      Matrix4f matrix,
      CellView view,
      double lane,
      int alpha,
      boolean bothSides) {
    WireFace face = new WireFace(BlockPos.fromLong(view.pos()), Direction.byId(view.side()));
    boolean on = (view.flags() & WireChunkPayload.ON) != 0;
    int argb = alpha | Hue.byIndex(view.color()).rgb();
    Vec3d center = faceCenter(face).add(WireLanes.shift(face.side(), lane));
    Direction reference = WireTopology.edges(face.side()).get(0);
    quad(
        consumer, matrix, center, face.side(), reference, on ? NODE_ON : NODE_OFF, argb, bothSides);
    for (Direction arm : WireTopology.edges(face.side())) {
      if (view.hasArm(arm) && (!on || (view.flags() & WireChunkPayload.POWERED) != 0)) {
        quad(consumer, matrix, center, face.side(), arm, on ? ARM_ON : ARM_OFF, argb, bothSides);
      }
    }
    if ((view.flags() & WireChunkPayload.TRIPPED) != 0) {
      quad(consumer, matrix, center, face.side(), reference, TRIPPED, alpha | WHITE, bothSides);
    } else if ((view.flags() & WireChunkPayload.LOOSE_END) != 0) {
      quad(consumer, matrix, center, face.side(), reference, LOOSE_END, alpha | WHITE, bothSides);
    }
    if (view.isSealed()) {
      quad(
          consumer,
          matrix,
          center,
          face.side(),
          reference,
          NODE_ON,
          alpha | WAX_AMBER,
          bothSides,
          WAX_SCALE);
    }
  }

  private static void drawMark(VertexConsumer consumer, Matrix4f matrix, MarkView mark, int alpha) {
    Vec3d center = Vec3d.ofCenter(BlockPos.fromLong(mark.pos()));
    int tile = mark.input() ? MARK_INPUT : MARK_OUTPUT;
    for (Direction side : Direction.values()) {
      Vec3d onFace = center.add(Vec3d.of(side.getVector()).multiply(0.5 + SURFACE_OFFSET));
      quad(
          consumer,
          matrix,
          onFace,
          side,
          WireTopology.edges(side).get(0),
          tile,
          alpha | WHITE,
          false);
    }
  }

  private static Vec3d faceCenter(WireFace face) {
    return Vec3d.ofCenter(face.pos())
        .add(Vec3d.of(face.side().getVector()).multiply(0.5 + SURFACE_OFFSET));
  }

  private static void quad(
      VertexConsumer consumer,
      Matrix4f matrix,
      Vec3d center,
      Direction normal,
      Direction up,
      int tile,
      int argb,
      boolean bothSides) {
    quad(consumer, matrix, center, normal, up, tile, argb, bothSides, 1.0F);
  }

  private static void quad(
      VertexConsumer consumer,
      Matrix4f matrix,
      Vec3d center,
      Direction normal,
      Direction up,
      int tile,
      int argb,
      boolean bothSides,
      float scale) {
    Vec3d upward = Vec3d.of(up.getVector()).multiply(0.5 * scale);
    Vec3d right = Vec3d.of(normal.getVector()).crossProduct(upward);
    float u0 = (tile % TILES) / (float) TILES;
    float v0 = (tile / TILES) / (float) TILES;
    float size = 1F / TILES;
    Vec3d topLeft = center.add(upward).subtract(right);
    Vec3d topRight = center.add(upward).add(right);
    Vec3d bottomRight = center.subtract(upward).add(right);
    Vec3d bottomLeft = center.subtract(upward).subtract(right);
    vertex(consumer, matrix, topLeft, u0, v0, argb);
    vertex(consumer, matrix, bottomLeft, u0, v0 + size, argb);
    vertex(consumer, matrix, bottomRight, u0 + size, v0 + size, argb);
    vertex(consumer, matrix, topRight, u0 + size, v0, argb);
    if (bothSides) {
      vertex(consumer, matrix, topRight, u0 + size, v0, argb);
      vertex(consumer, matrix, bottomRight, u0 + size, v0 + size, argb);
      vertex(consumer, matrix, bottomLeft, u0, v0 + size, argb);
      vertex(consumer, matrix, topLeft, u0, v0, argb);
    }
  }

  private static void vertex(
      VertexConsumer consumer, Matrix4f matrix, Vec3d pos, float u, float v, int argb) {
    consumer
        .vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
        .color(argb)
        .texture(u, v)
        .light(LightmapTextureManager.MAX_LIGHT_COORDINATE);
  }
}
