package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WireOverridesTest {

  private static WireOverrides empty() {
    return WireOverrides.read(new NbtCompound());
  }

  @ParameterizedTest(name = "{0} toggles")
  @ValueSource(ints = {1, 2, 3, 4})
  void eachToggleFlipsTheState(int toggles) {
    WireOverrides overrides = empty();
    UUID operator = new UUID(1, 2);
    boolean expected = false;
    for (int i = 0; i < toggles; i++) {
      expected = !expected;
      assertEquals(expected, overrides.toggle(operator));
    }
    assertEquals(expected, overrides.isOn(operator));
    assertTrue(overrides.isDirty());
  }

  @Test
  void stateSurvivesARoundTrip() {
    WireOverrides overrides = empty();
    UUID on = new UUID(1, 2);
    UUID off = new UUID(3, 4);
    overrides.toggle(on);
    overrides.toggle(off);
    overrides.toggle(off);

    WireOverrides read = WireOverrides.read(overrides.writeNbt(new NbtCompound(), null));

    assertTrue(read.isOn(on));
    assertFalse(read.isOn(off));
  }
}
