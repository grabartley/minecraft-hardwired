package com.grahambartley.hardwired.radio;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.RadioSettings;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

public final class RadioTuner {

  private RadioTuner() {}

  public static Optional<Reception> best(ServerWorld world, BlockPos pos, int frequency) {
    RadioSettings settings = Hardwired.SERVER_CONFIG.radio();
    BlockPos.Mutable probe = pos.mutableCopy();
    int depth =
        RadioDepth.below(
            world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()),
            pos.getY(),
            y -> world.getBlockState(probe.setY(y)).isSolidBlock(world, probe));
    List<Reception> heard =
        StationRegistry.of(world).stations().stream()
            .filter(station -> world.isChunkLoaded(station.towerFoot()))
            .map(
                station ->
                    new Reception(
                        station,
                        Signal.strength(
                            station.flatDistanceTo(pos),
                            station.reach(),
                            depth,
                            Frequency.stepsApart(frequency, station.frequency()),
                            settings)))
            .toList();
    return StationChoice.best(heard);
  }
}
