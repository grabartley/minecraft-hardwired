package com.grahambartley.hardwired.device;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.math.BlockPos;

public final class PanelMemory {

  private final Map<BlockPos, PanelRow> rows =
      new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<BlockPos, PanelRow> eldest) {
          return size() > PanelRow.MAX_ROWS;
        }
      };

  public PanelMemory(List<PanelRow> remembered) {
    remembered.forEach(this::remember);
  }

  public boolean remember(PanelRow row) {
    PanelRow fresh =
        new PanelRow(
            row.pos(), row.kind(), row.on(), row.color(), row.level(), row.maxLevel(), false);
    PanelRow previous = rows.remove(row.pos());
    rows.put(row.pos(), fresh);
    return !fresh.equals(previous);
  }

  public boolean forget(BlockPos pos) {
    return rows.remove(pos) != null;
  }

  public Optional<PanelRow> recall(BlockPos pos) {
    return Optional.ofNullable(rows.get(pos));
  }

  public List<PanelRow> rows() {
    return List.copyOf(rows.values());
  }
}
