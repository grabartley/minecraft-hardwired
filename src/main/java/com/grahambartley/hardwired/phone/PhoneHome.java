package com.grahambartley.hardwired.phone;

import com.grahambartley.hardwired.wire.Hue;
import java.util.Optional;
import java.util.Set;

public final class PhoneHome {

  private PhoneHome() {}

  public static Optional<Hue> choose(Optional<Hue> current, Set<Hue> withPartner) {
    if (current.isPresent() && withPartner.contains(current.get())) {
      return current;
    }
    return withPartner.stream().min(Enum::compareTo);
  }
}
