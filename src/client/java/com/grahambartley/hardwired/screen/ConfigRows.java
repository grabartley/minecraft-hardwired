package com.grahambartley.hardwired.screen;

import java.util.function.Consumer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

final class ConfigRows {
  static final int CONTENT_WIDTH = 220;
  private static final int WIDGET_WIDTH = 200;
  private static final int WIDGET_HEIGHT = 20;
  private static final int HEADER_HEIGHT = 12;

  private final boolean canEdit;
  private final TextRenderer textRenderer;

  ConfigRows(final boolean canEdit, final TextRenderer textRenderer) {
    this.canEdit = canEdit;
    this.textRenderer = textRenderer;
  }

  ClickableWidget header(final String key) {
    return new ConfigSectionHeader(
        CONTENT_WIDTH, HEADER_HEIGHT, Text.translatable(key), this.textRenderer);
  }

  ConfigIntSlider intSlider(
      final String labelKey,
      final String tooltipKey,
      final int min,
      final int max,
      final int initial,
      final Consumer<Integer> sink) {
    return withTooltip(
        new ConfigIntSlider(
            WIDGET_WIDTH, WIDGET_HEIGHT, Text.translatable(labelKey), "", min, max, initial, sink),
        Text.translatable(tooltipKey));
  }

  ClickableWidget percentSlider(
      final Text label,
      final Text tooltip,
      final int min,
      final int max,
      final int initial,
      final Consumer<Integer> sink) {
    return withTooltip(
        new ConfigIntSlider(WIDGET_WIDTH, WIDGET_HEIGHT, label, "%", min, max, initial, sink),
        tooltip);
  }

  ClickableWidget percentSlider(
      final String labelKey,
      final int min,
      final int max,
      final int initial,
      final Consumer<Integer> sink) {
    return percentSlider(
        Text.translatable(labelKey),
        Text.translatable(labelKey + ".tooltip"),
        min,
        max,
        initial,
        sink);
  }

  private <W extends ClickableWidget> W withTooltip(final W widget, final Text tooltip) {
    widget.setTooltip(Tooltip.of(tooltip));
    widget.active = this.canEdit;
    return widget;
  }
}
