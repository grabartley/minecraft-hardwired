package com.grahambartley.hardwired.phone;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.config.PhoneSettings;
import com.grahambartley.hardwired.device.ActionRate;
import com.grahambartley.hardwired.wire.WiredWorld;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class Switchboard {

  private static final int TICKS_PER_SECOND = 20;
  private static final int DIAL_GAP_TICKS = 20;

  private final ServerWorld world;
  private final List<Call> calls = new ArrayList<>();
  private final Map<Long, UUID> lifted = new HashMap<>();
  private final ActionRate dialRate = new ActionRate(DIAL_GAP_TICKS);
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

  public Optional<UUID> userOf(long phone) {
    return Optional.ofNullable(lifted.get(phone))
        .or(() -> callOf(phone).map(call -> call.members().get(phone)));
  }

  public LineStatus pickUp(ServerPlayerEntity player, long phone) {
    Optional<Call> call = callOf(phone);
    if (call.isPresent() && call.get().isRinging(phone)) {
      return answer(player, phone, call.get());
    }
    Optional<UUID> user = userOf(phone);
    if (user.isPresent()) {
      if (!user.get().equals(player.getUuid())) {
        return LineStatus.BUSY;
      }
      return call.map(c -> c.isConnected() ? LineStatus.CONNECTED : LineStatus.RINGING_OUT)
          .orElse(LineStatus.DIAL_TONE);
    }
    Optional<Call> numbered = numberCall(phone);
    if (numbered.isPresent()) {
      return join(player, phone, numbered.get());
    }
    lifted.put(phone, player.getUuid());
    line.show(phone);
    return LineStatus.DIAL_TONE;
  }

  public LineStatus dial(ServerPlayerEntity player, long phone, int number) {
    if (!player.getUuid().equals(lifted.get(phone))) {
      return LineStatus.LINE_DEAD;
    }
    if (!dialRate.allow(player.getUuid(), world.getTime())) {
      return LineStatus.TOO_FAST;
    }
    PhoneDirectory directory = PhoneDirectory.of(world);
    List<Long> targets = directory.phonesNumbered(phone, number);
    if (targets.isEmpty()) {
      return LineStatus.NO_SUCH_NUMBER;
    }
    if (targets.contains(phone) || targets.stream().anyMatch(this::isBusy)) {
      return LineStatus.BUSY;
    }
    Set<Long> exchanges = new HashSet<>();
    directory.entry(phone).ifPresent(entry -> exchanges.add(entry.numberedBy()));
    targets.forEach(
        target -> directory.entry(target).ifPresent(entry -> exchanges.add(entry.numberedBy())));
    if (!line.exchangesLive(exchanges)) {
      return LineStatus.LINE_DEAD;
    }
    if (!ExchangeLines.free(calls, exchanges, settings().exchangeLines())) {
      return LineStatus.BUSY;
    }
    boolean joinable = directory.systemOf(phone).map(network -> !network.isSealed()).orElse(true);
    lifted.remove(phone);
    calls.add(new Call(phone, player.getUuid(), targets, exchanges, joinable, world.getTime()));
    targets.forEach(line::show);
    return LineStatus.RINGING_OUT;
  }

  public void hangUp(long phone) {
    lifted.remove(phone);
    callOf(phone).ifPresent(call -> leave(call, phone, LineStatus.HUNG_UP));
    line.show(phone);
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

  public void tick() {
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
          leave(call, member.getKey(), fault.get());
          line.tell(member.getKey(), member.getValue(), fault.get());
          line.show(member.getKey());
        }
      }
      if (!calls.contains(call)) {
        continue;
      }
      if (call.unanswered(now, ringTicks)) {
        end(call, LineStatus.NO_ANSWER);
      } else if (!call.isConnected()) {
        line.ring(call, now);
      }
    }
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
    if (!call.join(phone, player.getUuid())) {
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
