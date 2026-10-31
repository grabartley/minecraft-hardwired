package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.wire.TouchPoint;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireNetwork;
import com.grahambartley.hardwired.wire.WireViewer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class PanelRoster {

  private PanelRoster() {}

  public record Listing(
      List<PanelRow> rows, int unknownPoints, int omitted, boolean memoryChanged) {}

  public static boolean reaches(
      WireEngine engine, BlockPos panel, BlockPos target, WireViewer viewer) {
    return networks(engine, panel, viewer).anyMatch(network -> network.isTouching(target));
  }

  public static Listing list(
      ServerWorld world, BlockPos panel, WireViewer viewer, PanelMemory memory) {
    WireEngine engine = WireEngine.of(world);
    Set<BlockPos> touched =
        networks(engine, panel, viewer)
            .flatMap(network -> network.touches().stream())
            .map(TouchPoint::pos)
            .collect(Collectors.toSet());
    Set<BlockPos> anchors = new HashSet<>();
    List<PanelRow> rows = new ArrayList<>();
    int unknown = 0;
    boolean memoryChanged = false;
    for (BlockPos pos : touched) {
      if (!engine.isLoaded(pos)) {
        PanelRow known = memory.recall(pos).map(PanelRow::asStale).orElse(null);
        if (known == null) {
          unknown++;
        } else {
          rows.add(known);
        }
        continue;
      }
      BlockState state = world.getBlockState(pos);
      if (state.getBlock() instanceof PanelDevice device) {
        if (anchors.add(device.anchor(pos, state))) {
          PanelRow row = device.read(world, pos, state);
          memoryChanged |= memory.remember(row);
          rows.add(row);
        }
      } else {
        memoryChanged |= memory.forget(pos);
      }
    }
    List<PanelRow> ordered = PanelRows.ordered(rows, panel);
    return new Listing(
        PanelRows.capped(ordered), unknown, PanelRows.omitted(ordered), memoryChanged);
  }

  private static Stream<WireNetwork> networks(
      WireEngine engine, BlockPos panel, WireViewer viewer) {
    return engine.grid().networksTouching(panel).stream().filter(viewer::canSee);
  }
}
