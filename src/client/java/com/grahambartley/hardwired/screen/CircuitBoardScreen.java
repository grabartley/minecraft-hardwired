package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.block.CircuitBoardBlockEntity;
import com.grahambartley.hardwired.circuit.Cost;
import com.grahambartley.hardwired.circuit.Design;
import com.grahambartley.hardwired.circuit.DesignText;
import com.grahambartley.hardwired.circuit.Evaluator;
import com.grahambartley.hardwired.circuit.Part;
import com.grahambartley.hardwired.circuit.PartType;
import com.grahambartley.hardwired.circuit.PortMode;
import com.grahambartley.hardwired.circuit.Refusal;
import com.grahambartley.hardwired.circuit.Side;
import com.grahambartley.hardwired.circuit.Validator;
import com.grahambartley.hardwired.config.CircuitSettings;
import com.grahambartley.hardwired.config.SyncedServerConfig;
import com.grahambartley.hardwired.device.CircuitAction;
import com.grahambartley.hardwired.device.DesignCodecs;
import com.grahambartley.hardwired.network.CircuitActionPayload;
import com.grahambartley.hardwired.network.CircuitResultPayload;
import com.grahambartley.hardwired.screen.CircuitLayout.Cell;
import com.grahambartley.hardwired.screen.CircuitLayout.Point;
import com.grahambartley.hardwired.screen.CircuitLayout.Rect;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public final class CircuitBoardScreen extends Screen {

  private static final int LEFT_BUTTON = 0;
  private static final int RIGHT_BUTTON = 1;
  private static final int NONE = -1;
  private static final int BUTTON_HEIGHT = 15;
  private static final int BUTTON_GAP = 1;
  private static final int SMALL_BUTTON = 12;
  private static final int BIG_SETTING_STEP = 10;
  private static final int TEXT_INSET = 11;
  private static final double TICKS_PER_SECOND = 20.0;

  private final BlockPos pos;
  private final CircuitBoardBlockEntity board;
  private final CircuitDraft draft;
  private PartType armed;
  private int selected = NONE;
  private CircuitPin pressedPin;
  private Side pressedPad;
  private int draggedPart = NONE;
  private CircuitBench bench;
  private Text message = Text.empty();
  private boolean messageIsWarning;
  private ButtonWidget testButton;
  private ButtonWidget saveButton;
  private ButtonWidget makeButton;
  private ButtonWidget lessButton;
  private ButtonWidget moreButton;

  public CircuitBoardScreen(BlockPos pos, CircuitBoardBlockEntity board) {
    super(Text.translatable("block.hardwired.circuit_board"));
    this.pos = pos;
    this.board = board;
    this.draft = new CircuitDraft(board.design());
  }

  public boolean shows(BlockPos other) {
    return pos.equals(other);
  }

  public void accept(CircuitResultPayload result) {
    boolean refused = result.design().isEmpty();
    message = Text.translatable(result.message(), result.amount());
    messageIsWarning = refused;
    if (!refused) {
      stopTest();
      draft.load(DesignCodecs.orEmpty(result.design()));
      selected = NONE;
    }
  }

  private CircuitLayout layout() {
    return CircuitLayout.centredIn(width, height);
  }

  private static CircuitSettings settings() {
    return SyncedServerConfig.get().circuit();
  }

  private Optional<Refusal> problem() {
    return Validator.check(draft.design(), settings().maxParts());
  }

  private boolean testing() {
    return bench != null;
  }

  @Override
  protected void init() {
    CircuitLayout layout = layout();
    int x = layout.columnX();
    int y = layout.buttonsY();
    int full = layout.columnWidth();
    int half = (full - BUTTON_GAP) / 2;
    int rowStep = BUTTON_HEIGHT + BUTTON_GAP;
    int second = x + half + BUTTON_GAP;
    testButton = button(x, y, full, "test", this::toggleTest);
    saveButton = button(x, y + rowStep, half, "save", this::save);
    makeButton = button(second, y + rowStep, half, "make", () -> send(CircuitAction.MAKE, ""));
    button(x, y + 2 * rowStep, half, "load", () -> send(CircuitAction.LOAD, ""));
    button(second, y + 2 * rowStep, half, "clear", this::clearDraft);
    button(x, y + 3 * rowStep, half, "export", this::export);
    button(second, y + 3 * rowStep, half, "paste", this::paste);
    int settingY = layout.settingY();
    lessButton =
        addDrawableChild(
            new PhosphorButton(
                x, settingY, SMALL_BUTTON, SMALL_BUTTON, Text.literal("-"), () -> adjust(-1)));
    moreButton =
        addDrawableChild(
            new PhosphorButton(
                x + full - SMALL_BUTTON,
                settingY,
                SMALL_BUTTON,
                SMALL_BUTTON,
                Text.literal("+"),
                () -> adjust(1)));
  }

  private ButtonWidget button(int x, int y, int width, String name, Runnable action) {
    return addDrawableChild(
        new PhosphorButton(
            x,
            y,
            width,
            BUTTON_HEIGHT,
            Text.translatable("screen.hardwired.circuit." + name),
            action));
  }

  private void adjust(int direction) {
    if (selected != NONE) {
      draft.adjustSetting(selected, direction * (hasShiftDown() ? BIG_SETTING_STEP : 1));
    }
  }

  private void toggleTest() {
    if (testing()) {
      stopTest();
    } else if (problem().isEmpty()) {
      bench = new CircuitBench(draft.design());
      armed = null;
      selected = NONE;
      say(Text.translatable("screen.hardwired.circuit.testing"), false);
    }
  }

  private void stopTest() {
    bench = null;
  }

  private void save() {
    if (!testing() && problem().isEmpty()) {
      send(CircuitAction.SAVE, DesignText.encode(draft.design()));
    }
  }

  private void clearDraft() {
    stopTest();
    draft.clear();
    selected = NONE;
  }

  private void export() {
    client.keyboard.setClipboard(DesignText.encode(draft.design()));
    say(Text.translatable("screen.hardwired.circuit.exported"), false);
  }

  private void paste() {
    String text = client.keyboard.getClipboard();
    if (text.length() > DesignText.MAX_LENGTH) {
      say(Text.translatable(Refusal.TOO_LONG.key()), true);
    } else {
      send(CircuitAction.PASTE, text);
    }
  }

  private void send(CircuitAction action, String text) {
    if (ClientPlayNetworking.canSend(CircuitActionPayload.ID)) {
      ClientPlayNetworking.send(CircuitActionPayload.of(pos, action, text));
    }
  }

  private void say(Text text, boolean warning) {
    message = text;
    messageIsWarning = warning;
  }

  @Override
  public void tick() {
    if (testing()) {
      bench.tick();
    }
    Design design = draft.design();
    boolean valid = problem().isEmpty();
    testButton.active = valid;
    testButton.setMessage(
        Text.translatable("screen.hardwired.circuit." + (testing() ? "stop" : "test")));
    saveButton.active = valid && !testing();
    makeButton.active = valid && !design.isEmpty() && design.equals(board.design());
    boolean adjustable = selectedPart().filter(part -> part.type().hasSetting()).isPresent();
    lessButton.visible = adjustable;
    moreButton.visible = adjustable;
  }

  private Optional<Part> selectedPart() {
    return selected == NONE ? Optional.empty() : Optional.of(draft.design().partAt(selected));
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (super.mouseClicked(mouseX, mouseY, button)) {
      return true;
    }
    CircuitLayout layout = layout();
    if (testing()) {
      if (button == LEFT_BUTTON) {
        layout.padAt(mouseX, mouseY).ifPresent(bench::toggle);
      }
      return true;
    }
    return button == RIGHT_BUTTON
        ? rightClick(layout, mouseX, mouseY)
        : leftClick(layout, mouseX, mouseY);
  }

  private boolean rightClick(CircuitLayout layout, double mouseX, double mouseY) {
    Design design = draft.design();
    Optional<CircuitPin> pin = layout.pinAt(design, mouseX, mouseY);
    if (pin.isPresent()) {
      draft.disconnect(pin.get());
      return true;
    }
    Optional<Cell> cell = layout.cellAt(mouseX, mouseY);
    if (cell.isPresent()) {
      draft.nodeAt(cell.get().x(), cell.get().y()).ifPresent(this::removePart);
    }
    armed = null;
    return true;
  }

  private void removePart(int node) {
    draft.remove(node);
    selected = NONE;
  }

  private boolean leftClick(CircuitLayout layout, double mouseX, double mouseY) {
    Optional<PartType> palette = layout.paletteAt(mouseX, mouseY);
    if (palette.isPresent()) {
      armed = palette.get() == armed ? null : palette.get();
      return true;
    }
    Design design = draft.design();
    pressedPad = layout.padAt(mouseX, mouseY).orElse(null);
    pressedPin = layout.pinAt(design, mouseX, mouseY).orElse(null);
    if (pressedPin != null || pressedPad != null) {
      return true;
    }
    Optional<Cell> cell = layout.cellAt(mouseX, mouseY);
    if (cell.isEmpty()) {
      return false;
    }
    OptionalInt node = draft.nodeAt(cell.get().x(), cell.get().y());
    if (node.isPresent()) {
      selected = node.getAsInt();
      draggedPart = selected;
    } else if (armed != null) {
      draft.place(armed, cell.get().x(), cell.get().y(), settings().maxParts());
    } else {
      selected = NONE;
    }
    return true;
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    CircuitLayout layout = layout();
    if (button == LEFT_BUTTON) {
      if (pressedPad != null
          && layout.padAt(mouseX, mouseY).filter(pressedPad::equals).isPresent()) {
        cycle(pressedPad);
      } else if (pressedPin != null) {
        layout
            .pinAt(draft.design(), mouseX, mouseY)
            .ifPresent(target -> draft.connect(pressedPin, target));
      } else if (draggedPart != NONE) {
        layout
            .cellAt(mouseX, mouseY)
            .ifPresent(cell -> draft.move(draggedPart, cell.x(), cell.y()));
      }
      pressedPad = null;
      pressedPin = null;
      draggedPart = NONE;
    }
    return super.mouseReleased(mouseX, mouseY, button);
  }

  private void cycle(Side side) {
    draft.cycleSide(side);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
    Optional<Cell> cell = layout().cellAt(mouseX, mouseY);
    if (cell.isPresent() && !testing()) {
      OptionalInt node = draft.nodeAt(cell.get().x(), cell.get().y());
      if (node.isPresent()) {
        int step = (int) Math.signum(vertical) * (hasShiftDown() ? BIG_SETTING_STEP : 1);
        draft.adjustSetting(node.getAsInt(), step);
        return true;
      }
    }
    return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == GLFW.GLFW_KEY_DELETE && selected != NONE && !testing()) {
      removePart(selected);
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderBackground(context, mouseX, mouseY, delta);
    CircuitLayout layout = layout();
    PhosphorScreen.drawCase(
        context, layout.left(), layout.top(), CircuitLayout.WIDTH, CircuitLayout.HEIGHT);
    PhosphorScreen.drawGlass(
        context,
        layout.gridX() - CircuitLayout.PAD_DEPTH - 1,
        layout.gridY() - CircuitLayout.PAD_DEPTH - 1,
        layout.gridRight() + CircuitLayout.PAD_DEPTH + 1,
        layout.gridBottom() + CircuitLayout.PAD_DEPTH + 1);
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);
    CircuitLayout layout = layout();
    CircuitPainter painter = new CircuitPainter(context, layout);
    Design design = draft.design();
    drawHeader(context, layout, design);
    painter.grid();
    painter.lines(design, testing() ? bench::isOn : node -> false);
    painter.parts(design, testing() ? bench::isOn : node -> false, selected);
    for (Side side : Side.values()) {
      painter.pad(side, design.mode(side), testing() && bench.isSideOn(side));
    }
    drawDrag(context, layout, painter, design, mouseX, mouseY);
    drawPalette(context, layout, painter);
    drawSetting(context, layout);
    drawStatus(context, layout);
    drawTooltip(context, layout, design, mouseX, mouseY);
  }

  private void drawHeader(DrawContext context, CircuitLayout layout, Design design) {
    context.drawText(
        textRenderer,
        title,
        layout.left() + TEXT_INSET,
        layout.top() + 6,
        PhosphorScreen.INK,
        false);
    CircuitSettings settings = settings();
    Text info =
        Text.translatable(
            "screen.hardwired.circuit.cost",
            design.parts().size(),
            settings.maxParts(),
            Cost.redstone(design.parts().size(), settings.partsPerRedstone()));
    context.drawText(
        textRenderer,
        info,
        layout.left() + CircuitLayout.WIDTH - TEXT_INSET - textRenderer.getWidth(info),
        layout.top() + 6,
        PhosphorScreen.INK,
        false);
  }

  private void drawDrag(
      DrawContext context,
      CircuitLayout layout,
      CircuitPainter painter,
      Design design,
      int mouseX,
      int mouseY) {
    if (pressedPin != null) {
      Point from =
          pressedPin.output()
              ? layout.sourcePoint(design, pressedPin.node())
              : layout.sinkPoint(design, pressedPin.node(), pressedPin.pin());
      painter.route(LineRoute.between(from, new Point(mouseX, mouseY), 2), PhosphorScreen.BRIGHT);
    } else if (armed != null) {
      layout
          .cellAt(mouseX, mouseY)
          .filter(cell -> draft.nodeAt(cell.x(), cell.y()).isEmpty())
          .ifPresent(cell -> painter.part(Part.of(armed, cell.x(), cell.y()), false, false));
    } else if (draggedPart != NONE) {
      Part part = design.partAt(draggedPart);
      layout
          .cellAt(mouseX, mouseY)
          .ifPresent(cell -> painter.part(part.movedTo(cell.x(), cell.y()), false, true));
    }
  }

  private void drawPalette(DrawContext context, CircuitLayout layout, CircuitPainter painter) {
    PartType[] types = PartType.values();
    for (int row = 0; row < types.length; row++) {
      Rect rect = layout.paletteRow(row);
      boolean chosen = types[row] == armed;
      context.fill(
          rect.x(),
          rect.y(),
          rect.x() + rect.width(),
          rect.y() + rect.height() - 1,
          chosen ? PhosphorScreen.DIM : PhosphorScreen.GLASS);
      painter.glyphRows(
          PartGlyph.rows(types[row]),
          rect.x() + 1,
          rect.y() + 1,
          chosen ? PhosphorScreen.GLASS : PhosphorScreen.BRIGHT);
      String name =
          textRenderer.trimToWidth(
              partName(types[row]).getString(), rect.width() - PartGlyph.SIZE - 3);
      context.drawText(
          textRenderer,
          name,
          rect.x() + PartGlyph.SIZE + 3,
          rect.y() + 1,
          chosen ? PhosphorScreen.GLASS : PhosphorScreen.BRIGHT,
          false);
    }
  }

  private static Text partName(PartType type) {
    return Text.translatable("screen.hardwired.circuit.part." + type.id());
  }

  private void drawSetting(DrawContext context, CircuitLayout layout) {
    selectedPart()
        .filter(part -> part.type().hasSetting())
        .ifPresent(
            part -> {
              Text value = settingText(part);
              context.drawText(
                  textRenderer,
                  value,
                  layout.columnX() + (layout.columnWidth() - textRenderer.getWidth(value)) / 2,
                  layout.settingY() + 2,
                  PhosphorScreen.INK,
                  false);
            });
  }

  private static Text settingText(Part part) {
    if (part.type() == PartType.TIMER) {
      double seconds = part.setting() * Evaluator.TICKS_PER_STEP / TICKS_PER_SECOND;
      return Text.translatable(
          "screen.hardwired.circuit.delay", String.format(Locale.ROOT, "%.1f", seconds));
    }
    return Text.translatable("screen.hardwired.circuit.count", part.setting());
  }

  private void drawStatus(DrawContext context, CircuitLayout layout) {
    Optional<Refusal> problem = problem();
    Text status = problem.<Text>map(refusal -> Text.translatable(refusal.key())).orElse(message);
    boolean warning = problem.isPresent() || messageIsWarning;
    context.drawText(
        textRenderer,
        status,
        layout.left() + TEXT_INSET,
        layout.statusY(),
        warning ? 0xFFA23B00 : PhosphorScreen.INK,
        false);
  }

  private void drawTooltip(
      DrawContext context, CircuitLayout layout, Design design, int mouseX, int mouseY) {
    Optional<Side> side = layout.padAt(mouseX, mouseY);
    if (side.isPresent()) {
      context.drawTooltip(textRenderer, sideTooltip(side.get(), design), mouseX, mouseY);
      return;
    }
    Optional<PartType> palette = layout.paletteAt(mouseX, mouseY);
    if (palette.isPresent()) {
      context.drawTooltip(textRenderer, partName(palette.get()), mouseX, mouseY);
      return;
    }
    if (pressedPin == null && draggedPart == NONE) {
      partTooltip(layout, design, mouseX, mouseY)
          .ifPresent(text -> context.drawTooltip(textRenderer, text, mouseX, mouseY));
    }
  }

  private static List<Text> sideTooltip(Side side, Design design) {
    PortMode mode = design.mode(side);
    String sideName = side.name().toLowerCase(Locale.ROOT);
    String modeName = mode.name().toLowerCase(Locale.ROOT);
    return List.of(
        Text.translatable(
            "screen.hardwired.circuit.side",
            Text.translatable("screen.hardwired.circuit.side." + sideName),
            Text.translatable("screen.hardwired.circuit.port." + modeName)));
  }

  private Optional<Text> partTooltip(
      CircuitLayout layout, Design design, double mouseX, double mouseY) {
    Optional<Cell> cell = layout.cellAt(mouseX, mouseY);
    if (cell.isEmpty()) {
      return Optional.empty();
    }
    OptionalInt node = draft.nodeAt(cell.get().x(), cell.get().y());
    if (node.isEmpty()) {
      return Optional.empty();
    }
    PartType type = design.partAt(node.getAsInt()).type();
    Optional<CircuitPin> pin = layout.pinAt(design, mouseX, mouseY);
    if (pin.isPresent() && !pin.get().output() && type.isDelay()) {
      return Optional.of(
          Text.translatable("screen.hardwired.circuit.pin." + type.id() + "." + pin.get().pin()));
    }
    return Optional.of(partName(type));
  }

  @Override
  public boolean shouldPause() {
    return false;
  }
}
