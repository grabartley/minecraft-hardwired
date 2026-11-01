package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.radio.Frequency;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

final class TuningRow {

  private static final int[] STEPS = {-100, -10, 10, 100};
  private static final String[] LABELS = {"<<", "<", ">", ">>"};
  private static final int GAP = 2;

  private TuningRow() {}

  static void add(
      Consumer<ButtonWidget> into,
      int x,
      int y,
      int width,
      int height,
      IntSupplier frequency,
      Consumer<Integer> tune) {
    int button = (width - (STEPS.length - 1) * GAP) / STEPS.length;
    for (int i = 0; i < STEPS.length; i++) {
      int step = STEPS[i];
      into.accept(
          ButtonWidget.builder(
                  Text.literal(LABELS[i]),
                  pressed -> tune.accept(Frequency.snap(frequency.getAsInt() + step)))
              .dimensions(x + i * (button + GAP), y, button, height)
              .build());
    }
  }
}
