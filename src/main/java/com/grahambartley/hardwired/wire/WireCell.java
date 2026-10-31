package com.grahambartley.hardwired.wire;

import java.util.UUID;

public record WireCell(WireFace face, Hue color, UUID owner) {

  public WireCell(WireFace face, Hue color) {
    this(face, color, null);
  }

  public boolean isSealed() {
    return owner != null;
  }

  public WireCell onFace(WireFace other) {
    return new WireCell(other, color, owner);
  }

  public WireCell ownedBy(UUID newOwner) {
    return new WireCell(face, color, newOwner);
  }
}
