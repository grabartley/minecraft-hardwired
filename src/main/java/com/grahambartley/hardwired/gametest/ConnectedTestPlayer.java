package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.network.CircuitResultPayload;
import com.grahambartley.hardwired.network.MainPanelViewPayload;
import com.grahambartley.hardwired.network.WireChunkPayload;
import com.grahambartley.hardwired.network.WireChunkPayload.CellView;
import com.grahambartley.hardwired.network.WireInspectResultPayload;
import com.grahambartley.hardwired.network.WireViewClearPayload;
import com.grahambartley.hardwired.wire.WireFace;
import com.grahambartley.hardwired.wire.WireView;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public final class ConnectedTestPlayer {

  private static final String PLAYER_NAME = "seal-test-player";
  private static final int NOT_OPERATOR = 0;
  private static final int OPERATOR = 4;
  private static final double FACE_OFFSET = 0.5;

  private static final class Player extends ServerPlayerEntity {

    private int permission = NOT_OPERATOR;

    private Player(MinecraftServer server, TestContext context, GameProfile profile) {
      super(server, context.getWorld(), profile, SyncedClientOptions.createDefault());
    }

    @Override
    public int getPermissionLevel() {
      return permission;
    }
  }

  private final MinecraftServer server;
  private final Player player;
  private final EmbeddedChannel channel;

  private ConnectedTestPlayer(MinecraftServer server, Player player, EmbeddedChannel channel) {
    this.server = server;
    this.player = player;
    this.channel = channel;
  }

  public static ConnectedTestPlayer joinAt(TestContext context, BlockPos relativePos) {
    MinecraftServer server = context.getWorld().getServer();
    GameProfile profile = new GameProfile(UUID.randomUUID(), PLAYER_NAME);
    Player player = new Player(server, context, profile);
    ClientConnection connection = new ClientConnection(NetworkSide.SERVERBOUND);
    EmbeddedChannel channel = new EmbeddedChannel(connection);
    server
        .getPlayerManager()
        .onPlayerConnect(connection, player, ConnectedClientData.createDefault(profile, false));
    player.changeGameMode(GameMode.SURVIVAL);
    ConnectedTestPlayer joined = new ConnectedTestPlayer(server, player, channel);
    joined.confirmTeleports();
    Vec3d feet = Vec3d.ofBottomCenter(context.getAbsolutePos(relativePos));
    player.refreshPositionAndAngles(feet.x, feet.y, feet.z, 0.0F, 0.0F);
    return joined;
  }

  private void confirmTeleports() {
    for (Object message : channel.outboundMessages()) {
      if (message instanceof PlayerPositionLookS2CPacket packet) {
        player.networkHandler.onTeleportConfirm(
            new TeleportConfirmC2SPacket(packet.getTeleportId()));
      }
    }
  }

  public ServerPlayerEntity player() {
    return player;
  }

  public UUID id() {
    return player.getUuid();
  }

  public void setOperator(boolean operator) {
    player.permission = operator ? OPERATOR : NOT_OPERATOR;
  }

  public void setChatVisibility(ChatVisibility visibility) {
    SyncedClientOptions base = SyncedClientOptions.createDefault();
    player.setClientOptions(
        new SyncedClientOptions(
            base.language(),
            base.viewDistance(),
            visibility,
            base.chatColorsEnabled(),
            base.playerModelParts(),
            base.mainArm(),
            base.filtersText(),
            base.allowsServerListing()));
  }

  public List<Text> intercomLines() {
    channel.flushOutbound();
    List<Text> lines = new ArrayList<>();
    for (Object message : channel.outboundMessages()) {
      if (message instanceof GameMessageS2CPacket packet
          && !packet.overlay()
          && packet.content().getContent() instanceof TranslatableTextContent content
          && content.getKey().equals("chat.hardwired.intercom")) {
        lines.add(packet.content());
      }
    }
    return lines;
  }

  public void hold(ItemStack main, ItemStack off) {
    player.setStackInHand(Hand.MAIN_HAND, main);
    player.setStackInHand(Hand.OFF_HAND, off);
  }

  public void useOn(WireFace face) {
    Vec3d point =
        Vec3d.ofCenter(face.pos()).add(Vec3d.of(face.side().getVector()).multiply(FACE_OFFSET));
    BlockHitResult hit = new BlockHitResult(point, face.side(), face.pos(), false);
    player.networkHandler.onPlayerInteractBlock(
        new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, 0));
  }

  public void command(String command) {
    server.getCommandManager().executeWithPrefix(player.getCommandSource(), command);
  }

  public List<CellView> receivedCellsAt(BlockPos pos) {
    List<CellView> cells = new ArrayList<>();
    for (WireChunkPayload payload : received(WireChunkPayload.class)) {
      payload.cells().stream().filter(cell -> cell.pos() == pos.asLong()).forEach(cells::add);
    }
    return cells;
  }

  public List<CellView> shownCellsAt(BlockPos pos) {
    long chunk = new ChunkPos(pos).toLong();
    List<CellView> shown = List.of();
    for (CustomPayload payload : received(CustomPayload.class)) {
      if (payload instanceof WireViewClearPayload) {
        shown = List.of();
      } else if (payload instanceof WireChunkPayload view && view.chunk() == chunk) {
        shown = view.cells().stream().filter(cell -> cell.pos() == pos.asLong()).toList();
      }
    }
    return shown;
  }

  public List<MainPanelViewPayload> panelViews() {
    return received(MainPanelViewPayload.class);
  }

  public List<CircuitResultPayload> circuitResults() {
    return received(CircuitResultPayload.class);
  }

  public boolean hasEarned(Identifier advancement) {
    return player
        .getAdvancementTracker()
        .getProgress(server.getAdvancementLoader().get(advancement))
        .isDone();
  }

  public int stepsEarned(Identifier advancement) {
    int steps = 0;
    Iterable<String> obtained =
        player
            .getAdvancementTracker()
            .getProgress(server.getAdvancementLoader().get(advancement))
            .getObtainedCriteria();
    for (String ignored : obtained) {
      steps++;
    }
    return steps;
  }

  public int inspectResults() {
    return received(WireInspectResultPayload.class).size();
  }

  private <T extends CustomPayload> List<T> received(Class<T> type) {
    channel.flushOutbound();
    List<T> payloads = new ArrayList<>();
    for (Object message : channel.outboundMessages()) {
      if (message instanceof CustomPayloadS2CPacket packet && type.isInstance(packet.payload())) {
        payloads.add(type.cast(packet.payload()));
      }
    }
    return payloads;
  }

  public void leave() {
    WireView.forget(player);
    if (server.getPlayerManager().getPlayer(player.getUuid()) == player) {
      server.getPlayerManager().remove(player);
    }
  }
}
