package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.wire.LampLook;

public final class LampLookRule {

  private LampLookRule() {}

  public static boolean ownLookStands(
      LampLook own, long ownStamp, LampLook network, long networkStamp) {
    return own != null && (network == null || ownStamp > networkStamp);
  }
}
