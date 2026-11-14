package com.grahambartley.hardwired.wire;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;

public record WireViewer(UUID id, boolean override) {

  private static final int OWN = 0;
  private static final int OPEN = 1;
  private static final int SOMEONE_ELSES = 2;

  public static WireViewer of(ServerPlayerEntity player) {
    return new WireViewer(player.getUuid(), WireOverrides.isActive(player));
  }

  public boolean canSee(WireCell cell) {
    return !cell.isSealed() || override || cell.owner().equals(id);
  }

  public boolean canSee(WireSpan span) {
    return span.owner() == null || override || span.owner().equals(id);
  }

  public boolean canSee(WireNetwork network) {
    return !network.isSealed() || override || network.owner().equals(id);
  }

  public List<WireCell> visibleOn(WireGrid grid, WireFace face) {
    return grid.cellsOn(face.pos()).stream()
        .filter(cell -> cell.face().equals(face) && canSee(cell))
        .sorted(
            Comparator.comparingInt(this::rank)
                .thenComparing(WireCell::color)
                .thenComparing(cell -> String.valueOf(cell.owner())))
        .toList();
  }

  public Optional<WireCell> find(WireGrid grid, WireFace face, Hue color) {
    return visibleOn(grid, face).stream().filter(cell -> cell.color() == color).findFirst();
  }

  public Optional<WireCell> choose(WireGrid grid, WireFace face, Hue preferred) {
    return find(grid, face, preferred).or(() -> visibleOn(grid, face).stream().findFirst());
  }

  public Optional<WireCell> waxTarget(WireGrid grid, WireFace face, WireWax.Use use) {
    Comparator<WireCell> order =
        Comparator.<WireCell, Boolean>comparing(cell -> cell.color() != use.wire())
            .thenComparing(WireCell::isSealed);
    return visibleOn(grid, face).stream()
        .filter(cell -> use.action() == WireWax.Action.SEAL || cell.isSealed())
        .min(order);
  }

  public UUID ownerOfNewWire(WireGrid grid, WireFace face, Hue color) {
    Set<UUID> owners = grid.sealedOwnersAround(face, color);
    if (owners.contains(id)) {
      return id;
    }
    return override ? owners.stream().min(Comparator.naturalOrder()).orElse(null) : null;
  }

  private int rank(WireCell cell) {
    if (!cell.isSealed()) {
      return OPEN;
    }
    return cell.owner().equals(id) ? OWN : SOMEONE_ELSES;
  }
}
