package com.grahambartley.hardwired.lift;

import com.grahambartley.hardwired.block.LiftFloorBlock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class LiftRiders {

  private static final double REACH_ABOVE_FLOOR = 2.5;
  private static final double REACH_BELOW_FLOOR = 0.5;

  private LiftRiders() {}

  public static List<ServerPlayerEntity> inColumn(LiftShaft shaft, double level) {
    BlockPos head = shaft.head();
    return shaft
        .world()
        .getPlayers(
            player ->
                !player.isSpectator()
                    && player.isAlive()
                    && !player.hasVehicle()
                    && LiftSafety.inColumn(player.getX(), player.getZ(), head.getX(), head.getZ())
                    && player.getY() >= level - REACH_BELOW_FLOOR
                    && player.getY() <= level + REACH_ABOVE_FLOOR);
  }

  public static void board(LiftCarEntity car, LiftShaft shaft) {
    inColumn(shaft, car.run().level()).forEach(player -> player.startRiding(car, true));
  }

  public static List<ServerPlayerEntity> aboard(LiftCarEntity car) {
    List<ServerPlayerEntity> riders = new ArrayList<>();
    for (Entity passenger : car.getPassengerList()) {
      if (passenger instanceof ServerPlayerEntity rider) {
        riders.add(rider);
      }
    }
    return riders;
  }

  public static void tell(List<ServerPlayerEntity> players, Text message) {
    players.forEach(player -> player.sendMessage(message, true));
  }

  public static void stepOut(ServerWorld world, ServerPlayerEntity rider, BlockPos gate) {
    Vec3d spot = spotAt(gate);
    BlockState state = world.getBlockState(gate);
    Direction out =
        state.getBlock() instanceof LiftFloorBlock
            ? state.get(LiftFloorBlock.FACING).getOpposite()
            : Direction.fromRotation(rider.getYaw());
    rider.fallDistance = 0;
    rider.teleport(world, spot.x, spot.y, spot.z, out.asRotation(), 0);
  }

  public static Vec3d spotAt(BlockPos gate) {
    return Vec3d.ofBottomCenter(gate);
  }

  public static void letDown(ServerPlayerEntity rider, double drop) {
    rider.stopRiding();
    rider.fallDistance = 0;
    rider.addStatusEffect(
        new StatusEffectInstance(StatusEffects.SLOW_FALLING, LiftSafety.slowFallTicks(drop)));
  }

  public static Optional<BlockPos> nearestGate(List<BlockPos> gates, double level) {
    return gates.stream()
        .min(
            Comparator.<BlockPos>comparingDouble(gate -> Math.abs(gate.getY() - level))
                .thenComparingInt(BlockPos::getY));
  }
}
