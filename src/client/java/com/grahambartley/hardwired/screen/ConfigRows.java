package com.grahambartley.hardwired.screen;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
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

  ClickableWidget toggle(
      final String labelKey, final boolean initial, final Consumer<Boolean> sink) {
    return withTooltip(
        CyclingButtonWidget.onOffBuilder(initial)
            .build(
                0,
                0,
                WIDGET_WIDTH,
                WIDGET_HEIGHT,
                Text.translatable(labelKey),
                (button, value) -> sink.accept(value)),
        Text.translatable(labelKey + ".tooltip"));
  }

  ClickableWidget choice(
      final String labelKey,
      final List<String> values,
      final String initial,
      final Function<String, Text> labels,
      final Consumer<String> sink) {
    return withTooltip(
        CyclingButtonWidget.builder(labels)
            .values(values)
            .initially(initial)
            .build(
                0,
                0,
                WIDGET_WIDTH,
                WIDGET_HEIGHT,
                Text.translatable(labelKey),
                (button, value) -> sink.accept(value)),
        Text.translatable(labelKey + ".tooltip"));
  }

  private <W extends ClickableWidget> W withTooltip(final W widget, final Text tooltip) {
    widget.setTooltip(Tooltip.of(tooltip));
    widget.active = this.canEdit;
    return widget;
  }
}
