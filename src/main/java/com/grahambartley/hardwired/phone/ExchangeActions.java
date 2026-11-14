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
import java.util.Optional;
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
    if (PhoneDirectory.readiness(world, anchor).isStopped()) {
      ElectronicsDriver.tell(world, anchor, player);
      return;
    }
    refusal(player, anchor)
        .ifPresentOrElse(
            key -> player.sendMessage(Text.translatable(key), true), () -> send(player, anchor));
  }

  public static void share(ServerPlayerEntity player, ExchangeSharePayload action) {
    BlockPos anchor = action.exchange();
    ServerWorld world = player.getServerWorld();
    if (!(world.getBlockState(anchor).getBlock() instanceof ExchangeBlock)
        || !DeviceActions.canUse(player, anchor)
        || PhoneDirectory.readiness(world, anchor).isStopped()
        || refusal(player, anchor).isPresent()) {
      return;
    }
    PhoneDirectory directory = PhoneDirectory.of(world);
    Map<Long, Integer> numbers = numbersOf(directory, anchor.asLong());
    List<Long> chosen = action.phones().stream().map(BlockPos::asLong).toList();
    Map<Long, Long> orders = new HashMap<>();
    numbers
        .keySet()
        .forEach(
            phone -> directory.entry(phone).ifPresent(entry -> orders.put(phone, entry.order())));
    Map<Long, Integer> changed =
        action.share()
            ? PartyLines.share(numbers, chosen)
            : PartyLines.part(numbers, orders, chosen);
    if (changed.isEmpty()) {
      player.sendMessage(
          action.share()
              ? Text.translatable("screen.hardwired.exchange.pick", PartyLines.MOST_PHONES)
              : Text.translatable("screen.hardwired.exchange.not_shared"),
          true);
    }
    changed.forEach((phone, number) -> directory.renumber(phone, number, anchor.asLong()));
    directory.refresh();
    send(player, anchor);
  }

  private static Optional<String> refusal(ServerPlayerEntity player, BlockPos anchor) {
    if (!player.canModifyBlocks()) {
      return Optional.of("screen.hardwired.exchange.needs_build");
    }
    WireViewer viewer = WireViewer.of(player);
    boolean mayChange =
        PhoneDirectory.of(player.getServerWorld())
            .systemOf(anchor.asLong())
            .map(viewer::canSee)
            .orElse(true);
    return mayChange ? Optional.empty() : Optional.of("screen.hardwired.exchange.not_yours");
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
