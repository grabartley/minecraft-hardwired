package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.device.IntercomLine;
import com.grahambartley.hardwired.network.IntercomSayPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public final class IntercomScreen extends Screen {

  private static final int WIDTH = 280;
  private static final int HEIGHT = 84;
  private static final int PADDING = 12;
  private static final int TITLE_Y = 12;
  private static final int FIELD_Y = 26;
  private static final int BUTTON_Y = 52;
  private static final int ROW_HEIGHT = 20;

  private final BlockPos pos;
  private TextFieldWidget field;

  public IntercomScreen(BlockPos pos) {
    super(Text.translatable("block.hardwired.intercom"));
    this.pos = pos;
  }

  private int left() {
    return (width - WIDTH) / 2;
  }

  private int top() {
    return (height - HEIGHT) / 2;
  }

  @Override
  protected void init() {
    int inner = WIDTH - 2 * PADDING;
    Text prompt = Text.translatable("screen.hardwired.intercom.prompt");
    field =
        new TextFieldWidget(
            textRenderer, left() + PADDING, top() + FIELD_Y, inner, ROW_HEIGHT, prompt);
    field.setMaxLength(IntercomLine.MAX_LENGTH);
    field.setPlaceholder(prompt);
    addDrawableChild(field);
    setInitialFocus(field);
    addDrawableChild(
        ButtonWidget.builder(Text.translatable("screen.hardwired.intercom.send"), b -> send())
            .dimensions(left() + PADDING, top() + BUTTON_Y, inner, ROW_HEIGHT)
            .build());
  }

  private void send() {
    if (!IntercomLine.clean(field.getText()).isEmpty()) {
      ClientPlayNetworking.send(new IntercomSayPayload(pos, field.getText()));
    }
    close();
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
      send();
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderBackground(context, mouseX, mouseY, delta);
    SlatePlate.draw(context, left(), top(), WIDTH, HEIGHT);
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);
    context.drawCenteredTextWithShadow(
        textRenderer, title, width / 2, top() + TITLE_Y, SlatePlate.TITLE_COLOR);
  }

  @Override
  public boolean shouldPause() {
    return false;
  }
}
