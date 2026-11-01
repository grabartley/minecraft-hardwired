package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.block.RadioBlockEntity;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.network.RadioSetPayload;
import com.grahambartley.hardwired.radio.Frequency;
import com.grahambartley.hardwired.radio.ReceptionMix;
import com.grahambartley.hardwired.radio.Signal;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class RadioScreen extends Screen {

  private static final int WIDTH = 220;
  private static final int HEIGHT = 170;
  private static final int PADDING = 12;
  private static final int TITLE_Y = 10;
  private static final int DIAL_Y = 48;
  private static final int DIAL_RADIUS = 20;
  private static final int LABEL_Y = 72;
  private static final int READOUT_Y = 84;
  private static final int TUNING_Y = 96;
  private static final int VOLUME_Y = 120;
  private static final int POWER_Y = 144;
  private static final int ROW_HEIGHT = 20;
  private static final int POWER_WIDTH = 48;
  private static final int STATUS_X = 66;
  private static final int STATUS_Y = 150;

  private final BlockPos pos;
  private final RadioBlockEntity radio;
  private boolean on;
  private int frequency;
  private int volume;
  private ButtonWidget power;

  public RadioScreen(BlockPos pos, RadioBlockEntity radio) {
    super(Text.translatable("block.hardwired.radio"));
    this.pos = pos;
    this.radio = radio;
    this.on = radio.isOn();
    this.frequency = radio.frequency();
    this.volume = radio.volume();
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
          send();
        });
    ConfigIntSlider slider =
        new ConfigIntSlider(
            inner,
            ROW_HEIGHT,
            Text.translatable("screen.hardwired.radio.volume"),
            "%",
            0,
            ReceptionMix.MAX_VOLUME,
            volume,
            next -> {
              volume = next;
              send();
            });
    slider.setPosition(x, top() + VOLUME_Y);
    addDrawableChild(slider);
    power =
        ButtonWidget.builder(powerLabel(), pressed -> toggle())
            .dimensions(x, top() + POWER_Y, POWER_WIDTH, ROW_HEIGHT)
            .build();
    addDrawableChild(power);
  }

  private Text powerLabel() {
    return Text.translatable(on ? "screen.hardwired.radio.on" : "screen.hardwired.radio.off");
  }

  private void toggle() {
    on = !on;
    power.setMessage(powerLabel());
    send();
  }

  private void send() {
    ClientPlayNetworking.send(new RadioSetPayload(pos, on, frequency, volume));
  }

  @Override
  public void tick() {
    if (client.world.getBlockEntity(pos) != radio) {
      close();
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
    int tuningX = width / 2 - WIDTH / 4;
    int signalX = width / 2 + WIDTH / 4;
    int dialY = top() + DIAL_Y;
    fit(context, title, PADDING, WIDTH - PADDING, TITLE_Y, SlatePlate.TITLE_COLOR);
    BrassDial.draw(
        context,
        tuningX,
        dialY,
        DIAL_RADIUS,
        (frequency - Frequency.MIN) / Frequency.STEP,
        Frequency.stepsAcross());
    int needle = radio.status() == DeviceStatus.RUNNING ? Signal.needle(radio.strength()) : 0;
    BrassDial.draw(context, signalX, dialY, DIAL_RADIUS, needle, Signal.NEEDLE_STEPS);
    fit(
        context,
        Text.translatable("screen.hardwired.radio.tuning"),
        PADDING,
        WIDTH / 2,
        LABEL_Y,
        SlatePlate.MUTED_COLOR);
    fit(
        context,
        Text.translatable("screen.hardwired.radio.signal"),
        WIDTH / 2,
        WIDTH - PADDING,
        LABEL_Y,
        SlatePlate.MUTED_COLOR);
    fit(
        context,
        Text.translatable("screen.hardwired.radio.kilohertz", frequency),
        PADDING,
        WIDTH - PADDING,
        READOUT_Y,
        SlatePlate.TITLE_COLOR);
    PanelText.shadowed(
        context,
        textRenderer,
        Text.translatable(radio.status().translationKey()),
        left() + STATUS_X,
        left() + WIDTH - PADDING,
        top() + STATUS_Y,
        SlatePlate.MUTED_COLOR,
        PanelText.Align.LEFT);
  }

  private void fit(DrawContext context, Text text, int from, int to, int y, int color) {
    PanelText.shadowed(
        context,
        textRenderer,
        text,
        left() + from,
        left() + to,
        top() + y,
        color,
        PanelText.Align.CENTER);
  }

  @Override
  public boolean shouldPause() {
    return false;
  }
}
