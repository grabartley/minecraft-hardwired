package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AnimatedDeviceTest {

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @ParameterizedTest
  @CsvSource({"lit,false,false", "lit,true,true", "age,true,false"})
  void aPartConditionNeedsThatPropertySetTrue(String property, boolean lit, boolean flagged) {
    BlockState state = Blocks.REDSTONE_LAMP.getDefaultState().with(RedstoneLampBlock.LIT, lit);
    assertEquals(flagged, AnimatedDevice.flagged(state, property));
  }

  @ParameterizedTest
  @CsvSource({"lit,true,true", "lit,false,false", "!lit,true,false", "!lit,false,true"})
  void aLeadingBangShowsAPartWhileThePropertyIsFalse(String rule, boolean lit, boolean shown) {
    BlockState state = Blocks.REDSTONE_LAMP.getDefaultState().with(RedstoneLampBlock.LIT, lit);
    assertEquals(shown, AnimatedDevice.shownWhen(state, rule));
  }

  @ParameterizedTest
  @CsvSource({
    "facing=north,NORTH,true",
    "facing=north,EAST,false",
    "facing=east,EAST,true",
    "half=top,NORTH,false",
    "color=red,NORTH,false"
  })
  void anEqualsRuleShowsAPartWhileThePropertyHasThatValue(
      String rule, Direction facing, boolean shown) {
    BlockState state = Blocks.OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, facing);
    assertEquals(shown, AnimatedDevice.shownWhen(state, rule));
  }
}
