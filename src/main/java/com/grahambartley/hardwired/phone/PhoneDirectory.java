package com.grahambartley.hardwired.phone;

import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireGrid;
import com.grahambartley.hardwired.wire.WireNetwork;
import com.grahambartley.hardwired.wire.WireViewer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.server.world.ServerWorld;

public final class PhoneDirectory {

  public record Listing(int number, String name) {}

  private final PhoneBook book;
  private final WireGrid grid;

  private PhoneDirectory(PhoneBook book, WireGrid grid) {
    this.book = book;
    this.grid = grid;
  }

  public static PhoneDirectory of(ServerWorld world) {
    return new PhoneDirectory(PhoneBook.of(world), WireEngine.of(world).grid());
  }

  public void refresh() {
    Map<Long, Long> anchors = anchorsByCell();
    Map<WireNetwork, List<Long>> systems = new IdentityHashMap<>();
    for (Map.Entry<Long, PhoneEntry> device : List.copyOf(book.entries().entrySet())) {
      long pos = device.getKey();
      PhoneEntry entry = device.getValue();
      Map<Hue, WireNetwork> touching = touching(entry);
      Set<Hue> withPartner =
          touching.entrySet().stream()
              .filter(network -> holdsPartner(network.getValue(), pos, entry, anchors))
              .map(Map.Entry::getKey)
              .collect(Collectors.toSet());
      Optional<Hue> home = PhoneHome.choose(entry.home(), withPartner);
      book.put(pos, entry.homedOn(home));
      home.ifPresent(
          hue -> systems.computeIfAbsent(touching.get(hue), key -> new ArrayList<>()).add(pos));
    }
    Map<Long, PhoneNumbering.Assignment> assigned = new HashMap<>();
    Map<Long, Integer> digits = new HashMap<>();
    systems.forEach(
        (network, members) -> {
          PhoneNumbering.Result result = numberSystem(network, members);
          assigned.putAll(result.phones());
          digits.putAll(result.digits());
        });
    for (Map.Entry<Long, PhoneEntry> device : List.copyOf(book.entries().entrySet())) {
      long pos = device.getKey();
      PhoneEntry entry = device.getValue();
      if (entry.exchange()) {
        book.put(pos, entry.numbered(digits.getOrDefault(pos, PhoneNumbering.NONE), pos));
      } else {
        PhoneNumbering.Assignment assignment =
            assigned.getOrDefault(pos, PhoneNumbering.Assignment.UNNUMBERED);
        book.put(pos, entry.numbered(assignment.number(), assignment.exchange()));
      }
    }
  }

  public void add(long pos, List<Long> cells, boolean exchange) {
    if (book.at(pos).isEmpty()) {
      book.put(pos, PhoneEntry.added(exchange, cells, book.nextOrder()));
      refresh();
    }
  }

  public void placedBy(long pos, String name, WireViewer placer) {
    book.at(pos).ifPresent(entry -> book.put(pos, entry.placedBy(name, placer)));
    refresh();
  }

  public void remove(long pos) {
    book.remove(pos);
    refresh();
  }

  public void renumber(long phone, int number, long exchange) {
    book.at(phone).ifPresent(entry -> book.put(phone, entry.numbered(number, exchange)));
  }

  public Optional<PhoneEntry> entry(long pos) {
    return book.at(pos);
  }

  public Optional<WireNetwork> systemOf(long pos) {
    return book.at(pos).flatMap(entry -> entry.home().map(hue -> touching(entry).get(hue)));
  }

  public List<Long> phonesNumbered(long caller, int number) {
    return membersOf(caller).stream()
        .filter(pos -> book.at(pos).filter(e -> !e.exchange() && e.number() == number).isPresent())
        .toList();
  }

  public List<Listing> listings(long caller) {
    return membersOf(caller).stream()
        .map(pos -> book.at(pos).orElseThrow())
        .filter(entry -> !entry.exchange() && entry.hasNumber())
        .map(entry -> new Listing(entry.number(), entry.name()))
        .distinct()
        .sorted(Comparator.comparingInt(Listing::number).thenComparing(Listing::name))
        .toList();
  }

  public List<Long> membersOf(long pos) {
    Optional<WireNetwork> system = systemOf(pos);
    if (system.isEmpty()) {
      return List.of();
    }
    return book.entries().keySet().stream()
        .filter(other -> systemOf(other).filter(found -> found == system.get()).isPresent())
        .toList();
  }

  public DeviceStatus status(long pos) {
    Optional<PhoneEntry> entry = book.at(pos);
    if (entry.isEmpty()) {
      return DeviceStatus.NO_EXCHANGE;
    }
    if (entry.get().home().isEmpty()) {
      return entry.get().exchange() ? DeviceStatus.READY : DeviceStatus.NO_EXCHANGE;
    }
    return entry.get().hasNumber() ? DeviceStatus.READY : DeviceStatus.NETWORK_FULL;
  }

  private PhoneNumbering.Result numberSystem(WireNetwork network, List<Long> members) {
    List<PhoneNumbering.Exchange> exchanges = new ArrayList<>();
    List<PhoneNumbering.Phone> phones = new ArrayList<>();
    for (long pos : members) {
      PhoneEntry entry = book.at(pos).orElseThrow();
      boolean allowed = entry.hasNumber() || entry.placer().canSee(network);
      if (entry.exchange()) {
        exchanges.add(new PhoneNumbering.Exchange(pos, entry.order(), entry.number(), allowed));
      } else {
        phones.add(
            new PhoneNumbering.Phone(
                pos, entry.order(), entry.number(), entry.numberedBy(), allowed));
      }
    }
    return PhoneNumbering.assign(exchanges, phones);
  }

  private Map<Hue, WireNetwork> touching(PhoneEntry entry) {
    Map<Hue, WireNetwork> touching = new EnumMap<>(Hue.class);
    for (long cell : entry.cells()) {
      for (WireNetwork network : grid.networksTouching(cell)) {
        touching.putIfAbsent(network.color(), network);
      }
    }
    return touching;
  }

  private boolean holdsPartner(
      WireNetwork network, long self, PhoneEntry entry, Map<Long, Long> anchors) {
    return network
        .touchedPositions()
        .longStream()
        .mapToObj(anchors::get)
        .filter(anchor -> anchor != null && anchor.longValue() != self)
        .anyMatch(anchor -> entry.exchange() || book.at(anchor).orElseThrow().exchange());
  }

  private Map<Long, Long> anchorsByCell() {
    Map<Long, Long> anchors = new HashMap<>();
    book.entries().forEach((pos, entry) -> entry.cells().forEach(cell -> anchors.put(cell, pos)));
    return anchors;
  }
}
