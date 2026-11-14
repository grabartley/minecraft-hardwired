package com.grahambartley.hardwired.phone;

import com.grahambartley.hardwired.block.ExchangeBlock;
import com.grahambartley.hardwired.device.DeviceActions;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.network.ExchangeSharePayload;
import com.grahambartley.hardwired.network.ExchangeViewPayload;
import com.grahambartley.hardwired.wire.WireViewer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class ExchangeActions {

  private ExchangeActions() {}

  public static void open(ServerPlayerEntity player, BlockPos anchor) {
    ServerWorld world = player.getServerWorld();
    ElectronicsDriver.pulse(world, anchor);
    if (ExchangeBlock.readiness(world, anchor).isStopped()) {
      ElectronicsDriver.tell(world, anchor, player);
      return;
    }
    if (!mayManage(player, anchor)) {
      player.sendMessage(Text.translatable("screen.hardwired.exchange.not_yours"), true);
      return;
    }
    send(player, anchor);
  }

  public static void share(ServerPlayerEntity player, ExchangeSharePayload action) {
    BlockPos anchor = action.exchange();
    ServerWorld world = player.getServerWorld();
    if (!(world.getBlockState(anchor).getBlock() instanceof ExchangeBlock)
        || !DeviceActions.canUse(player, anchor)
        || !mayManage(player, anchor)) {
      return;
    }
    PhoneDirectory directory = PhoneDirectory.of(world);
    Map<Long, Integer> numbers = numbersOf(directory, anchor.asLong());
    List<Long> chosen = action.phones().stream().map(BlockPos::asLong).toList();
    Map<Long, Integer> changed =
        action.share() ? PartyLines.share(numbers, chosen) : PartyLines.part(numbers, chosen);
    changed.forEach((phone, number) -> directory.renumber(phone, number, anchor.asLong()));
    directory.refresh();
    send(player, anchor);
  }

  private static boolean mayManage(ServerPlayerEntity player, BlockPos anchor) {
    WireViewer viewer = WireViewer.of(player);
    return player.canModifyBlocks()
        && PhoneDirectory.of(player.getServerWorld())
            .systemOf(anchor.asLong())
            .map(viewer::canSee)
            .orElse(true);
  }

  private static Map<Long, Integer> numbersOf(PhoneDirectory directory, long anchor) {
    Map<Long, Integer> numbers = new HashMap<>();
    for (long phone : directory.membersOf(anchor)) {
      directory
          .entry(phone)
          .filter(entry -> !entry.exchange() && entry.hasNumber() && entry.numberedBy() == anchor)
          .ifPresent(entry -> numbers.put(phone, entry.number()));
    }
    return numbers;
  }

  private static void send(ServerPlayerEntity player, BlockPos anchor) {
    PhoneDirectory directory = PhoneDirectory.of(player.getServerWorld());
    int digit =
        directory.entry(anchor.asLong()).map(PhoneEntry::number).orElse(PhoneNumbering.NONE);
    List<ExchangeViewPayload.Line> lines =
        numbersOf(directory, anchor.asLong()).entrySet().stream()
            .map(
                entry ->
                    new ExchangeViewPayload.Line(
                        BlockPos.fromLong(entry.getKey()),
                        entry.getValue(),
                        directory.entry(entry.getKey()).map(PhoneEntry::name).orElse("")))
            .sorted(
                Comparator.comparingInt(ExchangeViewPayload.Line::number)
                    .thenComparing(line -> line.phone().asLong()))
            .toList();
    ServerPlayNetworking.send(player, new ExchangeViewPayload(anchor, digit, lines));
  }
}
