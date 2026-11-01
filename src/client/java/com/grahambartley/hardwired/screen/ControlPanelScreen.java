package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.block.ControlPanelBlockEntity;
import com.grahambartley.hardwired.network.PanelActionPayload;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.LampLook;
import com.grahambartley.hardwired.wire.Scene;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class ControlPanelScreen extends Screen {

  private static final int WIDTH = 260;
  private static final int HEIGHT = 236;
  private static final int SWATCH = 20;
  private static final int SWATCH_COLUMNS = 9;
  private static final int ROW = 16;
  private static final int PADDING = 10;
  private static final int SELECTED_COLOR = 0xFFFFFFFF;

  private final BlockPos pos;
  private final ControlPanelBlockEntity panel;
  private LampLook look;

  public ControlPanelScreen(BlockPos pos, ControlPanelBlockEntity panel, LampLook look) {
    super(Text.translatable("block.hardwired.control_panel"));
    this.pos = pos;
    this.panel = panel;
    this.look = look;
  }

  private int left() {
    return (width - WIDTH) / 2;
  }

  private int top() {
    return (height - HEIGHT) / 2;
  }

  @Override
  protected void init() {
    for (Hue hue : Hue.values()) {
      int column = hue.ordinal() % SWATCH_COLUMNS;
      int row = hue.ordinal() / SWATCH_COLUMNS;
      addDrawableChild(
          ButtonWidget.builder(
                  Text.empty(), button -> setLook(new LampLook(hue, look.brightness())))
              .dimensions(
                  left() + PADDING + column * (SWATCH + 6),
                  top() + 22 + row * (SWATCH + 2),
                  SWATCH,
                  SWATCH)
              .tooltip(
                  net.minecraft.client.gui.tooltip.Tooltip.of(Text.translatable(hue.lampKey())))
              .build());
    }
    addDrawableChild(new BrightnessSlider(left() + PADDING, top() + 70, WIDTH - 2 * PADDING));
    for (int slot = 0; slot < Scene.MAX_SCENES; slot++) {
      addSceneRow(slot);
    }
  }

  private void addSceneRow(int slot) {
    int y = top() + 98 + slot * ROW;
    TextFieldWidget name =
        new TextFieldWidget(
            textRenderer,
            left() + PADDING,
            y,
            130,
            ROW - 2,
            Text.translatable("screen.hardwired.scene_name"));
    name.setMaxLength(Scene.MAX_NAME_LENGTH);
    Scene.at(panel.scenes(), slot).ifPresent(scene -> name.setText(scene.name()));
    addDrawableChild(name);
    addDrawableChild(
        ButtonWidget.builder(
                Text.translatable("screen.hardwired.save"),
                button ->
                    ClientPlayNetworking.send(
                        new PanelActionPayload(pos, look, slot, name.getText())))
            .dimensions(left() + 146, y, 46, ROW - 2)
            .build());
    addDrawableChild(
        ButtonWidget.builder(
                Text.translatable("screen.hardwired.recall"),
                button -> Scene.at(panel.scenes(), slot).ifPresent(scene -> setLook(scene.look())))
            .dimensions(left() + 196, y, 54, ROW - 2)
            .build());
  }

  private void setLook(LampLook newLook) {
    look = newLook;
    ClientPlayNetworking.send(new PanelActionPayload(pos, look, PanelActionPayload.NO_SLOT, ""));
  }

  @Override
  public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderBackground(context, mouseX, mouseY, delta);
    SlatePlate.draw(context, left(), top(), WIDTH, HEIGHT);
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);
    PanelText.flat(
        context,
        textRenderer,
        title,
        left() + PADDING + 4,
        left() + WIDTH / 2,
        top() + 9,
        SlatePlate.TITLE_COLOR,
        PanelText.Align.LEFT);
    PanelText.flat(
        context,
        textRenderer,
        Text.translatable(look.color().lampKey()),
        left() + WIDTH / 2 + 4,
        left() + WIDTH - PADDING,
        top() + 8,
        SlatePlate.TITLE_COLOR,
        PanelText.Align.RIGHT);
    for (Hue hue : Hue.values()) {
      int x = left() + PADDING + hue.ordinal() % SWATCH_COLUMNS * (SWATCH + 6);
      int y = top() + 22 + hue.ordinal() / SWATCH_COLUMNS * (SWATCH + 2);
      if (hue == look.color()) {
        context.fill(x + 2, y + 2, x + SWATCH - 2, y + SWATCH - 2, SELECTED_COLOR);
      }
      context.fill(x + 4, y + 4, x + SWATCH - 4, y + SWATCH - 4, SlatePlate.OPAQUE | hue.lampRgb());
    }
  }

  @Override
  public boolean shouldPause() {
    return false;
  }

  private final class BrightnessSlider extends SliderWidget {

    private BrightnessSlider(int x, int y, int width) {
      super(x, y, width, 20, Text.empty(), look.brightness() / (double) LampLook.MAX_BRIGHTNESS);
      updateMessage();
    }

    private int brightness() {
      return (int) Math.round(value * LampLook.MAX_BRIGHTNESS);
    }

    @Override
    protected void updateMessage() {
      setMessage(Text.translatable("screen.hardwired.brightness", brightness()));
    }

    @Override
    protected void applyValue() {
      setLook(new LampLook(look.color(), brightness()));
    }
  }
}
