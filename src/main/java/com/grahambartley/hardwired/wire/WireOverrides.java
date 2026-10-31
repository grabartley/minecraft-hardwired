package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.server.ServerConfigService;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;

public final class WireOverrides extends PersistentState {

  public static final String KEY = "hardwired_overrides";
  private static final String OPERATORS = "operators";
  private static final Type<WireOverrides> TYPE =
      new Type<>(WireOverrides::new, (nbt, registries) -> read(nbt), null);

  private final Set<UUID> operators = new HashSet<>();

  public static WireOverrides of(MinecraftServer server) {
    return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, KEY);
  }

  public static boolean isActive(ServerPlayerEntity player) {
    return player.hasPermissionLevel(ServerConfigService.OP_PERMISSION_LEVEL)
        && of(player.getServer()).isOn(player.getUuid());
  }

  public boolean isOn(UUID operator) {
    return operators.contains(operator);
  }

  public boolean toggle(UUID operator) {
    boolean on = operators.add(operator);
    if (!on) {
      operators.remove(operator);
    }
    markDirty();
    return on;
  }

  static WireOverrides read(NbtCompound nbt) {
    WireOverrides overrides = new WireOverrides();
    NbtList list = nbt.getList(OPERATORS, NbtElement.INT_ARRAY_TYPE);
    for (NbtElement element : list) {
      overrides.operators.add(NbtHelper.toUuid(element));
    }
    return overrides;
  }

  @Override
  public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    NbtList list = new NbtList();
    for (UUID operator : operators) {
      list.add(NbtHelper.fromUuid(operator));
    }
    nbt.put(OPERATORS, list);
    return nbt;
  }
}
