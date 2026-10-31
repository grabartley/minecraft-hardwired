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
  private static final int OPAQUE = 0xFF000000;
  private static final int TITLE_COLOR = 0xFFFDF55F;
  private static final int SELECTED_COLOR = 0xFFFFFFFF;
  private static final int BRASS_SHADOW = 0xFF8A5A0C;
  private static final int BRASS = 0xFFF5C21C;
  private static final int SLATE = 0xFF2A2632;
  private static final int RIVET = 0xFFBE7E12;
  private static final int RIVET_INSET = 6;
  private static final int RIVET_SIZE = 2;

  private final BlockPos pos;
  private final ControlPanelBlockEntity panel;
  private LampLook look;

  public ControlPanelScreen(BlockPos pos, ControlPanelBlockEntity panel) {
    super(Text.translatable("block.hardwired.control_panel"));
    this.pos = pos;
    this.panel = panel;
    this.look = panel.look();
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
    if (slot < panel.scenes().size()) {
      name.setText(panel.scenes().get(slot).name());
    }
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
                button -> {
                  if (slot < panel.scenes().size()) {
                    setLook(panel.scenes().get(slot).look());
                  }
                })
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
    int right = left() + WIDTH;
    int bottom = top() + HEIGHT;
    context.fill(left(), top(), right, bottom, BRASS_SHADOW);
    context.fill(left() + 1, top() + 1, right - 1, bottom - 1, BRASS);
    context.fill(left() + 3, top() + 3, right - 3, bottom - 3, SLATE);
    for (int x : new int[] {left() + RIVET_INSET, right - RIVET_INSET - RIVET_SIZE}) {
      for (int y : new int[] {top() + RIVET_INSET, bottom - RIVET_INSET - RIVET_SIZE}) {
        context.fill(x, y, x + RIVET_SIZE, y + RIVET_SIZE, RIVET);
      }
    }
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);
    context.drawText(textRenderer, title, left() + PADDING + 4, top() + 9, TITLE_COLOR, false);
    Text selected = Text.translatable(look.color().lampKey());
    context.drawText(
        textRenderer,
        selected,
        left() + WIDTH - PADDING - textRenderer.getWidth(selected),
        top() + 8,
        TITLE_COLOR,
        false);
    for (Hue hue : Hue.values()) {
      int x = left() + PADDING + hue.ordinal() % SWATCH_COLUMNS * (SWATCH + 6);
      int y = top() + 22 + hue.ordinal() / SWATCH_COLUMNS * (SWATCH + 2);
      if (hue == look.color()) {
        context.fill(x + 2, y + 2, x + SWATCH - 2, y + SWATCH - 2, SELECTED_COLOR);
      }
      context.fill(x + 4, y + 4, x + SWATCH - 4, y + SWATCH - 4, OPAQUE | hue.lampRgb());
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
