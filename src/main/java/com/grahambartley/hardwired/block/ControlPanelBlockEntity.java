package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import com.grahambartley.hardwired.registry.ModComponents;
import com.grahambartley.hardwired.wire.LampLook;
import com.grahambartley.hardwired.wire.Scene;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public final class ControlPanelBlockEntity extends BlockEntity {

  private static final String SCENES = "scenes";

  private List<Scene> scenes = new ArrayList<>();

  public ControlPanelBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.CONTROL_PANEL, pos, state);
  }

  public List<Scene> scenes() {
    return scenes;
  }

  public void saveScene(int slot, String name, LampLook look) {
    Scene.store(scenes, slot, new Scene(name, look));
    sync();
  }

  private void sync() {
    markDirty();
    if (world != null) {
      world.updateListeners(pos, getCachedState(), getCachedState(), 0);
    }
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    nbt.put(SCENES, Scene.LIST_CODEC.encodeStart(NbtOps.INSTANCE, scenes).getOrThrow());
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    NbtElement saved = nbt.get(SCENES);
    scenes =
        new ArrayList<>(
            Scene.LIST_CODEC
                .parse(NbtOps.INSTANCE, saved)
                .resultOrPartial(Hardwired.LOGGER::error)
                .orElse(List.of()));
  }

  @Override
  protected void readComponents(ComponentsAccess components) {
    super.readComponents(components);
    scenes = new ArrayList<>(components.getOrDefault(ModComponents.SCENES, List.of()));
  }

  @Override
  protected void addComponents(ComponentMap.Builder builder) {
    super.addComponents(builder);
    builder.add(ModComponents.SCENES, List.copyOf(scenes));
  }

  @Override
  public void removeFromCopiedStackNbt(NbtCompound nbt) {
    nbt.remove(SCENES);
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
