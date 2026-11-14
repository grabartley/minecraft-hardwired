package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.client.radio.CallTalk;
import com.grahambartley.hardwired.client.radio.TalkKey;
import com.grahambartley.hardwired.device.IntercomLine;
import com.grahambartley.hardwired.network.PhoneActionPayload;
import com.grahambartley.hardwired.network.PhoneViewPayload;
import com.grahambartley.hardwired.phone.LineStatus;
import com.grahambartley.hardwired.phone.PhoneDirectory.Listing;
import com.grahambartley.hardwired.phone.PhoneNumbering;
import com.grahambartley.hardwired.phone.RingCadence;
import com.grahambartley.hardwired.registry.ModSounds;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
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
  private static final int BUTTON_GAP = 6;
  private static final int MOST_LINES = 5;
  private static final int DIGITS = 3;
  private static final double RETURN_PER_TICK = RotaryWheel.STEP / 2;
  private static final int BUSY_TICKS = 10;
  private static final int LIST_COLOR = 0xFFE6E2DA;

  private final BlockPos phone;
  private final Deque<Text> transcript = new ArrayDeque<>();
  private PhoneViewPayload view;
  private LineStatus status;
  private String dialed = "";
  private int heldPulses;
  private double grabAngle;
  private double turned;
  private boolean returning;
  private int listOffset;
  private long ticks;
  private TextFieldWidget field;
  private TalkButton talkButton;
  private final CallTalk talk;

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
    LineStatus previous = status;
    status = LineStatus.byIndex(next.status());
    if (previous != status) {
      ticks = 0;
      if (!status.isOnCall()) {
        dialed = "";
      }
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

  @Override
  protected void init() {
    int sideWidth = WIDTH - SIDE_X - PADDING;
    field =
        new TextFieldWidget(
            textRenderer,
            left() + SIDE_X,
            top() + HEIGHT - PADDING - BUTTON_HEIGHT - FIELD_HEIGHT - 4,
            sideWidth,
            FIELD_HEIGHT,
            Text.translatable("screen.hardwired.intercom.prompt"));
    field.setMaxLength(IntercomLine.MAX_LENGTH);
    field.setPlaceholder(Text.translatable("screen.hardwired.intercom.prompt"));
    field.visible = connected();
    addDrawableChild(field);
    int half = (sideWidth - BUTTON_GAP) / 2;
    talkButton =
        new TalkButton(
            left() + SIDE_X,
            top() + HEIGHT - PADDING - BUTTON_HEIGHT,
            half,
            BUTTON_HEIGHT,
            Text.translatable("screen.hardwired.desk.talk"),
            () -> talk.press(client, connected()));
    addDrawableChild(talkButton);
    addDrawableChild(
        new RedButton(
            left() + SIDE_X + half + BUTTON_GAP,
            top() + HEIGHT - PADDING - BUTTON_HEIGHT,
            half,
            BUTTON_HEIGHT,
            Text.translatable("screen.hardwired.telephone.hang_up"),
            pressed -> close()));
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && canDial()) {
      int pulses =
          RotaryWheel.holeAt(
              mouseX - wheelX(),
              mouseY - wheelY(),
              RotaryDialPainter.RING_RADIUS,
              RotaryDialPainter.HOLE_RADIUS);
      if (pulses > 0) {
        heldPulses = pulses;
        grabAngle = angleTo(mouseX, mouseY);
        turned = 0;
        return true;
      }
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseDragged(
      double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    if (heldPulses > 0) {
      turned = RotaryWheel.turn(grabAngle, angleTo(mouseX, mouseY), heldPulses);
      return true;
    }
    return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    talk.letGo();
    if (heldPulses > 0) {
      if (RotaryWheel.atStop(turned, heldPulses)) {
        dial(RotaryWheel.digitFor(heldPulses));
      }
      heldPulses = 0;
      returning = true;
      return true;
    }
    return super.mouseReleased(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseScrolled(
      double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    int most = Math.max(0, view.listings().size() - visibleListings());
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
    ticks++;
    talk.tick(client, connected());
    talkButton.visible = connected() && CallTalk.allowed();
    talkButton.setLit(talk.micOpen());
    if (returning) {
      double before = turned;
      turned = Math.max(0, turned - RETURN_PER_TICK);
      if (Math.floor(before / RotaryWheel.STEP) > Math.floor(turned / RotaryWheel.STEP)
          && turned >= RotaryWheel.STEP) {
        play(ModSounds.TELEPHONE_DIAL_CLICK, 1F);
      }
      returning = turned > 0;
    }
    if (status == LineStatus.RINGING_OUT && RingCadence.strokeStarting(ticks, false) > 0) {
      play(ModSounds.TELEPHONE_RINGBACK, 1F);
    } else if (status == LineStatus.BUSY && ticks % BUSY_TICKS == 0) {
      play(ModSounds.TELEPHONE_BUSY, 1F);
    }
  }

  @Override
  public void removed() {
    talk.letGo();
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
    int sideRight = left() + WIDTH - PADDING;
    PanelText.shadowed(
        context,
        textRenderer,
        title,
        left() + PADDING,
        left() + WIDTH - PADDING,
        top() + TITLE_Y,
        SlatePlate.TITLE_COLOR,
        PanelText.Align.CENTER);
    RotaryDialPainter.draw(context, textRenderer, wheelX(), wheelY(), turned, ownNumber());
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
        Text.literal(dialed + "_".repeat(DIGITS - dialed.length())),
        sideLeft,
        sideRight,
        top() + DIALED_Y,
        LIST_COLOR,
        PanelText.Align.LEFT);
    if (connected()) {
      renderLines(context, sideLeft, sideRight, transcript.stream().toList());
    } else {
      renderLines(context, sideLeft, sideRight, listings());
    }
  }

  private void renderLines(DrawContext context, int left, int right, List<Text> lines) {
    for (int row = 0; row < lines.size() && row < visibleListings(); row++) {
      PanelText.flat(
          context,
          textRenderer,
          lines.get(row),
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
    List<Listing> all = view.listings();
    if (all.isEmpty()) {
      return List.of(Text.translatable("screen.hardwired.telephone.no_directory"));
    }
    return all.stream()
        .skip(listOffset)
        .<Text>map(
            listing ->
                listing.name().isEmpty()
                    ? Text.literal(String.valueOf(listing.number()))
                    : Text.translatable(
                        "screen.hardwired.telephone.listing",
                        String.valueOf(listing.number()),
                        listing.name()))
        .toList();
  }

  private int visibleListings() {
    return (HEIGHT - LIST_Y - PADDING - BUTTON_HEIGHT - FIELD_HEIGHT - 8) / ROW;
  }

  private Text ownNumber() {
    return view.number() == PhoneNumbering.NONE
        ? Text.literal("---")
        : Text.literal(String.valueOf(view.number()));
  }

  private boolean canDial() {
    return !status.isOnCall() && !returning && dialed.length() < DIGITS;
  }

  private void dial(int digit) {
    dialed += digit;
    if (dialed.length() == DIGITS) {
      ClientPlayNetworking.send(PhoneActionPayload.dial(phone, Integer.parseInt(dialed)));
    }
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

  private void play(SoundEvent sound, float pitch) {
    if (client != null) {
      client.getSoundManager().play(PositionedSoundInstance.master(sound, pitch));
    }
  }

  @Override
  public boolean shouldPause() {
    return false;
  }
}
