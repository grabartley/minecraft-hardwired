package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public class ElectronicBlockEntity extends BlockEntity {

  private static final String STATUS = "status";

  private DeviceStatus status = DeviceStatus.READY;

  public ElectronicBlockEntity(BlockPos pos, BlockState state) {
    this(ModBlockEntities.ELECTRONIC, pos, state);
  }

  protected ElectronicBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  public DeviceStatus status() {
    return status;
  }

  public void setStatus(DeviceStatus next) {
    if (status == next) {
      return;
    }
    status = next;
    sync();
  }

  protected void sync() {
    markDirty();
    if (world != null && !world.isClient) {
      world.updateListeners(pos, getCachedState(), getCachedState(), 0);
    }
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    nbt.putString(STATUS, status.id());
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    status = DeviceStatus.byId(nbt.getString(STATUS), DeviceStatus.READY);
  }

  @Override
  public void removeFromCopiedStackNbt(NbtCompound nbt) {
    nbt.remove(STATUS);
  }

  @Override
  public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
    NbtCompound nbt = new NbtCompound();
    nbt.putString(STATUS, status.id());
    return nbt;
  }

  @Override
  public Packet<ClientPlayPacketListener> toUpdatePacket() {
    return BlockEntityUpdateS2CPacket.create(this);
  }
}
