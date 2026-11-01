package com.grahambartley.hardwired.client;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.LiftFloorBlock;
import com.grahambartley.hardwired.block.LiftFloorBlockEntity;
import com.grahambartley.hardwired.lift.LiftDial;
import java.util.OptionalDouble;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

public final class LiftFloorRenderer implements BlockEntityRenderer<LiftFloorBlockEntity> {

  private static final Identifier NEEDLE =
      Identifier.of(Hardwired.MOD_ID, "block/lift_floor_needle");
  private static final Identifier MARK = Identifier.of(Hardwired.MOD_ID, "block/lift_floor_mark");
  private static final Identifier MARK_LIT =
      Identifier.of(Hardwired.MOD_ID, "block/lift_floor_mark_lit");
  private static final Identifier LEAF = Identifier.of(Hardwired.MOD_ID, "block/lift_gate_leaf");
  private static final Identifier WIDE_LEAF =
      Identifier.of(Hardwired.MOD_ID, "block/lift_gate_leaf_wide");
  private static final float PIXEL = 1F / 16F;
  private static final float HALF = 0.5F;
  private static final float PIVOT_Y = 12.25F * PIXEL;
  private static final float HALL_FACE = 3.15F * PIXEL;
  private static final float SHAFT_FACE = -0.15F * PIXEL;
  private static final float BEHIND = 180F;
  private static final float HINGE = PIXEL;
  private static final float GATE_MIDDLE = 1.5F * PIXEL;
  private static final int RENDER_DISTANCE = 96;

  public LiftFloorRenderer(BlockEntityRendererFactory.Context context) {}

  public static void registerModels() {
    ModelLoadingPlugin.register(
        context -> context.addModels(NEEDLE, MARK, MARK_LIT, LEAF, WIDE_LEAF));
  }

  @Override
  public boolean rendersOutsideBoundingBox(LiftFloorBlockEntity gate) {
    return true;
  }

  @Override
  public int getRenderDistance() {
    return RENDER_DISTANCE;
  }

  @Override
  public void render(
      LiftFloorBlockEntity gate,
      float tickDelta,
      MatrixStack matrices,
      VertexConsumerProvider buffers,
      int light,
      int overlay) {
    LiftDial dial = gate.dial();
    double time = gate.getWorld().getTime() + (double) tickDelta;
    double level = dial.levelAt(time);
    float needle = LiftDial.degrees(dial.reading(level));
    OptionalDouble lit = dial.litDegrees(level);
    BlockState state = gate.getCachedState();
    LiftFloorBlock floor = (LiftFloorBlock) state.getBlock();
    Direction facing = state.get(LiftFloorBlock.FACING);
    VertexConsumer vertices = buffers.getBuffer(TexturedRenderLayers.getEntityCutout());
    matrices.push();
    matrices.translate(HALF, 0, HALF);
    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(facing.asRotation() + BEHIND));
    matrices.translate(-HALF, 0, -HALF);
    float pivotX = floor.dialPivotX();
    drawFace(matrices, vertices, dial, needle, lit, pivotX, HALL_FACE, 0, light);
    drawFace(matrices, vertices, dial, needle, lit, pivotX, SHAFT_FACE, BEHIND, light);
    float width = gate.swing().width(state.get(LiftFloorBlock.OPEN), time);
    Identifier leaf = floor.leafModel();
    drawLeaf(matrices, vertices, leaf, HINGE, 0, width, light);
    drawLeaf(matrices, vertices, leaf, 2 * pivotX - HINGE, BEHIND, width, light);
    matrices.pop();
  }

  private static void drawFace(
      MatrixStack matrices,
      VertexConsumer vertices,
      LiftDial dial,
      float needle,
      OptionalDouble lit,
      float pivotX,
      float depth,
      float turn,
      int light) {
    matrices.push();
    matrices.translate(pivotX, PIVOT_Y, depth);
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(turn));
    for (float mark : dial.markDegrees()) {
      drawAt(matrices, vertices, MARK, mark, light);
    }
    if (lit.isPresent()) {
      drawAt(
          matrices,
          vertices,
          MARK_LIT,
          (float) lit.getAsDouble(),
          LightmapTextureManager.MAX_LIGHT_COORDINATE);
    }
    drawAt(matrices, vertices, NEEDLE, needle, light);
    matrices.pop();
  }

  private static void drawLeaf(
      MatrixStack matrices,
      VertexConsumer vertices,
      Identifier leaf,
      float hingeX,
      float turn,
      float width,
      int light) {
    matrices.push();
    matrices.translate(hingeX, -1F, GATE_MIDDLE);
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(turn));
    matrices.scale(width, 1F, 1F);
    matrices.translate(0, 0, -GATE_MIDDLE);
    draw(matrices, vertices, leaf, light);
    matrices.pop();
  }

  private static void drawAt(
      MatrixStack matrices, VertexConsumer vertices, Identifier id, float degrees, int light) {
    matrices.push();
    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(degrees));
    matrices.translate(-HALF, -HALF, -HALF);
    draw(matrices, vertices, id, light);
    matrices.pop();
  }

  private static void draw(
      MatrixStack matrices, VertexConsumer vertices, Identifier id, int light) {
    MinecraftClient client = MinecraftClient.getInstance();
    BakedModel model = client.getBakedModelManager().getModel(id);
    client
        .getBlockRenderManager()
        .getModelRenderer()
        .render(
            matrices.peek(), vertices, null, model, 1F, 1F, 1F, light, OverlayTexture.DEFAULT_UV);
  }
}
