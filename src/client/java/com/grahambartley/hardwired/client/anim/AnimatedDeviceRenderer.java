package com.grahambartley.hardwired.client.anim;

import com.grahambartley.hardwired.device.AnimatedDevice;
import com.grahambartley.hardwired.device.DialDevice;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.RotationAxis;

public final class AnimatedDeviceRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

  private static final float PIXEL = 1F / 16F;
  private static final float CENTER = 0.5F;
  private static final float TICKS_PER_SECOND = 20F;
  private static final int PHASE_SPREAD = 64;

  private final BlockModelRenderer models;

  public AnimatedDeviceRenderer(BlockEntityRendererFactory.Context context) {
    this.models = context.getRenderManager().getModelRenderer();
  }

  @Override
  public boolean rendersOutsideBoundingBox(T entity) {
    DeviceAnimations.Animation animation =
        DeviceAnimations.of(Registries.BLOCK.getId(entity.getCachedState().getBlock()));
    return animation != null && animation.wide();
  }

  @Override
  public void render(
      T entity,
      float tickDelta,
      MatrixStack matrices,
      VertexConsumerProvider buffers,
      int light,
      int overlay) {
    BlockState state = entity.getCachedState();
    DeviceAnimations.Animation animation =
        DeviceAnimations.of(Registries.BLOCK.getId(state.getBlock()));
    if (animation == null || entity.getWorld() == null) {
      return;
    }
    DialDevice dials = entity instanceof DialDevice device ? device : null;
    boolean animating =
        dials != null
            ? dials.isMoving()
            : state.getBlock() instanceof AnimatedDevice device && device.isAnimating(state);
    float seconds =
        dials != null
            ? DevicePhase.seconds(entity, dials.clipSpeed(), tickDelta)
            : animating ? clock(entity, tickDelta) : 0;
    VertexConsumer consumer = buffers.getBuffer(TexturedRenderLayers.getEntityCutout());
    DeviceTurn turn = DeviceTurn.of(state, animation.upright());
    matrices.push();
    matrices.translate(CENTER, CENTER, CENTER);
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-turn.y() - animation.yaw()));
    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-turn.x()));
    matrices.translate(-CENTER, -CENTER, -CENTER);
    for (DeviceAnimations.Part part : animation.parts()) {
      if (!part.drawnWhile(animating) || !part.shownIn(state)) {
        continue;
      }
      DeviceClip.Pose pose =
          part.dial() != null && dials != null
              ? dialPose(part.dial(), dials, tickDelta)
              : moving(animating, dials, part, state)
                  ? animation.clip().sample(part.bone(), seconds)
                  : DeviceClip.Pose.REST;
      matrices.push();
      float unturn = part.dial() != null && part.dial().world() ? turn.y() + animation.yaw() : 0F;
      apply(matrices, part.pivot(), pose, unturn);
      BakedModel model =
          MinecraftClient.getInstance().getBakedModelManager().getModel(part.model());
      models.render(
          matrices.peek(), consumer, state, model, 1F, 1F, 1F, part.light(light), overlay);
      matrices.pop();
    }
    matrices.pop();
  }

  private static boolean moving(
      boolean animating, DialDevice dials, DeviceAnimations.Part part, BlockState state) {
    return (animating || dials != null) && part.movesIn(state);
  }

  private static DeviceClip.Pose dialPose(
      DeviceAnimations.Dial dial, DialDevice dials, float tickDelta) {
    float[] rotation = new float[3];
    rotation[dial.axis() - 'x'] = dial.degrees(dials.dial(dial.value(), tickDelta));
    return new DeviceClip.Pose(rotation, new float[3]);
  }

  private static float clock(BlockEntity entity, float tickDelta) {
    int phase = Math.floorMod(entity.getPos().hashCode(), PHASE_SPREAD);
    return (entity.getWorld().getTime() % 24000L + phase + tickDelta) / TICKS_PER_SECOND;
  }

  private static void apply(
      MatrixStack matrices, float[] pivot, DeviceClip.Pose pose, float unturn) {
    float[] position = pose.position();
    float[] rotation = pose.rotation();
    matrices.translate(
        (position[0] + pivot[0]) * PIXEL,
        (position[1] + pivot[1]) * PIXEL,
        (position[2] + pivot[2]) * PIXEL);
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(unturn));
    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation[2]));
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation[1]));
    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotation[0]));
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-unturn));
    matrices.translate(-pivot[0] * PIXEL, -pivot[1] * PIXEL, -pivot[2] * PIXEL);
  }
}
