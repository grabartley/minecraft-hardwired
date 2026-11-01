package com.grahambartley.hardwired.client.glow;

import com.grahambartley.hardwired.Hardwired;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

final class GlowShader {

  private static ShaderProgram program;

  private GlowShader() {}

  static void register() {
    CoreShaderRegistrationCallback.EVENT.register(
        context ->
            context.register(
                Identifier.of(Hardwired.MOD_ID, "cast_light"),
                VertexFormats.POSITION_COLOR,
                loaded -> program = loaded));
  }

  static ShaderProgram program() {
    return program;
  }
}
