package com.grahambartley.hardwired.phone;

import com.grahambartley.hardwired.block.TelephoneBlock;
import com.grahambartley.hardwired.device.DeviceActions;
import com.grahambartley.hardwired.device.DeviceChat;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.network.PhoneActionPayload;
import com.grahambartley.hardwired.network.PhoneSaidPayload;
import java.util.Optional;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class PhoneActions {

  private static final DeviceChat CHAT = new DeviceChat();

  private PhoneActions() {}

  public static void use(ServerPlayerEntity player, BlockPos pos) {
    ServerWorld world = player.getServerWorld();
    ElectronicsDriver.pulse(world, pos);
    if (TelephoneBlock.readiness(world, pos).isStopped()) {
      ElectronicsDriver.tell(world, pos, player);
      return;
    }
    LineStatus status = Switchboard.of(world).pickUp(player, pos.asLong());
    if (status != LineStatus.BUSY) {
      click(world, pos);
    }
    PhoneViews.send(player, pos.asLong(), status);
  }

  public static void act(ServerPlayerEntity player, PhoneActionPayload action) {
    ServerWorld world = player.getServerWorld();
    BlockPos pos = action.phone();
    long phone = pos.asLong();
    Switchboard board = Switchboard.of(world);
    if (!board.userOf(phone).map(player.getUuid()::equals).orElse(false)) {
      return;
    }
    switch (action.action()) {
      case HANG_UP -> {
        board.hangUp(phone);
        click(world, pos);
      }
      case DIAL -> {
        if (DeviceActions.canUse(player, pos)) {
          PhoneViews.send(player, phone, board.dial(player, phone, action.number()));
        }
      }
      case SAY -> say(player, board, phone, action.line());
    }
  }

  public static void forget(ServerPlayerEntity player) {
    CHAT.forget(player);
    CallVoice.forget(player);
    Switchboard.of(player.getServerWorld()).forget(player.getUuid());
  }

  private static void say(ServerPlayerEntity player, Switchboard board, long phone, String raw) {
    Optional<Call> call = board.callOf(phone).filter(Call::isConnected);
    if (call.isEmpty() || !call.get().hasMember(phone)) {
      return;
    }
    Optional<String> line = CHAT.accept(player, raw);
    if (line.isEmpty()) {
      return;
    }
    Text said =
        Text.translatable(
            "chat.hardwired.telephone", player.getDisplayName(), Text.literal(line.get()));
    player.getServer().sendMessage(said);
    for (var member : call.get().members().entrySet()) {
      ServerPlayerEntity listener =
          player.getServer().getPlayerManager().getPlayer(member.getValue());
      if (listener != null && DeviceChat.reads(listener)) {
        ServerPlayNetworking.send(
            listener, new PhoneSaidPayload(BlockPos.fromLong(member.getKey()), said));
      }
    }
  }

  private static void click(ServerWorld world, BlockPos pos) {
    world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.4F, 1.2F);
  }
}
