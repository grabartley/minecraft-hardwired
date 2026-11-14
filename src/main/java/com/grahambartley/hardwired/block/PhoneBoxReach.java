package com.grahambartley.hardwired.block;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class PhoneBoxReach {

  private static final int STANDING_ROOM = 2;
  private static final double DOOR_HALF = 0.5;

  private PhoneBoxReach() {}

  public static boolean inside(Vec3d feet, BlockPos floor) {
    return MathHelper.floor(feet.x) == floor.getX()
        && MathHelper.floor(feet.z) == floor.getZ()
        && feet.y >= floor.getY()
        && feet.y < floor.getY() + STANDING_ROOM;
  }

  public static boolean opensDoor(boolean inside, double depthFromDoor) {
    return !inside || depthFromDoor < DOOR_HALF;
  }
}
