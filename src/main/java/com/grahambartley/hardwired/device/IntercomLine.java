package com.grahambartley.hardwired.device;

import java.util.List;
import net.minecraft.util.StringHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class IntercomLine {

  public static final int MAX_LENGTH = 256;

  private IntercomLine() {}

  public static String clean(String raw) {
    return StringHelper.stripInvalidChars(raw).strip();
  }

  public static boolean reaches(Vec3d listener, List<BlockPos> intercoms, int range) {
    double reach = (double) range * range;
    return intercoms.stream()
        .anyMatch(intercom -> Vec3d.ofCenter(intercom).squaredDistanceTo(listener) <= reach);
  }
}
