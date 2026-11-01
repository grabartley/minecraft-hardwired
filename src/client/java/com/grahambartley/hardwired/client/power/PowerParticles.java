package com.grahambartley.hardwired.client.power;

import com.grahambartley.hardwired.block.SourceBlock;
import com.grahambartley.hardwired.block.SourceBlockEntity;
import com.grahambartley.hardwired.power.SourceKind;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

final class PowerParticles {

  private static final float PIXEL = 1F / 16F;
  private static final double SMOKE_RATE = 0.1D;
  private static final double SPARK_RATE = 0.01D;
  private static final float[] STACK = {8F, 16F, 8F};
  private static final float[] ROTOR = {8F, 10F, 8F};

  private PowerParticles() {}

  static void tick(MinecraftClient client, List<SourceBlockEntity> sources) {
    Random random = client.world.getRandom();
    for (SourceBlockEntity source : sources) {
      double busy = 0.3D + source.workload() / 15D;
      if (source.kind() == SourceKind.GENERATOR && random.nextDouble() < SMOKE_RATE * busy) {
        Vec3d stack = at(source, STACK);
        client.world.addParticle(ParticleTypes.SMOKE, stack.x, stack.y, stack.z, 0D, 0.03D, 0D);
      } else if (source.kind() == SourceKind.DYNAMO && random.nextDouble() < SPARK_RATE * busy) {
        Vec3d rotor = at(source, ROTOR);
        client.world.addParticle(
            ParticleTypes.ELECTRIC_SPARK, rotor.x, rotor.y, rotor.z, 0D, 0.02D, 0D);
      }
    }
  }

  private static Vec3d at(SourceBlockEntity source, float[] pixels) {
    Direction facing = source.getCachedState().get(SourceBlock.FACING);
    double side = (pixels[0] - 8F) * PIXEL;
    double out = (8F - pixels[2]) * PIXEL;
    Vec3d flat =
        Vec3d.of(facing.getVector())
            .multiply(out)
            .add(Vec3d.of(facing.rotateYClockwise().getVector()).multiply(side));
    return Vec3d.ofBottomCenter(source.getPos()).add(flat.x, pixels[1] * PIXEL, flat.z);
  }
}
