package com.grahambartley.hardwired.client.radio;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class TalkKey {

  private static KeyBinding binding;

  private TalkKey() {}

  public static void register() {
    binding =
        KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.hardwired.talk",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.categories.hardwired"));
  }

  public static boolean matches(int keyCode, int scanCode) {
    return binding.matchesKey(keyCode, scanCode);
  }
}
