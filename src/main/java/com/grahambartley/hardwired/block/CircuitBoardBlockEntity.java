package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.circuit.Design;
import com.grahambartley.hardwired.circuit.DesignText;
import com.grahambartley.hardwired.device.DesignCodecs;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import com.grahambartley.hardwired.registry.ModComponents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public final class CircuitBoardBlockEntity extends BlockEntity {

  private static final String DESIGN = "design";
  private static final String COPY = "copy";

  private Design design = Design.EMPTY;
  private boolean copy;

  public CircuitBoardBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.CIRCUIT_BOARD, pos, state);
  }

  public Design design() {
    return design;
  }

  public boolean isCopy() {
    return copy;
  }

  public void setDesign(Design next, boolean fromCircuitBlock) {
    design = next;
    copy = fromCircuitBlock;
    markDirty();
    if (world != null) {
      world.updateListeners(pos, getCachedState(), getCachedState(), 0);
    }
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    nbt.putString(DESIGN, DesignText.encode(design));
    nbt.putBoolean(COPY, copy);
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    design = DesignCodecs.orEmpty(nbt.getString(DESIGN));
    copy = nbt.getBoolean(COPY);
  }

  @Override
  protected void readComponents(ComponentsAccess components) {
    super.readComponents(components);
    design = components.getOrDefault(ModComponents.CIRCUIT_DESIGN, Design.EMPTY);
  }

  @Override
  protected void addComponents(ComponentMap.Builder builder) {
    super.addComponents(builder);
    if (!design.equals(Design.EMPTY)) {
      builder.add(ModComponents.CIRCUIT_DESIGN, design);
    }
  }

  @Override
  public void removeFromCopiedStackNbt(NbtCompound nbt) {
    nbt.remove(DESIGN);
    nbt.remove(COPY);
  }

  @Override
  public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
    return createNbt(registries);
  }

  @Override
  public Packet<ClientPlayPacketListener> toUpdatePacket() {
    return BlockEntityUpdateS2CPacket.create(this);
  }
}
