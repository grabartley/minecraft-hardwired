package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.grahambartley.hardwired.device.WireDevice;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class WireRulesTest {

  private static final BlockPos POS = new BlockPos(1, 2, 3);
  private static final float STONE_HARDNESS = 1.5F;

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  private static BlockState plainState(boolean tagged) {
    BlockState state = mock(BlockState.class);
    when(state.getBlock()).thenReturn(mock(Block.class));
    when(state.isIn(WireRules.OUTPUTS)).thenReturn(tagged);
    return state;
  }

  private static BlockState deviceState(Direction connectedSide, boolean tagged) {
    Block block = mock(Block.class, withSettings().extraInterfaces(WireDevice.class));
    BlockState state = mock(BlockState.class);
    when(state.getBlock()).thenReturn(block);
    when(state.isIn(WireRules.OUTPUTS)).thenReturn(tagged);
    when(((WireDevice) block).connectsFrom(state, connectedSide)).thenReturn(true);
    return state;
  }

  private static BlockView worldWith(BlockState state) {
    BlockView world = mock(BlockView.class);
    when(world.getBlockState(POS)).thenReturn(state);
    return world;
  }

  @ParameterizedTest(name = "tagged {0}")
  @ValueSource(booleans = {true, false})
  void aPlainBlockIsAnOutputOnlyWhenTagged(boolean tagged) {
    assertEquals(tagged, WireRules.isOutput(plainState(tagged), new TouchPoint(POS, Direction.UP)));
  }

  @ParameterizedTest(name = "wire below {0}")
  @EnumSource(Direction.class)
  void aDeviceIsAskedAboutTheSideFacingTheWire(Direction awayFromWire) {
    BlockState state = deviceState(awayFromWire.getOpposite(), false);
    assertEquals(true, WireRules.isOutput(state, new TouchPoint(POS, awayFromWire)));
    assertEquals(false, WireRules.isOutput(state, new TouchPoint(POS, awayFromWire.getOpposite())));
  }

  @ParameterizedTest(name = "wire below {0}")
  @EnumSource(Direction.class)
  void aDeviceThatRefusesASideIgnoresTheTag(Direction awayFromWire) {
    BlockState state = deviceState(awayFromWire, true);
    assertEquals(false, WireRules.isOutput(state, new TouchPoint(POS, awayFromWire)));
  }

  @ParameterizedTest(name = "moving {0} air {1} refused {2} hardness {3} tag {4} solid {5}")
  @CsvSource({
    "false,false,false,1.5,false,true,true",
    "false,false,false,1.5,true,false,true",
    "false,false,false,0,false,true,true",
    "false,false,false,1.5,false,false,false",
    "false,true,false,0,true,true,false",
    "false,false,true,1.5,true,true,false",
    "false,false,false,-1,true,true,false",
    "true,true,true,-1,false,false,true"
  })
  void surfacesFollowTheTagsAndTheShape(
      boolean moving,
      boolean air,
      boolean refused,
      float hardness,
      boolean accepted,
      boolean solid,
      boolean expected) {
    BlockState state = mock(BlockState.class);
    BlockView world = worldWith(state);
    when(state.isOf(Blocks.MOVING_PISTON)).thenReturn(moving);
    when(state.isAir()).thenReturn(air);
    when(state.isIn(WireRules.REFUSED)).thenReturn(refused);
    when(state.isIn(WireRules.ACCEPTED)).thenReturn(accepted);
    when(state.getHardness(any(), any())).thenReturn(hardness);
    when(state.isSideSolidFullSquare(world, POS, Direction.UP)).thenReturn(solid);
    assertEquals(expected, WireRules.accepts(world, POS, Direction.UP));
  }

  @ParameterizedTest(name = "side {0}")
  @EnumSource(Direction.class)
  void onlyTheTouchedSideHasToBeSolid(Direction side) {
    BlockState state = mock(BlockState.class);
    BlockView world = worldWith(state);
    when(state.getHardness(any(), any())).thenReturn(STONE_HARDNESS);
    when(state.isSideSolidFullSquare(world, POS, Direction.UP)).thenReturn(true);
    assertEquals(side == Direction.UP, WireRules.accepts(world, POS, side));
  }
}
