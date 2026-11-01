package com.grahambartley.hardwired.client;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.LargeLiftFloorBlock;
import com.grahambartley.hardwired.block.LiftFloorBlock;
import com.grahambartley.hardwired.block.LiftFloorBlockEntity;
import com.grahambartley.hardwired.lift.LiftDial;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.MinecraftClient;
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
  private static final float PIXEL = 1F / 16F;
  private static final float HALF = 0.5F;
  private static final float PIVOT_X = 8F * PIXEL;
  private static final float PIVOT_Y = 12.25F * PIXEL;
  private static final float HALL_FACE = 3.15F * PIXEL;
  private static final float SHAFT_FACE = -0.15F * PIXEL;
  private static final float BEHIND = 180F;

  public LiftFloorRenderer(BlockEntityRendererFactory.Context context) {}

  public static void registerModels() {
    ModelLoadingPlugin.register(context -> context.addModels(NEEDLE, MARK));
  }

  @Override
  public boolean rendersOutsideBoundingBox(LiftFloorBlockEntity gate) {
    return true;
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
    float needle = LiftDial.degrees(dial.reading(dial.levelAt(time)));
    Direction facing = gate.getCachedState().get(LiftFloorBlock.FACING);
    VertexConsumer vertices = buffers.getBuffer(TexturedRenderLayers.getEntityCutout());
    matrices.push();
    matrices.translate(HALF, 0, HALF);
    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(facing.asRotation() + BEHIND));
    matrices.translate(-HALF, 0, -HALF);
    float pivotX = gate.getCachedState().getBlock() instanceof LargeLiftFloorBlock ? 1F : PIVOT_X;
    drawFace(matrices, vertices, dial, needle, pivotX, HALL_FACE, 0, light);
    drawFace(matrices, vertices, dial, needle, pivotX, SHAFT_FACE, BEHIND, light);
    matrices.pop();
  }

  private static void drawFace(
      MatrixStack matrices,
      VertexConsumer vertices,
      LiftDial dial,
      float needle,
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
    drawAt(matrices, vertices, NEEDLE, needle, light);
    matrices.pop();
  }

  private static void drawAt(
      MatrixStack matrices, VertexConsumer vertices, Identifier id, float degrees, int light) {
    MinecraftClient client = MinecraftClient.getInstance();
    BakedModel model = client.getBakedModelManager().getModel(id);
    matrices.push();
    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(degrees));
    matrices.translate(-HALF, -HALF, -HALF);
    client
        .getBlockRenderManager()
        .getModelRenderer()
        .render(
            matrices.peek(), vertices, null, model, 1F, 1F, 1F, light, OverlayTexture.DEFAULT_UV);
    matrices.pop();
  }
}
