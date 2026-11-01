package com.grahambartley.hardwired.client.glow;

import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
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

  public static void register() {
    GlowShader.register();
    WorldRenderEvents.LAST.register(GlowRenderer::render);
  }

  public static void clear() {
    TRACKER.clear();
  }

  private static void render(WorldRenderContext context) {
    ClientWorld world = context.world();
    if (world == null || !ClientSettingsStore.get().colouredLight()) {
      return;
    }
    Vec3d camera = context.camera().getPos();
    GlowMesh mesh = TRACKER.mesh(world, BlockPos.ofFloored(camera));
    if (mesh.isEmpty()) {
      return;
    }
    RenderSystem.enableBlend();
    RenderSystem.depthMask(false);
    RenderSystem.enableDepthTest();
    RenderSystem.depthFunc(GL11.GL_LEQUAL);
    RenderSystem.disableCull();
    RenderSystem.setShader(GlowShader::program);
    RenderSystem.blendFuncSeparate(
        GlStateManager.SrcFactor.DST_COLOR,
        GlStateManager.DstFactor.ONE,
        GlStateManager.SrcFactor.ZERO,
        GlStateManager.DstFactor.ONE);
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
    BlockPos anchor = mesh.anchor();
    float offsetX = (float) (anchor.getX() - camera.x);
    float offsetY = (float) (anchor.getY() - camera.y);
    float offsetZ = (float) (anchor.getZ() - camera.z);
    BufferBuilder buffer =
        Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
    mesh.forEachVertex(
        (x, y, z, red, green, blue, strength) ->
            buffer
                .vertex(x + offsetX, y + offsetY, z + offsetZ)
                .color(
                    GlowColor.blend(red, strength, removing),
                    GlowColor.blend(green, strength, removing),
                    GlowColor.blend(blue, strength, removing),
                    1F));
    BuiltBuffer built = buffer.endNullable();
    if (built != null) {
      BufferRenderer.drawWithGlobalProgram(built);
    }
  }
}
