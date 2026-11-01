package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.block.BroadcastDeskBlockEntity;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.network.DeskActionPayload;
import com.grahambartley.hardwired.radio.DeskAction;
import com.grahambartley.hardwired.radio.Frequency;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class BroadcastDeskScreen extends Screen {

  private static final int WIDTH = 240;
  private static final int HEIGHT = 170;
  private static final int PADDING = 12;
  private static final int TITLE_Y = 10;
  private static final int DIAL_Y = 46;
  private static final int DIAL_RADIUS = 18;
  private static final int READOUT_Y = 70;
  private static final int TUNING_Y = 82;
  private static final int AIR_Y = 106;
  private static final int TRANSPORT_Y = 128;
  private static final int STATUS_Y = 152;
  private static final int ROW_HEIGHT = 20;
  private static final int GAP = 4;
  private static final int TRANSPORT_BUTTONS = 3;

  private final BlockPos pos;
  private final BroadcastDeskBlockEntity desk;
  private int frequency;
  private RedButton air;
  private ButtonWidget play;
  private ButtonWidget stop;
  private ButtonWidget eject;

  public BroadcastDeskScreen(BlockPos pos, BroadcastDeskBlockEntity desk) {
    super(Text.translatable("block.hardwired.broadcast_desk"));
    this.pos = pos;
    this.desk = desk;
    this.frequency = desk.frequency();
  }

  private int left() {
    return (width - WIDTH) / 2;
  }

  private int top() {
    return (height - HEIGHT) / 2;
  }

  @Override
  protected void init() {
    int x = left() + PADDING;
    int inner = WIDTH - 2 * PADDING;
    TuningRow.add(
        this::addDrawableChild,
        x,
        top() + TUNING_Y,
        inner,
        ROW_HEIGHT,
        () -> frequency,
        next -> {
          frequency = next;
          send(DeskAction.SET_FREQUENCY, next);
        });
    air =
        new RedButton(
            x,
            top() + AIR_Y,
            inner,
            ROW_HEIGHT,
            Text.translatable("screen.hardwired.desk.on_air"),
            pressed -> send(DeskAction.TOGGLE_AIR, 0));
    addDrawableChild(air);
    int button = (inner - (TRANSPORT_BUTTONS - 1) * GAP) / TRANSPORT_BUTTONS;
    play = transport(x, button, "play", DeskAction.PLAY);
    stop = transport(x + button + GAP, button, "stop", DeskAction.STOP);
    eject = transport(x + 2 * (button + GAP), button, "eject", DeskAction.EJECT);
    refresh();
  }

  private ButtonWidget transport(int x, int buttonWidth, String key, DeskAction action) {
    ButtonWidget button =
        ButtonWidget.builder(
                Text.translatable("screen.hardwired.desk." + key), pressed -> send(action, 0))
            .dimensions(x, top() + TRANSPORT_Y, buttonWidth, ROW_HEIGHT)
            .build();
    addDrawableChild(button);
    return button;
  }

  private void send(DeskAction action, int value) {
    ClientPlayNetworking.send(new DeskActionPayload(pos, action.ordinal(), value));
  }

  private void refresh() {
    boolean live = desk.status() == DeviceStatus.RUNNING;
    air.setLit(desk.wantsAir());
    play.active = live && desk.hasDisc();
    stop.active = desk.songPlaying();
    eject.active = desk.hasDisc();
  }

  @Override
  public void tick() {
    if (client.world.getBlockEntity(pos) != desk) {
      close();
      return;
    }
    refresh();
  }

  @Override
  public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderBackground(context, mouseX, mouseY, delta);
    SlatePlate.draw(context, left(), top(), WIDTH, HEIGHT);
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);
    context.drawCenteredTextWithShadow(
        textRenderer, title, width / 2, top() + TITLE_Y, SlatePlate.TITLE_COLOR);
    BrassDial.draw(
        context,
        width / 2,
        top() + DIAL_Y,
        DIAL_RADIUS,
        (frequency - Frequency.MIN) / Frequency.STEP,
        Frequency.stepsAcross());
    context.drawCenteredTextWithShadow(
        textRenderer,
        Text.translatable("screen.hardwired.radio.kilohertz", frequency),
        width / 2,
        top() + READOUT_Y,
        SlatePlate.TITLE_COLOR);
    Text disc =
        desk.hasDisc()
            ? desk.song().map(song -> song.value().description()).orElse(desk.disc().getName())
            : Text.translatable("screen.hardwired.desk.no_disc");
    context.drawCenteredTextWithShadow(
        textRenderer,
        Text.translatable(desk.status().translationKey()).append(" | ").append(disc),
        width / 2,
        top() + STATUS_Y,
        SlatePlate.MUTED_COLOR);
  }

  @Override
  public boolean shouldPause() {
    return false;
  }
}
