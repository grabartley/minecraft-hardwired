package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.block.JunctionBlock;
import com.grahambartley.hardwired.block.JunctionRule;
import com.grahambartley.hardwired.network.JunctionSetPayload;
import com.grahambartley.hardwired.wire.Hue;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class JunctionScreen extends Screen {

  private static final int BUTTON_WIDTH = 200;
  private static final int BUTTON_HEIGHT = 20;

  private final BlockPos pos;
  private JunctionRule rule;
  private Hue output;

  public JunctionScreen(BlockPos pos, BlockState state) {
    super(Text.translatable("block.hardwired.junction"));
    this.pos = pos;
    this.rule = state.get(JunctionBlock.RULE);
    this.output = state.get(JunctionBlock.OUTPUT);
  }

  @Override
  protected void init() {
    int x = (width - BUTTON_WIDTH) / 2;
    int y = height / 2 - BUTTON_HEIGHT;
    addDrawableChild(
        CyclingButtonWidget.<JunctionRule>builder(
                value -> Text.translatable(value.translationKey()))
            .values(JunctionRule.values())
            .initially(rule)
            .build(
                x,
                y,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                Text.translatable("screen.hardwired.rule"),
                (button, value) -> {
                  rule = value;
                  send();
                }));
    addDrawableChild(
        CyclingButtonWidget.<Hue>builder(value -> Text.translatable(value.wireKey()))
            .values(Hue.values())
            .initially(output)
            .build(
                x,
                y + BUTTON_HEIGHT + 4,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                Text.translatable("screen.hardwired.output"),
                (button, value) -> {
                  output = value;
                  send();
                }));
  }

  private void send() {
    ClientPlayNetworking.send(new JunctionSetPayload(pos, rule.ordinal(), output.ordinal()));
  }

  @Override
  public void render(
      net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);
    context.drawCenteredTextWithShadow(
        textRenderer, title, width / 2, height / 2 - BUTTON_HEIGHT - 16, 0xFFFFFFFF);
  }

  @Override
  public boolean shouldPause() {
    return false;
  }
}
