package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.power.SourceKind;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class LodestoneDynamoBlock extends SourceBlock {

  public LodestoneDynamoBlock(Settings settings) {
    super(settings);
  }

  @Override
  public SourceKind sourceKind() {
    return SourceKind.DYNAMO;
  }

  @Override
  public boolean isFuelled(ServerWorld world, BlockPos pos, BlockState state) {
    return true;
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new SourceBlockEntity(ModBlockEntities.LODESTONE_DYNAMO, pos, state);
  }
}
