package com.grahambartley.hardwired.client;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.lift.LiftCarEntity;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public final class LiftCarRenderer extends EntityRenderer<LiftCarEntity> {

  private static final Identifier SMALL = Identifier.of(Hardwired.MOD_ID, "block/lift_car");
  private static final Identifier LARGE = Identifier.of(Hardwired.MOD_ID, "block/lift_car_large");
  private static final float RIGHT_ANGLE = 90F;
  private static final float ABOVE_PIT_FLOOR = 1F / 16F;

  private final Identifier modelId;
  private final float halfWidth;

  private LiftCarRenderer(
      EntityRendererFactory.Context context, Identifier modelId, float halfWidth) {
    super(context);
    this.modelId = modelId;
    this.halfWidth = halfWidth;
  }

  public static LiftCarRenderer small(EntityRendererFactory.Context context) {
    return new LiftCarRenderer(context, SMALL, 0.5F);
  }

  public static LiftCarRenderer large(EntityRendererFactory.Context context) {
    return new LiftCarRenderer(context, LARGE, 1F);
  }

  public static void registerModels() {
    ModelLoadingPlugin.register(context -> context.addModels(SMALL, LARGE));
  }

  @Override
  public void render(
      LiftCarEntity car,
      float yaw,
      float tickDelta,
      MatrixStack matrices,
      VertexConsumerProvider buffers,
      int light) {
    MinecraftClient client = MinecraftClient.getInstance();
    BakedModel model = client.getBakedModelManager().getModel(modelId);
    matrices.push();
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(RIGHT_ANGLE * car.quarterTurns()));
    matrices.translate(-halfWidth, LiftCarEntity.PLATE + ABOVE_PIT_FLOOR, -halfWidth);
    client
        .getBlockRenderManager()
        .getModelRenderer()
        .render(
            matrices.peek(),
            buffers.getBuffer(TexturedRenderLayers.getEntityCutout()),
            null,
            model,
            1F,
            1F,
            1F,
            light,
            OverlayTexture.DEFAULT_UV);
    matrices.pop();
    super.render(car, yaw, tickDelta, matrices, buffers, light);
  }

  @Override
  public Identifier getTexture(LiftCarEntity car) {
    return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
  }
}
