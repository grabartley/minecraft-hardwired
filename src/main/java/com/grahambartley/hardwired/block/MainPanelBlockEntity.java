package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.device.PanelMemory;
import com.grahambartley.hardwired.device.PanelRow;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public final class MainPanelBlockEntity extends BlockEntity {

  private static final String MEMORY = "memory";

  private PanelMemory memory = new PanelMemory(List.of());

  public MainPanelBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.MAIN_PANEL, pos, state);
  }

  public PanelMemory memory() {
    return memory;
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    nbt.put(MEMORY, PanelRow.LIST_CODEC.encodeStart(NbtOps.INSTANCE, memory.rows()).getOrThrow());
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    memory =
        new PanelMemory(
            nbt.contains(MEMORY)
                ? PanelRow.LIST_CODEC
                    .parse(NbtOps.INSTANCE, nbt.get(MEMORY))
                    .resultOrPartial(Hardwired.LOGGER::error)
                    .orElse(List.of())
                : List.of());
  }
}
