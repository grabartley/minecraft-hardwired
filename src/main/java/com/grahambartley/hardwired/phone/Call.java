package com.grahambartley.hardwired.phone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Call {

  private final long caller;
  private final List<Long> called;
  private final Set<Long> exchanges;
  private final long startedAt;
  private final Map<Long, UUID> members = new LinkedHashMap<>();
  private boolean connected;

  public Call(
      long caller, UUID callerPlayer, List<Long> called, Set<Long> exchanges, long startedAt) {
    this.caller = caller;
    this.called = new ArrayList<>(called);
    this.exchanges = Set.copyOf(exchanges);
    this.startedAt = startedAt;
    members.put(caller, callerPlayer);
  }

  public long caller() {
    return caller;
  }

  public List<Long> called() {
    return Collections.unmodifiableList(called);
  }

  public Set<Long> exchanges() {
    return exchanges;
  }

  public boolean isConnected() {
    return connected;
  }

  public boolean isPartyLine() {
    return called.size() > 1;
  }

  public long ringingFor(long now) {
    return now - startedAt;
  }

  public boolean isRinging(long phone) {
    return !connected && called.contains(phone);
  }

  public boolean hasMember(long phone) {
    return members.containsKey(phone);
  }

  public boolean involves(long phone) {
    return hasMember(phone) || isRinging(phone);
  }

  public Map<Long, UUID> members() {
    return Collections.unmodifiableMap(members);
  }

  public boolean stopRinging(long phone) {
    if (connected) {
      return false;
    }
    called.remove(phone);
    return called.isEmpty();
  }

  public boolean answer(long phone, UUID player) {
    if (!isRinging(phone)) {
      return false;
    }
    members.put(phone, player);
    connected = true;
    return true;
  }

  public boolean join(long phone, UUID player, boolean openWire) {
    if (!connected || !openWire || !called.contains(phone) || members.containsKey(phone)) {
      return false;
    }
    members.put(phone, player);
    return true;
  }

  public boolean leave(long phone) {
    if (members.remove(phone) == null) {
      return false;
    }
    return !connected || members.size() < 2;
  }

  public boolean unanswered(long now, long ringTicks) {
    return !connected && ringingFor(now) >= ringTicks;
  }
}
