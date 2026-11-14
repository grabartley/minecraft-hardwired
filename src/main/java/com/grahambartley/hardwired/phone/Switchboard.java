package com.grahambartley.hardwired.phone;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.block.ExchangeBlock;
import com.grahambartley.hardwired.config.PhoneSettings;
import com.grahambartley.hardwired.device.ActionRate;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WiredWorld;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public final class Switchboard {

  private static final int TICKS_PER_SECOND = 20;
  private static final int DIAL_GAP_TICKS = 20;

  private final ServerWorld world;
  private final List<Call> calls = new ArrayList<>();
  private final Map<Long, UUID> lifted = new HashMap<>();
  private final ActionRate dialRate = new ActionRate(DIAL_GAP_TICKS);
  private final LongOpenHashSet loadedChunks = new LongOpenHashSet();
  private Map<Long, Integer> lampsLit = Map.of();
  private final PhoneLine line;

  public Switchboard(ServerWorld world) {
    this.world = world;
    this.line = new PhoneLine(world);
  }

  public static Switchboard of(ServerWorld world) {
    return ((WiredWorld) world).hardwired$switchboard();
  }

  public Optional<Call> callOf(long phone) {
    return calls.stream().filter(call -> call.involves(phone)).findFirst();
  }

  public boolean isLifted(long phone) {
    return lifted.containsKey(phone) || callOf(phone).filter(c -> c.hasMember(phone)).isPresent();
  }

  public boolean isRinging(long phone) {
    return callOf(phone).filter(call -> call.isRinging(phone)).isPresent();
  }

  public boolean bellRings(BlockPos bell) {
    if (calls.stream().allMatch(Call::isConnected)) {
      return false;
    }
    Set<Long> touched =
        WireEngine.of(world).grid().networksTouching(bell).stream()
            .flatMap(network -> network.touches().stream())
            .map(touch -> touch.pos().asLong())
            .collect(Collectors.toSet());
    PhoneDirectory directory = PhoneDirectory.of(world);
    return BellRing.rings(
        calls,
        touched,
        phone -> directory.entry(phone).map(PhoneEntry::cells).orElse(List.of(phone)),
        world.getTime());
  }

  public Optional<UUID> userOf(long phone) {
    return Optional.ofNullable(lifted.get(phone))
        .or(() -> callOf(phone).map(call -> call.members().get(phone)));
  }

  public LineStatus pickUp(ServerPlayerEntity player, long phone) {
    Optional<Call> call = callOf(phone);
    Optional<Call> numbered = numberCall(phone);
    LineRules.PickUp outcome =
        LineRules.pickUp(
            call.filter(c -> c.isRinging(phone)).isPresent(),
            userOf(phone).map(player.getUuid()::equals),
            numbered.isPresent());
    return switch (outcome) {
      case ANSWER -> answer(player, phone, call.orElseThrow());
      case BUSY -> LineStatus.BUSY;
      case RESUME ->
          call.map(c -> c.isConnected() ? LineStatus.CONNECTED : LineStatus.RINGING_OUT)
              .orElse(LineStatus.DIAL_TONE);
      case JOIN -> join(player, phone, numbered.orElseThrow());
      case LIFT -> {
        lifted.put(phone, player.getUuid());
        line.show(phone);
        yield LineStatus.DIAL_TONE;
      }
    };
  }

  public LineStatus dial(ServerPlayerEntity player, long phone, int number) {
    boolean holding = player.getUuid().equals(lifted.get(phone));
    boolean rateAllows = holding && dialRate.allow(player.getUuid(), world.getTime());
    PhoneDirectory directory = PhoneDirectory.of(world);
    List<Long> targets = directory.phonesNumbered(phone, number);
    List<Long> answerable = targets.stream().filter(line::answerable).toList();
    Set<Long> exchanges = new HashSet<>();
    directory.entry(phone).ifPresent(entry -> exchanges.add(entry.numberedBy()));
    targets.forEach(
        target -> directory.entry(target).ifPresent(entry -> exchanges.add(entry.numberedBy())));
    LineStatus status =
        LineRules.dial(
            new LineRules.Dial(
                holding,
                rateAllows,
                !targets.isEmpty(),
                targets.contains(phone),
                targets.stream().anyMatch(this::isBusy),
                !answerable.isEmpty(),
                line.exchangesLive(exchanges),
                ExchangeLines.free(calls, exchanges, settings().exchangeLines())));
    if (status == LineStatus.RINGING_OUT) {
      lifted.remove(phone);
      calls.add(new Call(phone, player.getUuid(), answerable, exchanges, world.getTime()));
      answerable.forEach(line::show);
    }
    return status;
  }

  public void hangUp(long phone) {
    lifted.remove(phone);
    callOf(phone).ifPresent(call -> leave(call, phone, LineStatus.HUNG_UP));
    line.show(phone);
  }

  public void removed(long phone) {
    Optional<UUID> user = userOf(phone);
    lifted.remove(phone);
    callOf(phone).ifPresent(call -> leave(call, phone, LineStatus.LINE_DEAD));
    user.ifPresent(id -> line.tell(phone, id, LineStatus.LINE_DEAD));
  }

  public void forget(UUID player) {
    for (Map.Entry<Long, UUID> held : List.copyOf(lifted.entrySet())) {
      if (held.getValue().equals(player)) {
        hangUp(held.getKey());
      }
    }
    for (Call call : List.copyOf(calls)) {
      call.members().entrySet().stream()
          .filter(member -> member.getValue().equals(player))
          .map(Map.Entry::getKey)
          .toList()
          .forEach(this::hangUp);
    }
  }

  public void chunkLoaded(ChunkPos chunk) {
    loadedChunks.add(chunk.toLong());
  }

  public void tick() {
    lightExchanges();
    settleLoadedPhones();
    for (Map.Entry<Long, UUID> held : List.copyOf(lifted.entrySet())) {
      line.fault(held.getKey(), held.getValue())
          .ifPresent(
              fault -> {
                hangUp(held.getKey());
                line.tell(held.getKey(), held.getValue(), fault);
              });
    }
    long now = world.getTime();
    long ringTicks = (long) settings().phoneRingSeconds() * TICKS_PER_SECOND;
    for (Call call : List.copyOf(calls)) {
      if (!line.exchangesLive(call.exchanges())) {
        end(call, LineStatus.LINE_DEAD);
        continue;
      }
      for (Map.Entry<Long, UUID> member : List.copyOf(call.members().entrySet())) {
        Optional<LineStatus> fault =
            calls.contains(call)
                ? line.fault(member.getKey(), member.getValue())
                : Optional.empty();
        if (fault.isPresent()) {
          leave(
              call,
              member.getKey(),
              fault.get() == LineStatus.LEFT ? LineStatus.HUNG_UP : fault.get());
          line.tell(member.getKey(), member.getValue(), fault.get());
          line.show(member.getKey());
        }
      }
      if (!calls.contains(call) || dropDarkPhones(call)) {
        continue;
      }
      if (call.unanswered(now, ringTicks)) {
        end(call, LineStatus.NO_ANSWER);
      } else if (!call.isConnected()) {
        line.ring(call, now);
      }
    }
  }

  private void lightExchanges() {
    Map<Long, Integer> lit = ExchangeLines.lampsLit(calls, ExchangeBlock.LAMPS);
    Set<Long> exchanges = new HashSet<>(lampsLit.keySet());
    exchanges.addAll(lit.keySet());
    for (long exchange : exchanges) {
      int lines = lit.getOrDefault(exchange, 0);
      if (lines != lampsLit.getOrDefault(exchange, 0)) {
        line.lightExchange(exchange, lines);
      }
    }
    lampsLit = lit;
  }

  private void settleLoadedPhones() {
    if (loadedChunks.isEmpty()) {
      return;
    }
    PhoneBook.of(world)
        .entries()
        .forEach(
            (device, entry) -> {
              if (!loadedChunks.contains(ChunkPos.toLong(BlockPos.fromLong(device)))) {
                return;
              }
              if (entry.exchange()) {
                line.lightExchange(device, lampsLit.getOrDefault(device, 0));
              } else {
                line.show(device);
              }
            });
    loadedChunks.clear();
  }

  private boolean dropDarkPhones(Call call) {
    for (long phone : List.copyOf(call.called())) {
      if (!call.isConnected() && !line.answerable(phone)) {
        boolean none = call.stopRinging(phone);
        line.show(phone);
        if (none) {
          end(call, LineStatus.LINE_DEAD);
          return true;
        }
      }
    }
    return false;
  }

  private boolean isBusy(long phone) {
    return lifted.containsKey(phone) || callOf(phone).isPresent();
  }

  private Optional<Call> numberCall(long phone) {
    return calls.stream()
        .filter(Call::isConnected)
        .filter(call -> call.called().contains(phone) && !call.hasMember(phone))
        .findFirst();
  }

  private LineStatus answer(ServerPlayerEntity player, long phone, Call call) {
    if (!call.answer(phone, player.getUuid())) {
      return LineStatus.BUSY;
    }
    ServerPlayerEntity caller = line.player(call.members().get(call.caller()));
    if (caller != null) {
      ModCriteria.DEVICE.trigger(caller, ModCriteria.NUMBER_PLEASE);
      if (call.exchanges().size() > 1) {
        ModCriteria.DEVICE.trigger(caller, ModCriteria.LONG_DISTANCE);
      }
    }
    call.called().forEach(line::show);
    line.tell(call, LineStatus.CONNECTED);
    return LineStatus.CONNECTED;
  }

  private LineStatus join(ServerPlayerEntity player, long phone, Call call) {
    boolean openWire =
        PhoneDirectory.of(world).systemOf(phone).map(network -> !network.isSealed()).orElse(false);
    if (!call.join(phone, player.getUuid(), openWire)) {
      return LineStatus.BUSY;
    }
    ModCriteria.DEVICE.trigger(player, ModCriteria.PARTY_LINE);
    line.show(phone);
    line.click(call);
    line.tell(call, LineStatus.CONNECTED);
    return LineStatus.CONNECTED;
  }

  private void leave(Call call, long phone, LineStatus reason) {
    if (call.leave(phone)) {
      end(call, reason);
    } else {
      line.show(phone);
    }
  }

  private void end(Call call, LineStatus reason) {
    calls.remove(call);
    for (Map.Entry<Long, UUID> member : call.members().entrySet()) {
      lifted.put(member.getKey(), member.getValue());
    }
    line.tell(call, reason);
    call.called().forEach(line::show);
    call.members().keySet().forEach(line::show);
  }

  private static PhoneSettings settings() {
    return Hardwired.SERVER_CONFIG.phone();
  }
}
