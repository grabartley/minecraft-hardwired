package com.grahambartley.hardwired.registry;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.lift.LiftCarEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {

  private static final float CAR_WIDTH = 1F;
  private static final float LARGE_CAR_WIDTH = 2F;
  private static final float EYE_ABOVE_PLATE = 1F;
  private static final int TRACKING_CHUNKS = 10;

  public static final EntityType<LiftCarEntity> LIFT_CAR =
      Registry.register(
          Registries.ENTITY_TYPE,
          Identifier.of(Hardwired.MOD_ID, "lift_car"),
          EntityType.Builder.<LiftCarEntity>create(LiftCarEntity::new, SpawnGroup.MISC)
              .dimensions(CAR_WIDTH, LiftCarEntity.PLATE)
              .eyeHeight(EYE_ABOVE_PLATE)
              .maxTrackingRange(TRACKING_CHUNKS)
              .makeFireImmune()
              .disableSummon()
              .build("lift_car"));

  public static final EntityType<LiftCarEntity> LARGE_LIFT_CAR =
      Registry.register(
          Registries.ENTITY_TYPE,
          Identifier.of(Hardwired.MOD_ID, "large_lift_car"),
          EntityType.Builder.<LiftCarEntity>create(LiftCarEntity::new, SpawnGroup.MISC)
              .dimensions(LARGE_CAR_WIDTH, LiftCarEntity.PLATE)
              .eyeHeight(EYE_ABOVE_PLATE)
              .maxTrackingRange(TRACKING_CHUNKS)
              .makeFireImmune()
              .disableSummon()
              .build("large_lift_car"));

  private ModEntities() {}

  public static void register() {}
}
