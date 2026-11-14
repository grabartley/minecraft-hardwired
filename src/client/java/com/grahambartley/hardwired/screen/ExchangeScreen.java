package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.network.ExchangeSharePayload;
import com.grahambartley.hardwired.network.ExchangeViewPayload;
import com.grahambartley.hardwired.phone.PartyLines;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public final class ExchangeScreen extends Screen {

  private static final int WIDTH = 240;
  private static final int HEIGHT = 200;
  private static final int PADDING = 10;
  private static final int TITLE_Y = 10;
  private static final int DIGIT_Y = 24;
  private static final int LIST_Y = 40;
  private static final int ROW = 12;
  private static final int BUTTON_HEIGHT = 18;
  private static final int GAP = 6;
  private static final int CHOSEN_FILL = 0xFF5C4A1E;

  private ExchangeViewPayload view;
  private final Set<BlockPos> chosen = new LinkedHashSet<>();
  private int offset;

  public ExchangeScreen(ExchangeViewPayload view) {
    super(Text.translatable("block.hardwired.exchange"));
    this.view = view;
  }

  public boolean shows(BlockPos pos) {
    return view.exchange().equals(pos);
  }

  public void accept(ExchangeViewPayload next) {
    view = next;
    chosen.clear();
  }

  private int left() {
    return (width - WIDTH) / 2;
  }

  private int top() {
    return (height - HEIGHT) / 2;
  }

  @Override
  protected void init() {
    int half = (WIDTH - 2 * PADDING - GAP) / 2;
    int y = top() + HEIGHT - PADDING - BUTTON_HEIGHT;
    addDrawableChild(
        new RedButton(
            left() + PADDING,
            y,
            half,
            BUTTON_HEIGHT,
            Text.translatable("screen.hardwired.exchange.share"),
            pressed -> send(true)));
    addDrawableChild(
        new RedButton(
            left() + PADDING + half + GAP,
            y,
            half,
            BUTTON_HEIGHT,
            Text.translatable("screen.hardwired.exchange.part"),
            pressed -> send(false)));
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    int row = (int) Math.floor((mouseY - top() - LIST_Y) / ROW);
    boolean inList =
        mouseX >= left() + PADDING && mouseX < left() + WIDTH - PADDING && mouseY >= top() + LIST_Y;
    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && inList && row >= 0 && row < visibleRows()) {
      int index = row + offset;
      if (index < view.lines().size()) {
        BlockPos phone = view.lines().get(index).phone();
        if (!chosen.remove(phone) && chosen.size() < PartyLines.MOST_PHONES) {
          chosen.add(phone);
        }
        return true;
      }
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseScrolled(
      double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    int most = Math.max(0, view.lines().size() - visibleRows());
    offset = Math.clamp(offset - (int) Math.signum(verticalAmount), 0, most);
    return true;
  }

  @Override
  public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderBackground(context, mouseX, mouseY, delta);
    SlatePlate.draw(context, left(), top(), WIDTH, HEIGHT);
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);
    int textLeft = left() + PADDING;
    int textRight = left() + WIDTH - PADDING;
    PanelText.shadowed(
        context,
        textRenderer,
        title,
        textLeft,
        textRight,
        top() + TITLE_Y,
        SlatePlate.TITLE_COLOR,
        PanelText.Align.CENTER);
    Text digit = Text.translatable("screen.hardwired.exchange.digit", view.digit());
    PanelText.flat(
        context,
        textRenderer,
        digit,
        textLeft,
        textRight,
        top() + DIGIT_Y,
        SlatePlate.BRASS,
        PanelText.Align.CENTER);
    List<ExchangeViewPayload.Line> lines = view.lines();
    if (lines.isEmpty()) {
      PanelText.flat(
          context,
          textRenderer,
          Text.translatable("screen.hardwired.exchange.no_lines"),
          textLeft,
          textRight,
          top() + LIST_Y,
          SlatePlate.MUTED_COLOR,
          PanelText.Align.CENTER);
    }
    for (int row = 0; row < visibleRows() && row + offset < lines.size(); row++) {
      ExchangeViewPayload.Line line = lines.get(row + offset);
      int y = top() + LIST_Y + row * ROW;
      if (chosen.contains(line.phone())) {
        context.fill(textLeft - 2, y - 2, textRight + 2, y + ROW - 2, CHOSEN_FILL);
      }
      Text label = PhoneListing.label(line.number(), line.name());
      PanelText.flat(
          context,
          textRenderer,
          label,
          textLeft,
          textRight,
          y,
          SlatePlate.CALCITE,
          PanelText.Align.LEFT);
    }
  }

  private int visibleRows() {
    return (HEIGHT - LIST_Y - PADDING - BUTTON_HEIGHT - GAP) / ROW;
  }

  private void send(boolean share) {
    if (!chosen.isEmpty()) {
      ClientPlayNetworking.send(
          new ExchangeSharePayload(view.exchange(), new ArrayList<>(chosen), share));
    }
  }

  @Override
  public boolean shouldPause() {
    return false;
  }
}
