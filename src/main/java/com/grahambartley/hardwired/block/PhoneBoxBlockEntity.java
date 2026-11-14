package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.device.DialDevice;
import com.grahambartley.hardwired.device.PhoneDevice;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public final class PhoneBoxBlockEntity extends ElectronicBlockEntity implements DialDevice {

  private static final String SWUNG_AT = "swung_at";
  private static final String DOOR = "door";

  private long swungAt = Long.MIN_VALUE / 2;

  public PhoneBoxBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.PHONE_BOX, pos, state);
  }

  public void swing(long now) {
    swungAt = now;
    sync();
  }

  @Override
  public float dial(String value, float tickDelta) {
    if (!DOOR.equals(value) || world == null) {
      return 0F;
    }
    boolean open = getCachedState().get(PhoneBoxBlock.OPEN);
    return DoorSwing.openness(open, world.getTime() - swungAt + tickDelta);
  }

  @Override
  public boolean isMoving() {
    return getCachedState().get(PhoneDevice.RINGING);
  }

  @Override
  public float clipSpeed() {
    return 1F;
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    nbt.putLong(SWUNG_AT, swungAt);
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    swungAt = nbt.contains(SWUNG_AT) ? nbt.getLong(SWUNG_AT) : swungAt;
  }

  @Override
  public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
    NbtCompound nbt = super.toInitialChunkDataNbt(registries);
    nbt.putLong(SWUNG_AT, swungAt);
    return nbt;
  }
}
