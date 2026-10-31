package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class IntercomLineTest {

  @ParameterizedTest(name = "case {index}")
  @CsvSource({
    "'  hello  ',hello",
    "'§cred',cred",
    "'a\u0007b',ab",
    "'line\nbreak',linebreak",
    "'   ',''"
  })
  void aLineIsCleanedByVanillaChatRules(String raw, String clean) {
    assertEquals(clean, IntercomLine.clean(raw));
  }

  @ParameterizedTest(name = "x {0}")
  @ValueSource(doubles = {0.5, 8.5, 16.5, 33.5})
  void aListenerNeedsOnlyOneIntercomInRange(double x) {
    List<BlockPos> intercoms = List.of(new BlockPos(0, 0, 0), new BlockPos(100, 0, 0));
    boolean near = x <= 16.5;
    assertEquals(near, IntercomLine.reaches(new Vec3d(x, 0.5, 0.5), intercoms, 16));
  }

  @ParameterizedTest(name = "range {0}")
  @CsvSource({"1,false", "99,true"})
  void aSecondIntercomExtendsTheReach(int range, boolean heard) {
    List<BlockPos> intercoms = List.of(new BlockPos(0, 0, 0), new BlockPos(50, 0, 0));
    assertEquals(heard, IntercomLine.reaches(new Vec3d(40.5, 0.5, 0.5), intercoms, range));
  }
}
