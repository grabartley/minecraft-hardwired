package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.lift.LiftDial;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public final class LiftFloorBlockEntity extends ElectronicBlockEntity {

  private LiftDial dial = LiftDial.NONE;

  public LiftFloorBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.LIFT_FLOOR, pos, state);
  }

  public LiftDial dial() {
    return dial;
  }

  public void setDial(LiftDial next) {
    if (!dial.equals(next)) {
      dial = next;
      sync();
    }
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    dial.write(nbt);
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    dial = LiftDial.read(nbt);
  }

  @Override
  public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
    NbtCompound nbt = super.toInitialChunkDataNbt(registries);
    dial.write(nbt);
    return nbt;
  }
}
