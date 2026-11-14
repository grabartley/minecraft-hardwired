package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.client.radio.CallTalk;
import com.grahambartley.hardwired.client.radio.TalkKey;
import com.grahambartley.hardwired.device.IntercomLine;
import com.grahambartley.hardwired.network.PhoneActionPayload;
import com.grahambartley.hardwired.network.PhoneViewPayload;
import com.grahambartley.hardwired.phone.LineStatus;
import com.grahambartley.hardwired.phone.PhoneNumbering;
import com.grahambartley.hardwired.registry.ModSounds;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public final class DialScreen extends Screen {

  private static final int WIDTH = 330;
  private static final int HEIGHT = 196;
  private static final int PADDING = 10;
  private static final int WHEEL_X = 86;
  private static final int WHEEL_Y = 104;
  private static final int SIDE_X = 172;
  private static final int TITLE_Y = 10;
  private static final int STATUS_Y = 26;
  private static final int DIALED_Y = 40;
  private static final int LIST_Y = 58;
  private static final int ROW = 10;
  private static final int FIELD_HEIGHT = 18;
  private static final int BUTTON_HEIGHT = 18;
  private static final int GAP = 4;
  private static final int MOST_LINES = 5;
  private static final int LIST_COLOR = 0xFFE6E2DA;

  private final BlockPos phone;
  private final Deque<Text> transcript = new ArrayDeque<>();
  private final DialState dial = new DialState();
  private final LineTones tones = new LineTones();
  private final CallTalk talk;
  private PhoneViewPayload view;
  private LineStatus status;
  private int listOffset;
  private TextFieldWidget field;
  private TalkButton talkButton;

  public DialScreen(PhoneViewPayload view) {
    super(Text.translatable("screen.hardwired.telephone.title"));
    this.phone = view.phone();
    this.talk = new CallTalk(view.phone());
    accept(view);
  }

  public boolean shows(BlockPos pos) {
    return phone.equals(pos);
  }

  public void accept(PhoneViewPayload next) {
    view = next;
    status = LineStatus.byIndex(next.status());
    if (!status.isOnCall()) {
      dial.clearDigits();
    }
    if (field != null) {
      field.visible = connected();
    }
  }

  public void hear(Text line) {
    transcript.addLast(line);
    while (transcript.size() > MOST_LINES) {
      transcript.removeFirst();
    }
  }

  private int left() {
    return (width - WIDTH) / 2;
  }

  private int top() {
    return (height - HEIGHT) / 2;
  }

  private int sideWidth() {
    return WIDTH - SIDE_X - PADDING;
  }

  private int buttonY() {
    return top() + HEIGHT - PADDING - BUTTON_HEIGHT;
  }

  private int fieldY() {
    return buttonY() - GAP - FIELD_HEIGHT;
  }

  @Override
  protected void init() {
    Text prompt = Text.translatable("screen.hardwired.intercom.prompt");
    field =
        new TextFieldWidget(
            textRenderer, left() + SIDE_X, fieldY(), sideWidth(), FIELD_HEIGHT, prompt);
    field.setMaxLength(IntercomLine.MAX_LENGTH);
    field.setPlaceholder(prompt);
    field.visible = connected();
    addDrawableChild(field);
    int half = (sideWidth() - GAP) / 2;
    talkButton =
        new TalkButton(
            left() + SIDE_X,
            buttonY(),
            half,
            BUTTON_HEIGHT,
            Text.translatable("screen.hardwired.desk.talk"),
            () -> talk.press(client, connected()));
    addDrawableChild(talkButton);
    addDrawableChild(
        new RedButton(
            left() + SIDE_X + half + GAP,
            buttonY(),
            half,
            BUTTON_HEIGHT,
            Text.translatable("screen.hardwired.telephone.hang_up"),
            pressed -> close()));
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
      int pulses =
          RotaryWheel.holeAt(
              mouseX - wheelX(),
              mouseY - wheelY(),
              RotaryDialPainter.RING_RADIUS,
              RotaryDialPainter.HOLE_RADIUS);
      if (dial.grab(pulses, angleTo(mouseX, mouseY), status.isOnCall())) {
        return true;
      }
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseDragged(
      double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    if (dial.holding()) {
      dial.drag(angleTo(mouseX, mouseY));
      return true;
    }
    return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    talk.letGo();
    if (dial.holding()) {
      dial.release();
      dial.number()
          .ifPresent(number -> ClientPlayNetworking.send(PhoneActionPayload.dial(phone, number)));
      return true;
    }
    return super.mouseReleased(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseScrolled(
      double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    int most = Math.max(0, view.listings().size() - visibleRows());
    listOffset = Math.clamp(listOffset - (int) Math.signum(verticalAmount), 0, most);
    return true;
  }

  @Override
  public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
    if (TalkKey.matches(keyCode, scanCode)) {
      talk.letGo();
      return true;
    }
    return super.keyReleased(keyCode, scanCode, modifiers);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (TalkKey.matches(keyCode, scanCode) && !field.isFocused()) {
      talk.press(client, connected());
      return true;
    }
    if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
        && field.isFocused()) {
      say();
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public void tick() {
    if (dial.tick()) {
      LineTones.play(client, ModSounds.TELEPHONE_DIAL_CLICK);
    }
    tones.tick(client, status);
    talk.tick(client, connected());
    talkButton.visible = connected() && CallTalk.allowed();
    talkButton.setLit(talk.micOpen());
  }

  @Override
  public void removed() {
    talk.letGo();
    tones.stop(client);
    ClientPlayNetworking.send(PhoneActionPayload.hangUp(phone));
    super.removed();
  }

  @Override
  public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderBackground(context, mouseX, mouseY, delta);
    SlatePlate.draw(context, left(), top(), WIDTH, HEIGHT);
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);
    int sideLeft = left() + SIDE_X;
    int sideRight = sideLeft + sideWidth();
    PanelText.shadowed(
        context,
        textRenderer,
        title,
        left() + PADDING,
        left() + WIDTH - PADDING,
        top() + TITLE_Y,
        SlatePlate.TITLE_COLOR,
        PanelText.Align.CENTER);
    RotaryDialPainter.draw(context, textRenderer, wheelX(), wheelY(), dial.turned(), ownNumber());
    PanelText.shadowed(
        context,
        textRenderer,
        statusText(),
        sideLeft,
        sideRight,
        top() + STATUS_Y,
        SlatePlate.BRASS,
        PanelText.Align.LEFT);
    PanelText.flat(
        context,
        textRenderer,
        Text.literal(dial.dialed() + "_".repeat(DialState.DIGITS - dial.dialed().length())),
        sideLeft,
        sideRight,
        top() + DIALED_Y,
        LIST_COLOR,
        PanelText.Align.LEFT);
    if (connected()) {
      renderTranscript(context, sideLeft, sideRight);
    } else {
      renderRows(context, sideLeft, sideRight, listings());
    }
  }

  private void renderTranscript(DrawContext context, int left, int right) {
    List<OrderedText> lines =
        transcript.stream()
            .flatMap(line -> textRenderer.wrapLines(line, right - left).stream())
            .toList();
    int shown = Math.min(lines.size(), visibleRows());
    for (int row = 0; row < shown; row++) {
      OrderedText line = lines.get(lines.size() - shown + row);
      context.drawText(textRenderer, line, left, top() + LIST_Y + row * ROW, LIST_COLOR, false);
    }
  }

  private void renderRows(DrawContext context, int left, int right, List<Text> rows) {
    for (int row = 0; row < rows.size() && row < visibleRows(); row++) {
      PanelText.flat(
          context,
          textRenderer,
          rows.get(row),
          left,
          right,
          top() + LIST_Y + row * ROW,
          row % 2 == 0 ? LIST_COLOR : SlatePlate.MUTED_COLOR,
          PanelText.Align.LEFT);
    }
  }

  private Text statusText() {
    if (talk.problem() != null) {
      return Text.translatable(talk.problem().translationKey());
    }
    return Text.translatable(status.translationKey());
  }

  private boolean connected() {
    return status == LineStatus.CONNECTED;
  }

  private List<Text> listings() {
    if (view.listings().isEmpty()) {
      return List.of(Text.translatable("screen.hardwired.telephone.no_directory"));
    }
    return view.listings().stream()
        .skip(listOffset)
        .map(listing -> PhoneListing.label(listing.number(), listing.name()))
        .toList();
  }

  private int visibleRows() {
    return (fieldY() - GAP - top() - LIST_Y) / ROW;
  }

  private Text ownNumber() {
    return view.number() == PhoneNumbering.NONE
        ? Text.literal("---")
        : Text.literal(String.valueOf(view.number()));
  }

  private void say() {
    if (!IntercomLine.clean(field.getText()).isEmpty()) {
      ClientPlayNetworking.send(PhoneActionPayload.say(phone, field.getText()));
    }
    field.setText("");
  }

  private int wheelX() {
    return left() + WHEEL_X;
  }

  private int wheelY() {
    return top() + WHEEL_Y;
  }

  private double angleTo(double mouseX, double mouseY) {
    return Math.atan2(mouseY - wheelY(), mouseX - wheelX());
  }

  @Override
  public boolean shouldPause() {
    return false;
  }
}
