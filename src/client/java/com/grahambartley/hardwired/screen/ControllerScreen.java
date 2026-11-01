package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.block.ControllerBlockEntity;
import com.grahambartley.hardwired.controller.ControllerProgram;
import com.grahambartley.hardwired.controller.TimelineStep;
import com.grahambartley.hardwired.network.ControllerSetPayload;
import com.grahambartley.hardwired.wire.Hue;
import java.util.Optional;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class ControllerScreen extends Screen {

  private static final int WIDTH = 300;
  private static final int HEIGHT = 234;
  private static final int PADDING = 10;
  private static final int CELL = 14;
  private static final int SWATCH = 14;
  private static final int SMALL_BUTTON = 16;
  private static final int WAIT_STEP_TICKS = 2;
  private static final int REVERSE_STEP_TICKS = 20;
  private static final double TICKS_PER_SECOND = 20.0;
  private static final int CURSOR_COLOR = 0x60F5C21C;
  private static final int SELECTED_COLOR = 0xFFFFFFFF;
  private static final int OFF_MARK_INSET = 5;

  private final BlockPos pos;
  private final ControllerBlockEntity controller;
  private final ControllerProgram original;
  private final ControllerDraft draft;
  private int firstStep;
  private int selected;

  public ControllerScreen(BlockPos pos, ControllerBlockEntity controller) {
    super(Text.translatable("block.hardwired.controller"));
    this.pos = pos;
    this.controller = controller;
    this.original = controller.program();
    this.draft = new ControllerDraft(original);
  }

  private int left() {
    return (width - WIDTH) / 2;
  }

  private int top() {
    return (height - HEIGHT) / 2;
  }

  private TimelineGrid grid() {
    return new TimelineGrid(left() + 2 * PADDING + CELL + 2, top() + 78, CELL);
  }

  private static int cycleDirection() {
    return hasShiftDown() ? -1 : 1;
  }

  @Override
  protected void init() {
    Tooltip hint = Tooltip.of(Text.translatable("screen.hardwired.controller.cycle_hint"));
    addDrawableChild(
        ButtonWidget.builder(Text.empty(), button -> draft.cycleOpen(cycleDirection()))
            .dimensions(left() + PADDING, top() + 22, SWATCH + 2, SWATCH + 2)
            .tooltip(hint)
            .build());
    addDrawableChild(
        ButtonWidget.builder(Text.empty(), button -> draft.cycleClose(cycleDirection()))
            .dimensions(left() + WIDTH / 2, top() + 22, SWATCH + 2, SWATCH + 2)
            .tooltip(hint)
            .build());
    addSmall("-", left() + PADDING, top() + 42, () -> draft.adjustAutoReverse(-REVERSE_STEP_TICKS));
    addSmall(
        "+",
        left() + PADDING + SMALL_BUTTON + 2,
        top() + 42,
        () -> draft.adjustAutoReverse(REVERSE_STEP_TICKS));
    addSmall("<", left() + WIDTH - 2 * SMALL_BUTTON - PADDING - 2, top() + 42, () -> page(-1));
    addSmall(">", left() + WIDTH - SMALL_BUTTON - PADDING, top() + 42, () -> page(1));
    int bottom = top() + HEIGHT - 24;
    addSmall("-", left() + PADDING, bottom, () -> draft.adjustWait(selected, -WAIT_STEP_TICKS));
    addSmall(
        "+",
        left() + PADDING + SMALL_BUTTON + 2,
        bottom,
        () -> draft.adjustWait(selected, WAIT_STEP_TICKS));
    addDrawableChild(
        ButtonWidget.builder(
                Text.translatable("screen.hardwired.controller.add_step"), button -> addStep())
            .dimensions(left() + WIDTH - 158, bottom, 72, SMALL_BUTTON)
            .build());
    addDrawableChild(
        ButtonWidget.builder(
                Text.translatable("screen.hardwired.controller.remove_step"),
                button -> removeStep())
            .dimensions(left() + WIDTH - 84, bottom, 74, SMALL_BUTTON)
            .build());
  }

  private void addSmall(String label, int x, int y, Runnable action) {
    addDrawableChild(
        ButtonWidget.builder(Text.literal(label), button -> action.run())
            .dimensions(x, y, SMALL_BUTTON, SMALL_BUTTON)
            .build());
  }

  private void page(int direction) {
    int next = firstStep + direction * TimelineGrid.VISIBLE_STEPS;
    if (next >= 0 && next < Math.max(1, draft.steps().size())) {
      firstStep = next;
    }
  }

  private void addStep() {
    if (draft.addStep()) {
      selected = draft.steps().size() - 1;
      firstStep = TimelineGrid.pageStart(selected);
    }
  }

  private void removeStep() {
    draft.removeStep(selected);
    selected = Math.clamp(selected, 0, Math.max(0, draft.steps().size() - 1));
    firstStep = TimelineGrid.pageStart(selected);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (super.mouseClicked(mouseX, mouseY, button)) {
      return true;
    }
    TimelineGrid grid = grid();
    int steps = draft.steps().size();
    var cell = grid.cellAt(mouseX, mouseY, firstStep, steps);
    if (cell.isPresent()) {
      selected = cell.get().step();
      draft.toggle(cell.get().step(), cell.get().slot());
      return true;
    }
    var header = grid.headerAt(mouseX, mouseY, firstStep, steps);
    if (header.isPresent()) {
      selected = header.getAsInt();
      return true;
    }
    var label = grid.labelAt(mouseX, mouseY);
    if (label.isPresent()) {
      draft.cycleOutput(label.getAsInt(), cycleDirection());
      return true;
    }
    return false;
  }

  @Override
  public void removed() {
    ControllerProgram edited = draft.program();
    if (!edited.equals(original) && ClientPlayNetworking.canSend(ControllerSetPayload.ID)) {
      ClientPlayNetworking.send(new ControllerSetPayload(pos, edited));
    }
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
    drawReads(context);
    drawGrid(context);
    drawFooter(context);
  }

  private void drawReads(DrawContext context) {
    int x = left() + PADDING;
    int y = top() + 22;
    int middle = left() + WIDTH / 2;
    int right = left() + WIDTH - PADDING;
    drawHueRead(context, "screen.hardwired.controller.open", draft.open(), x, middle, y);
    drawHueRead(context, "screen.hardwired.controller.close", draft.close(), middle, right, y);
    PanelText.flat(
        context,
        textRenderer,
        Text.translatable(
            "screen.hardwired.controller.auto_reverse", seconds(draft.autoReverseTicks())),
        x + 2 * SMALL_BUTTON + 6,
        right - 2 * SMALL_BUTTON - 6,
        top() + 46,
        SlatePlate.TITLE_COLOR,
        PanelText.Align.LEFT);
  }

  private void drawHueRead(DrawContext context, String key, Hue hue, int x, int right, int y) {
    context.fill(x + 3, y + 3, x + SWATCH - 1, y + SWATCH - 1, SlatePlate.OPAQUE | hue.rgb());
    Text label = Text.translatable(key).append(": ").append(Text.translatable(hue.wireKey()));
    PanelText.flat(
        context,
        textRenderer,
        label,
        x + SWATCH + 6,
        right - 2,
        y + 4,
        SlatePlate.TITLE_COLOR,
        PanelText.Align.LEFT);
  }

  private void drawGrid(DrawContext context) {
    TimelineGrid grid = grid();
    int steps = draft.steps().size();
    int lastShown = Math.min(steps, firstStep + TimelineGrid.VISIBLE_STEPS);
    for (int slot = 0; slot < TimelineStep.OUTPUT_SLOTS; slot++) {
      drawOutputLabel(context, grid, slot);
      for (int step = firstStep; step < lastShown; step++) {
        drawCell(context, grid, step, slot);
      }
    }
    for (int step = firstStep; step < lastShown; step++) {
      drawHeader(context, grid, step);
    }
    int running = controller.run().position() - 1;
    if (controller.run().isRunning() && running >= firstStep && running < lastShown) {
      int x = grid.columnX(running, firstStep);
      context.fill(x, grid.y(), x + CELL, grid.rowY(TimelineStep.OUTPUT_SLOTS), CURSOR_COLOR);
    }
  }

  private void drawOutputLabel(DrawContext context, TimelineGrid grid, int slot) {
    int x = grid.labelX();
    int y = grid.rowY(slot);
    Optional<Hue> hue = draft.output(slot);
    context.fill(x, y, x + CELL - 1, y + CELL - 1, SlatePlate.BRASS_SHADOW);
    if (hue.isPresent()) {
      context.fill(x + 1, y + 1, x + CELL - 2, y + CELL - 2, SlatePlate.OPAQUE | hue.get().rgb());
    } else {
      context.fill(x + 1, y + 1, x + CELL - 2, y + CELL - 2, SlatePlate.SLATE);
    }
  }

  private void drawCell(DrawContext context, TimelineGrid grid, int step, int slot) {
    int x = grid.columnX(step, firstStep);
    int y = grid.rowY(slot);
    context.fill(x, y, x + CELL - 1, y + CELL - 1, SlatePlate.SLATE_LIGHT);
    if (draft.steps().get(step).isOn(slot)) {
      context.fill(x + 2, y + 2, x + CELL - 3, y + CELL - 3, SlatePlate.BRASS);
    } else {
      int low = OFF_MARK_INSET;
      int high = CELL - 1 - OFF_MARK_INSET;
      context.fill(x + low, y + low, x + high, y + high, SlatePlate.SLATE);
    }
  }

  private void drawHeader(DrawContext context, TimelineGrid grid, int step) {
    int x = grid.columnX(step, firstStep);
    String number = Integer.toString(step + 1);
    int color = step == selected ? SELECTED_COLOR : SlatePlate.MUTED_COLOR;
    context.drawText(
        textRenderer,
        number,
        x + (CELL - 1 - textRenderer.getWidth(number)) / 2,
        grid.headerY() + 3,
        color,
        false);
  }

  private void drawFooter(DrawContext context) {
    int steps = draft.steps().size();
    Text info =
        steps == 0
            ? Text.translatable("screen.hardwired.controller.no_steps")
            : Text.translatable(
                "screen.hardwired.controller.step",
                selected + 1,
                steps,
                seconds(draft.steps().get(selected).waitTicks()));
    PanelText.flat(
        context,
        textRenderer,
        info,
        left() + PADDING,
        left() + WIDTH - PADDING,
        top() + HEIGHT - 37,
        SlatePlate.TITLE_COLOR,
        PanelText.Align.LEFT);
    if (steps > TimelineGrid.VISIBLE_STEPS) {
      Text shown =
          Text.translatable(
              "screen.hardwired.controller.steps_shown",
              firstStep + 1,
              Math.min(steps, firstStep + TimelineGrid.VISIBLE_STEPS),
              steps);
      PanelText.flat(
          context,
          textRenderer,
          shown,
          left() + WIDTH / 2 + 4,
          left() + WIDTH - PADDING,
          top() + 9,
          SlatePlate.MUTED_COLOR,
          PanelText.Align.RIGHT);
    }
  }

  private static Text seconds(int ticks) {
    return ticks == 0
        ? Text.translatable("screen.hardwired.controller.off")
        : Text.translatable(
            "screen.hardwired.controller.seconds",
            String.format(java.util.Locale.ROOT, "%.1f", ticks / TICKS_PER_SECOND));
  }

  @Override
  public boolean shouldPause() {
    return false;
  }
}
