package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.block.LiftFloorBlock;
import com.grahambartley.hardwired.lift.LiftRow;
import com.grahambartley.hardwired.network.LiftChoosePayload;
import com.grahambartley.hardwired.network.LiftViewPayload;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class LiftScreen extends Screen {

  private static final int TITLE_INSET = 8;
  private static final int WIDTH = 150;
  private static final int PADDING = 14;
  private static final int TITLE_Y = 12;
  private static final int ROWS_Y = 26;
  private static final int ROW_HEIGHT = 22;
  private static final int BUTTON_HEIGHT = 20;
  private static final int PAGE_BUTTON_WIDTH = 30;

  private final LiftViewPayload view;
  private final List<LiftRow> rows;
  private int page;

  public LiftScreen(LiftViewPayload view) {
    super(Text.translatable("block.hardwired.lift"));
    this.view = view;
    this.rows = LiftRow.topDown(view.levels());
    this.page =
        PanelPages.clamp(LiftRow.pageOf(rows, view.here(), PanelPages.ROWS_PER_PAGE), rows.size());
  }

  private int shownRows() {
    return Math.min(rows.size(), PanelPages.ROWS_PER_PAGE);
  }

  private boolean paged() {
    return PanelPages.pageCount(rows.size()) > 1;
  }

  private int plateHeight() {
    int pager = paged() ? ROW_HEIGHT : 0;
    return ROWS_Y + shownRows() * ROW_HEIGHT + pager + PADDING - 2;
  }

  private int left() {
    return (width - WIDTH) / 2;
  }

  private int top() {
    return (height - plateHeight()) / 2;
  }

  @Override
  protected void init() {
    int x = left() + PADDING;
    int inner = WIDTH - 2 * PADDING;
    int y = top() + ROWS_Y;
    for (int i = PanelPages.firstRow(page); i < PanelPages.lastRow(page, rows.size()); i++) {
      LiftRow row = rows.get(i);
      boolean here = row.level() == view.here();
      String key = here ? "screen.hardwired.lift.here" : "screen.hardwired.lift.floor";
      ButtonWidget button =
          ButtonWidget.builder(Text.translatable(key, row.number()), b -> choose(row))
              .dimensions(x, y, inner, BUTTON_HEIGHT)
              .build();
      button.active = !here;
      addDrawableChild(button);
      y += ROW_HEIGHT;
    }
    if (paged()) {
      addPageButton(x, Text.literal("<"), -1, y);
      addPageButton(x + inner - PAGE_BUTTON_WIDTH, Text.literal(">"), 1, y);
    }
  }

  private void addPageButton(int x, Text label, int step, int y) {
    ButtonWidget button =
        ButtonWidget.builder(label, b -> turn(step))
            .dimensions(x, y, PAGE_BUTTON_WIDTH, BUTTON_HEIGHT)
            .build();
    button.active = PanelPages.clamp(page + step, rows.size()) != page;
    addDrawableChild(button);
  }

  private void turn(int step) {
    page = PanelPages.clamp(page + step, rows.size());
    clearAndInit();
  }

  private void choose(LiftRow row) {
    ClientPlayNetworking.send(new LiftChoosePayload(view.gate(), row.level()));
    close();
  }

  @Override
  public void tick() {
    BlockState gate = client.world.getBlockState(view.gate());
    if (!(gate.getBlock() instanceof LiftFloorBlock) || !gate.get(LiftFloorBlock.OPEN)) {
      close();
    }
  }

  @Override
  public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderBackground(context, mouseX, mouseY, delta);
    SlatePlate.draw(context, left(), top(), WIDTH, plateHeight());
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);
    PanelText.shadowed(
        context,
        textRenderer,
        title,
        left() + TITLE_INSET,
        left() + WIDTH - TITLE_INSET,
        top() + TITLE_Y,
        SlatePlate.TITLE_COLOR,
        PanelText.Align.CENTER);
  }

  @Override
  public boolean shouldPause() {
    return false;
  }
}
