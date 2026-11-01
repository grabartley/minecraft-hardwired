package com.grahambartley.hardwired.lift;

import com.grahambartley.hardwired.block.LiftFloorBlock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class LiftRiders {

  private LiftRiders() {}

  public static List<ServerPlayerEntity> aboard(LiftCarEntity car) {
    if (!(car.getWorld() instanceof ServerWorld world)) {
      return List.of();
    }
    return world.getPlayers(
        player -> !player.isSpectator() && player.isAlive() && car.holds(player));
  }

  public static void carry(List<ServerPlayerEntity> riders, double rise) {
    for (ServerPlayerEntity rider : riders) {
      lift(rider, rise);
    }
  }

  public static void lift(PlayerEntity player, double rise) {
    player.setPosition(player.getX(), player.getY() + rise, player.getZ());
    player.fallDistance = 0;
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

  public static void moveIfNoGateHere(
      ServerWorld world, ServerPlayerEntity rider, List<BlockPos> gates, int level) {
    Optional<BlockPos> gate = nearestGate(gates, level);
    if (gate.filter(spot -> spot.getY() == level).isEmpty()) {
      gate.ifPresentOrElse(spot -> stepOut(world, rider, spot), () -> letDown(rider, 0));
    }
  }

  public static Vec3d spotAt(BlockPos gate) {
    return Vec3d.ofBottomCenter(gate);
  }

  public static void letDown(ServerPlayerEntity rider, double drop) {
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
