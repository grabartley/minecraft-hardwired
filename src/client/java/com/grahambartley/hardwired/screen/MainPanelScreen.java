package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.device.PanelAction;
import com.grahambartley.hardwired.device.PanelKind;
import com.grahambartley.hardwired.device.PanelRow;
import com.grahambartley.hardwired.network.MainPanelActionPayload;
import com.grahambartley.hardwired.network.MainPanelViewPayload;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.LampLook;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class MainPanelScreen extends Screen {

  private static final int WIDTH = 320;
  private static final int HEIGHT = 236;
  private static final int PADDING = 10;
  private static final int ROW = 24;
  private static final int ROWS_TOP = 22;
  private static final int STATE_X = 128;
  private static final int DIAL_X = 186;
  private static final int CONTROLS_X = 204;
  private static final int BUTTON_HEIGHT = 16;
  private static final int WIDE_BUTTON = 60;
  private static final int SMALL_BUTTON = 16;
  private static final int DIAL_RADIUS = 9;
  private static final int PAGE_DIAL_RADIUS = 7;
  private static final int MARK = 6;
  private static final int REFRESH_TICKS = 20;
  private static final int ON_COLOR = 0xFFFFE58A;

  private final BlockPos panel;
  private List<PanelRow> rows;
  private int unknownPoints;
  private int omitted;
  private int page;
  private int ticks;

  public MainPanelScreen(MainPanelViewPayload view) {
    super(Text.translatable("block.hardwired.main_panel"));
    this.panel = view.panel();
    this.rows = view.rows();
    this.unknownPoints = view.unknownPoints();
    this.omitted = view.omitted();
  }

  public boolean shows(BlockPos pos) {
    return panel.equals(pos);
  }

  public void update(MainPanelViewPayload view) {
    boolean changed = !rows.equals(view.rows());
    rows = view.rows();
    unknownPoints = view.unknownPoints();
    omitted = view.omitted();
    page = PanelPages.clamp(page, rows.size());
    if (changed) {
      clearAndInit();
    }
  }

  private int left() {
    return (width - WIDTH) / 2;
  }

  private int top() {
    return (height - HEIGHT) / 2;
  }

  private int rowY(int indexOnPage) {
    return top() + ROWS_TOP + indexOnPage * ROW;
  }

  @Override
  protected void init() {
    int first = PanelPages.firstRow(page);
    int last = PanelPages.lastRow(page, rows.size());
    for (int index = first; index < last; index++) {
      addControls(rows.get(index), rowY(index - first) + 3);
    }
    int y = top() + HEIGHT - 26;
    addSmall("<", left() + PADDING, y, () -> turnPage(-1)).active = page > 0;
    addSmall(">", left() + PADDING + SMALL_BUTTON + 2, y, () -> turnPage(1)).active =
        page < PanelPages.pageCount(rows.size()) - 1;
  }

  private void addControls(PanelRow row, int y) {
    int x = left() + CONTROLS_X;
    if (row.kind() == PanelKind.LAMP) {
      addLampControls(row, x, y);
    } else {
      Text label =
          Text.translatable(
              row.kind() == PanelKind.SWITCH
                  ? "screen.hardwired.main_panel.flip"
                  : row.on()
                      ? "screen.hardwired.main_panel.stop"
                      : "screen.hardwired.main_panel.run");
      addDrawableChild(
                  ButtonWidget.builder(label, button -> toggle(row))
                      .dimensions(x, y, WIDE_BUTTON, BUTTON_HEIGHT)
                      .build())
              .active =
          !row.stale();
    }
  }

  private void addLampControls(PanelRow row, int x, int y) {
    addDrawableChild(
                ButtonWidget.builder(
                        Text.empty(),
                        button ->
                            setLook(
                                row, Hue.byIndex(row.color().ordinal() + direction()), row.level()))
                    .dimensions(x, y, SMALL_BUTTON, BUTTON_HEIGHT)
                    .tooltip(Tooltip.of(Text.translatable(row.color().lampKey())))
                    .build())
            .active =
        !row.stale();
    addSmall(
                "-",
                x + SMALL_BUTTON + 4,
                y,
                () -> setLook(row, row.color(), Math.max(0, row.level() - 1)))
            .active =
        !row.stale();
    addSmall(
                "+",
                x + 2 * SMALL_BUTTON + 6,
                y,
                () -> setLook(row, row.color(), Math.min(LampLook.MAX_BRIGHTNESS, row.level() + 1)))
            .active =
        !row.stale();
  }

  private static int direction() {
    return hasShiftDown() ? -1 : 1;
  }

  private ButtonWidget addSmall(String label, int x, int y, Runnable action) {
    return addDrawableChild(
        ButtonWidget.builder(Text.literal(label), button -> action.run())
            .dimensions(x, y, SMALL_BUTTON, BUTTON_HEIGHT)
            .build());
  }

  private void turnPage(int direction) {
    page = PanelPages.clamp(page + direction, rows.size());
    clearAndInit();
  }

  private void toggle(PanelRow row) {
    ClientPlayNetworking.send(
        new MainPanelActionPayload(panel, row.pos(), PanelAction.TOGGLE, LampLook.DEFAULT));
  }

  private void setLook(PanelRow row, Hue color, int brightness) {
    ClientPlayNetworking.send(
        new MainPanelActionPayload(
            panel, row.pos(), PanelAction.SET_LOOK, new LampLook(color, brightness)));
  }

  @Override
  public void tick() {
    ticks++;
    if (ticks % REFRESH_TICKS == 0) {
      ClientPlayNetworking.send(MainPanelActionPayload.refresh(panel));
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
    context.drawText(
        textRenderer, title, left() + PADDING + 4, top() + 9, SlatePlate.TITLE_COLOR, false);
    int first = PanelPages.firstRow(page);
    int last = PanelPages.lastRow(page, rows.size());
    for (int index = first; index < last; index++) {
      drawRow(context, rows.get(index), rowY(index - first));
    }
    if (rows.isEmpty()) {
      context.drawText(
          textRenderer,
          Text.translatable("screen.hardwired.main_panel.empty"),
          left() + PADDING + 4,
          rowY(0) + 4,
          SlatePlate.MUTED_COLOR,
          false);
    }
    drawFooter(context);
  }

  private void drawRow(DrawContext context, PanelRow row, int y) {
    int x = left() + PADDING + 4;
    context.fill(
        left() + PADDING,
        y + ROW - 2,
        left() + WIDTH - PADDING,
        y + ROW - 1,
        SlatePlate.SLATE_LIGHT);
    context.drawText(
        textRenderer,
        Text.translatable(row.kind().translationKey()),
        x,
        y + 2,
        SlatePlate.TITLE_COLOR,
        false);
    context.drawText(
        textRenderer,
        Text.translatable(
            "screen.hardwired.main_panel.position",
            row.pos().getX(),
            row.pos().getY(),
            row.pos().getZ()),
        x,
        y + 12,
        SlatePlate.MUTED_COLOR,
        false);
    drawState(context, row, left() + STATE_X, y);
    if (row.kind() != PanelKind.SWITCH) {
      BrassDial.draw(
          context, left() + DIAL_X, y + ROW / 2 - 1, DIAL_RADIUS, row.level(), row.maxLevel());
    }
    if (row.kind() == PanelKind.LAMP) {
      int swatch = left() + CONTROLS_X;
      context.fill(
          swatch + 3,
          y + 6,
          swatch + SMALL_BUTTON - 3,
          y + 3 + BUTTON_HEIGHT - 3,
          SlatePlate.OPAQUE | row.color().lampRgb());
      context.drawText(
          textRenderer,
          Integer.toString(row.level()),
          swatch + 3 * SMALL_BUTTON + 10,
          y + 7,
          SlatePlate.MUTED_COLOR,
          false);
    } else if (row.kind() == PanelKind.CONTROLLER) {
      context.drawText(
          textRenderer,
          Text.translatable("screen.hardwired.main_panel.progress", row.level(), row.maxLevel()),
          left() + CONTROLS_X + WIDE_BUTTON + 2,
          y + 7,
          SlatePlate.MUTED_COLOR,
          false);
    }
  }

  private void drawState(DrawContext context, PanelRow row, int x, int y) {
    int color = row.on() ? ON_COLOR : SlatePlate.BRASS_SHADOW;
    context.fill(x, y + 3, x + MARK, y + 3 + MARK, color);
    if (!row.on()) {
      context.fill(x + 1, y + 4, x + MARK - 1, y + 2 + MARK, SlatePlate.SLATE);
    }
    context.drawText(
        textRenderer,
        Text.translatable(
            row.on() ? "screen.hardwired.main_panel.on" : "screen.hardwired.main_panel.off"),
        x + MARK + 3,
        y + 2,
        SlatePlate.TITLE_COLOR,
        false);
    if (row.stale()) {
      context.drawText(
          textRenderer,
          Text.translatable("screen.hardwired.main_panel.last_seen"),
          x,
          y + 12,
          SlatePlate.MUTED_COLOR,
          false);
    }
  }

  private void drawFooter(DrawContext context) {
    int y = top() + HEIGHT - 26;
    int pages = PanelPages.pageCount(rows.size());
    int dialX = left() + PADDING + 2 * SMALL_BUTTON + 14;
    BrassDial.draw(context, dialX, y + 8, PAGE_DIAL_RADIUS, page, pages - 1);
    context.drawText(
        textRenderer,
        Text.translatable("screen.hardwired.main_panel.page", page + 1, pages),
        dialX + PAGE_DIAL_RADIUS + 5,
        y + 4,
        SlatePlate.TITLE_COLOR,
        false);
    int noteY = y - 11;
    if (unknownPoints > 0) {
      context.drawText(
          textRenderer,
          Text.translatable("screen.hardwired.main_panel.unknown", unknownPoints),
          left() + PADDING + 4,
          noteY,
          SlatePlate.MUTED_COLOR,
          false);
    }
    if (omitted > 0) {
      Text note = Text.translatable("screen.hardwired.main_panel.omitted", omitted);
      context.drawText(
          textRenderer,
          note,
          left() + WIDTH - PADDING - textRenderer.getWidth(note),
          y + 4,
          SlatePlate.MUTED_COLOR,
          false);
    }
  }

  @Override
  public boolean shouldPause() {
    return false;
  }
}
