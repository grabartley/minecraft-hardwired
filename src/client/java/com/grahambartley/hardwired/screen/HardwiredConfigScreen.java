package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.grahambartley.hardwired.config.ConfigRefusals;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.config.SyncedServerConfig;
import com.grahambartley.hardwired.network.ServerConfigPayloads.EditServerConfigC2SPayload;
import com.grahambartley.hardwired.server.ServerConfigEditPermission;
import java.util.Optional;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public final class HardwiredConfigScreen extends Screen {
  private static final int ROW_HEIGHT = 24;
  private static final int DONE_CANCEL_WIDTH = 97;
  private static final int DONE_CANCEL_GAP = 6;
  private static final int BANNER_INSET = 8;
  private static final int BANNER_COLOR = 0xFFAA00;
  private static final int HINT_COLOR = 0xBBBBBB;

  private final Screen parent;
  private final SettingsConfigSection settings;
  private final ClientConfigSection clientSettings = new ClientConfigSection();
  private ButtonWidget doneButton;
  private Optional<Text> shownRefusal = Optional.empty();

  public HardwiredConfigScreen(@Nullable Screen parent) {
    super(Text.translatable("screen.hardwired.settings.title"));
    this.parent = parent;
    this.settings =
        new SettingsConfigSection(
            resolveAccess() == EditAccess.NO_WORLD
                ? HardwiredConfig.defaults()
                : SyncedServerConfig.get());
  }

  @Override
  protected void init() {
    final boolean canEdit = resolveAccess() == EditAccess.EDITABLE;
    final ConfigScreenLayout layout = ConfigScreenLayout.of(this.height, !canEdit);
    final HardwiredConfigList list =
        new HardwiredConfigList(
            this.client, this.width, layout, ROW_HEIGHT, ConfigRows.CONTENT_WIDTH);
    final ConfigRows rows = new ConfigRows(canEdit, this.textRenderer);

    this.clientSettings.addTo(list, new ConfigRows(true, this.textRenderer));
    this.settings.addTo(list, rows);
    addDrawableChild(list);

    final int doneLeft = (this.width - (DONE_CANCEL_WIDTH * 2 + DONE_CANCEL_GAP)) / 2;
    final int cancelLeft = doneLeft + DONE_CANCEL_WIDTH + DONE_CANCEL_GAP;
    this.doneButton =
        ButtonWidget.builder(
                Text.translatable("screen.hardwired.settings.done"), button -> saveAndClose())
            .dimensions(
                doneLeft, layout.buttonsY(), DONE_CANCEL_WIDTH, ConfigScreenLayout.BUTTON_HEIGHT)
            .build();
    addDrawableChild(this.doneButton);
    this.shownRefusal = Optional.empty();
    showRefusal();

    addDrawableChild(
        ButtonWidget.builder(
                Text.translatable("screen.hardwired.settings.cancel"), button -> close())
            .dimensions(
                cancelLeft, layout.buttonsY(), DONE_CANCEL_WIDTH, ConfigScreenLayout.BUTTON_HEIGHT)
            .build());
  }

  @Override
  public void tick() {
    super.tick();
    showRefusal();
  }

  private void showRefusal() {
    final Optional<Text> refusal =
        resolveAccess() == EditAccess.EDITABLE
            ? ConfigRefusals.of(this.settings.config())
            : Optional.empty();
    if (refusal.equals(this.shownRefusal)) {
      return;
    }
    this.shownRefusal = refusal;
    this.doneButton.active = refusal.isEmpty();
    this.doneButton.setTooltip(refusal.map(Tooltip::of).orElse(null));
  }

  private void saveAndClose() {
    ClientSettingsStore.update(this.clientSettings.settings(), this.clientSettings.radioSettings());
    if (resolveAccess() == EditAccess.EDITABLE) {
      ClientPlayNetworking.send(new EditServerConfigC2SPayload(this.settings.config()));
    }
    close();
  }

  @Override
  public void close() {
    MinecraftClient.getInstance().setScreen(this.parent);
  }

  @Override
  public void render(
      final DrawContext context, final int mouseX, final int mouseY, final float delta) {
    super.render(context, mouseX, mouseY, delta);
    PanelText.shadowed(
        context,
        this.textRenderer,
        this.title,
        BANNER_INSET,
        this.width - BANNER_INSET,
        ConfigScreenLayout.TITLE_Y,
        0xFFFFFF,
        PanelText.Align.CENTER);
    final EditAccess access = resolveAccess();
    if (access != EditAccess.EDITABLE) {
      PanelText.shadowed(
          context,
          this.textRenderer,
          Text.translatable(access.bannerKey()),
          BANNER_INSET,
          this.width - BANNER_INSET,
          ConfigScreenLayout.BANNER_Y,
          BANNER_COLOR,
          PanelText.Align.CENTER);
      PanelText.shadowed(
          context,
          this.textRenderer,
          Text.translatable(access.hintKey()),
          BANNER_INSET,
          this.width - BANNER_INSET,
          ConfigScreenLayout.BANNER_Y + ConfigScreenLayout.BANNER_LINE_HEIGHT,
          HINT_COLOR,
          PanelText.Align.CENTER);
    }
  }

  private static EditAccess resolveAccess() {
    final MinecraftClient client = MinecraftClient.getInstance();
    final ClientPlayerEntity player = client.player;
    return EditAccess.resolve(
        player != null,
        player != null && ServerConfigEditPermission.canEdit(player, client.getServer()));
  }

  public enum EditAccess {
    EDITABLE("", ""),
    READ_ONLY(
        "screen.hardwired.settings.readonly_banner", "screen.hardwired.settings.readonly_hint"),
    NO_WORLD("screen.hardwired.settings.noworld_banner", "screen.hardwired.settings.noworld_hint");

    private final String bannerKey;
    private final String hintKey;

    EditAccess(final String bannerKey, final String hintKey) {
      this.bannerKey = bannerKey;
      this.hintKey = hintKey;
    }

    public static EditAccess resolve(final boolean inWorld, final boolean canEdit) {
      if (!inWorld) {
        return NO_WORLD;
      }
      return canEdit ? EDITABLE : READ_ONLY;
    }

    public String bannerKey() {
      return bannerKey;
    }

    public String hintKey() {
      return hintKey;
    }
  }
}
