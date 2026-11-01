package com.grahambartley.hardwired.radio;

import com.grahambartley.hardwired.wire.WiredWorld;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class StationRegistry {

  private final Map<BlockPos, Station> stations = new LinkedHashMap<>();

  public static StationRegistry of(ServerWorld world) {
    return ((WiredWorld) world).hardwired$stations();
  }

  public Collection<Station> stations() {
    return Collections.unmodifiableCollection(stations.values());
  }

  public void publish(Station station) {
    stations.put(station.desk().toImmutable(), station);
  }

  public void withdraw(BlockPos desk) {
    stations.remove(desk);
  }

  public boolean towerUsedByAnother(BlockPos towerFoot, BlockPos desk) {
    return stations.values().stream()
        .anyMatch(station -> station.towerFoot().equals(towerFoot) && !station.desk().equals(desk));
  }
}
