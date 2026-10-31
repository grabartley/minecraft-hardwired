package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.device.DeviceActions;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireNetwork;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class JunctionBlock extends Block implements WireDevice {

  public static final EnumProperty<JunctionRule> RULE = EnumProperty.of("rule", JunctionRule.class);
  public static final EnumProperty<Hue> OUTPUT = EnumProperty.of("output", Hue.class);

  public JunctionBlock(Settings settings) {
    super(settings);
    setDefaultState(
        getStateManager().getDefaultState().with(RULE, JunctionRule.OR).with(OUTPUT, Hue.PLAIN));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(RULE, OUTPUT);
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    return DeviceActions.open(player, pos);
  }

  @Override
  public boolean isDriving(ServerWorld world, BlockPos pos, BlockState state, Hue color) {
    if (color != state.get(OUTPUT)) {
      return false;
    }
    int inputs = 0;
    int inputsOn = 0;
    for (WireNetwork network : WireEngine.of(world).grid().networksTouching(pos)) {
      if (network.color() != color) {
        inputs++;
        inputsOn += network.isOn() ? 1 : 0;
      }
    }
    return state.get(RULE).apply(inputsOn, inputs);
  }

  @Override
  public void onWireChange(ServerWorld world, BlockPos pos, BlockState state) {
    WireEngine.of(world).refresh(pos);
  }
}
