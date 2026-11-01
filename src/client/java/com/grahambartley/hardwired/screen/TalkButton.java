package com.grahambartley.hardwired.screen;

import net.minecraft.text.Text;

final class TalkButton extends RedButton {

  private final Runnable hold;

  TalkButton(int x, int y, int width, int height, Text label, Runnable hold) {
    super(x, y, width, height, label, pressed -> {});
    this.hold = hold;
  }

  @Override
  public void onClick(double mouseX, double mouseY) {
    hold.run();
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    return false;
  }
}
