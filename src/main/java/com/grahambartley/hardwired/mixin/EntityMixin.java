package com.grahambartley.hardwired.mixin;

import com.grahambartley.hardwired.lift.LiftCarEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
abstract class EntityMixin {

  @WrapOperation(
      method =
          "adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/World;getEntityCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Ljava/util/List;"))
  private List<VoxelShape> hardwired$liftCarWalls(
      World world, Entity entity, Box box, Operation<List<VoxelShape>> original) {
    return LiftCarEntity.withWalls(entity, box, original.call(world, entity, box));
  }
}
