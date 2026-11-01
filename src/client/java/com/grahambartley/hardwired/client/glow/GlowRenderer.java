package com.grahambartley.hardwired.client.glow;

import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public final class GlowRenderer {

  private static final GlowTracker TRACKER = new GlowTracker();

  private GlowRenderer() {}

  public static void render(WorldRenderContext context) {
    ClientWorld world = context.world();
    if (world == null || !ClientSettingsStore.get().colouredLight()) {
      return;
    }
    Vec3d camera = context.camera().getPos();
    GlowMesh mesh = TRACKER.mesh(world, BlockPos.ofFloored(camera));
    if (mesh.vertices().isEmpty()) {
      return;
    }
    RenderSystem.enableBlend();
    RenderSystem.depthMask(false);
    RenderSystem.enableDepthTest();
    RenderSystem.depthFunc(GL11.GL_LEQUAL);
    RenderSystem.disableCull();
    RenderSystem.setShader(GameRenderer::getPositionColorProgram);
    RenderSystem.blendFunc(GlStateManager.SrcFactor.DST_COLOR, GlStateManager.DstFactor.ONE);
    RenderSystem.blendEquation(GL14.GL_FUNC_REVERSE_SUBTRACT);
    draw(mesh, camera, true);
    RenderSystem.blendEquation(GL14.GL_FUNC_ADD);
    draw(mesh, camera, false);
    RenderSystem.enableCull();
    RenderSystem.depthMask(true);
    RenderSystem.defaultBlendFunc();
    RenderSystem.disableBlend();
  }

  private static void draw(GlowMesh mesh, Vec3d camera, boolean removing) {
    FloatArrayList vertices = mesh.vertices();
    BlockPos anchor = mesh.anchor();
    float offsetX = (float) (anchor.getX() - camera.x);
    float offsetY = (float) (anchor.getY() - camera.y);
    float offsetZ = (float) (anchor.getZ() - camera.z);
    BufferBuilder buffer =
        Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
    for (int index = 0; index < vertices.size(); index += GlowMesh.FLOATS_PER_VERTEX) {
      float strength = vertices.getFloat(index + GlowMesh.STRENGTH_OFFSET);
      buffer
          .vertex(
              vertices.getFloat(index) + offsetX,
              vertices.getFloat(index + 1) + offsetY,
              vertices.getFloat(index + 2) + offsetZ)
          .color(
              GlowColor.blend(vertices.getFloat(index + 3), strength, removing),
              GlowColor.blend(vertices.getFloat(index + 4), strength, removing),
              GlowColor.blend(vertices.getFloat(index + 5), strength, removing),
              1F);
    }
    BuiltBuffer built = buffer.endNullable();
    if (built != null) {
      BufferRenderer.drawWithGlobalProgram(built);
    }
  }
}
