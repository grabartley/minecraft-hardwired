package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SceneTest {

  private static final Scene DINNER = new Scene("Dinner", new LampLook(Hue.RED, 6));

  @ParameterizedTest
  @ValueSource(ints = {0, 2, Scene.MAX_SCENES - 1})
  void aSceneStaysInTheSlotItWasSavedTo(int slot) {
    List<Scene> scenes = new ArrayList<>();
    Scene.store(scenes, slot, DINNER);
    assertEquals(Optional.of(DINNER), Scene.at(scenes, slot));
    assertEquals(slot + 1, scenes.size());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 3})
  void anEmptyOrMissingSlotHasNothingToRecall(int slot) {
    List<Scene> scenes = new ArrayList<>();
    Scene.store(scenes, 2, DINNER);
    assertEquals(Optional.empty(), Scene.at(scenes, slot));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  void savingOverASlotReplacesIt(int slot) {
    List<Scene> scenes = new ArrayList<>();
    Scene.store(scenes, slot, Scene.EMPTY);
    Scene.store(scenes, slot, DINNER);
    assertEquals(Optional.of(DINNER), Scene.at(scenes, slot));
  }
}
