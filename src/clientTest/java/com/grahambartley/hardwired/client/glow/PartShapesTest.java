package com.grahambartley.hardwired.client.glow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.block.BlockRenderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PartShapesTest {

  @ParameterizedTest(name = "{0} with block entity {1}: {2}")
  @CsvSource({
    "MODEL,false,true",
    "MODEL,true,false",
    "ENTITYBLOCK_ANIMATED,true,false",
    "ENTITYBLOCK_ANIMATED,false,false",
    "INVISIBLE,false,false"
  })
  void onlyPlainModelsTakeTint(BlockRenderType type, boolean entity, boolean expected) {
    assertEquals(expected, PartShapes.isPlainModel(type, entity));
  }
}
