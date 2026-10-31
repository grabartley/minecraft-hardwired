package com.grahambartley.hardwired.device;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

public final class SmartHomeLog extends PersistentState {

  public static final int GOAL = 8;
  private static final String KEY = "hardwired_smart_home";
  private static final Type<SmartHomeLog> TYPE =
      new Type<>(SmartHomeLog::new, (nbt, registries) -> read(nbt), null);

  private final Map<UUID, Set<String>> worked = new HashMap<>();

  public static SmartHomeLog of(MinecraftServer server) {
    return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, KEY);
  }

  public int record(UUID player, String device) {
    Set<String> devices = worked.computeIfAbsent(player, id -> new HashSet<>());
    if (devices.size() < GOAL && devices.add(device)) {
      markDirty();
    }
    return devices.size();
  }

  static SmartHomeLog read(NbtCompound nbt) {
    SmartHomeLog log = new SmartHomeLog();
    for (String player : nbt.getKeys()) {
      Set<String> devices = new HashSet<>();
      NbtList saved = nbt.getList(player, NbtElement.STRING_TYPE);
      for (int i = 0; i < Math.min(saved.size(), GOAL); i++) {
        devices.add(saved.getString(i));
      }
      log.worked.put(UUID.fromString(player), devices);
    }
    return log;
  }

  @Override
  public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    worked.forEach(
        (player, devices) -> {
          NbtList list = new NbtList();
          devices.forEach(device -> list.add(NbtString.of(device)));
          nbt.put(player.toString(), list);
        });
    return nbt;
  }
}
