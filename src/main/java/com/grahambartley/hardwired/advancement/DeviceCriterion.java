package com.grahambartley.hardwired.advancement;

import com.grahambartley.hardwired.wire.Hue;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public final class DeviceCriterion extends AbstractCriterion<DeviceCriterion.Conditions> {

  private static final int WITNESS_RANGE = 16;

  public static String colorEvent(Hue hue) {
    return "color_" + hue.asString();
  }

  public record Conditions(Optional<LootContextPredicate> player, String event)
      implements AbstractCriterion.Conditions {
    public static final Codec<Conditions> CODEC =
        RecordCodecBuilder.create(
            instance ->
                instance
                    .group(
                        EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
                            .optionalFieldOf("player")
                            .forGetter(Conditions::player),
                        Codec.STRING.fieldOf("event").forGetter(Conditions::event))
                    .apply(instance, Conditions::new));
  }

  @Override
  public Codec<Conditions> getConditionsCodec() {
    return Conditions.CODEC;
  }

  public void trigger(ServerPlayerEntity player, String event) {
    trigger(player, conditions -> conditions.event().equals(event));
  }

  public void triggerNear(World world, BlockPos pos, String event) {
    Box witnesses = new Box(pos).expand(WITNESS_RANGE);
    for (ServerPlayerEntity player : ((ServerWorld) world).getPlayers()) {
      if (witnesses.contains(player.getPos())) {
        trigger(player, event);
      }
    }
  }
}
