package com.grahambartley.hardwired.phone;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

public final class PhoneBook extends PersistentState {

  public static final String KEY = "hardwired_phones";
  private static final String ENTRIES = "entries";
  private static final String POS = "pos";
  private static final String NEXT_ORDER = "next_order";
  private static final Type<PhoneBook> TYPE =
      new Type<>(PhoneBook::new, (nbt, registries) -> read(nbt), null);

  private final Map<Long, PhoneEntry> entries = new LinkedHashMap<>();
  private long nextOrder;
  private boolean stale;

  public static PhoneBook of(ServerWorld world) {
    return world.getPersistentStateManager().getOrCreate(TYPE, KEY);
  }

  public Map<Long, PhoneEntry> entries() {
    return Collections.unmodifiableMap(entries);
  }

  public Optional<PhoneEntry> at(long pos) {
    return Optional.ofNullable(entries.get(pos));
  }

  public long nextOrder() {
    markDirty();
    return nextOrder++;
  }

  public void markStale() {
    stale = true;
  }

  public boolean takeStale() {
    boolean was = stale;
    stale = false;
    return was;
  }

  public void put(long pos, PhoneEntry entry) {
    if (!entry.equals(entries.put(pos, entry))) {
      markDirty();
    }
  }

  public void remove(long pos) {
    if (entries.remove(pos) != null) {
      markDirty();
    }
  }

  static PhoneBook read(NbtCompound nbt) {
    PhoneBook book = new PhoneBook();
    book.nextOrder = nbt.getLong(NEXT_ORDER);
    for (NbtElement element : nbt.getList(ENTRIES, NbtElement.COMPOUND_TYPE)) {
      NbtCompound entry = (NbtCompound) element;
      book.entries.put(entry.getLong(POS), PhoneEntry.read(entry));
    }
    return book;
  }

  @Override
  public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    NbtList list = new NbtList();
    entries.forEach(
        (pos, entry) -> {
          NbtCompound written = entry.write();
          written.putLong(POS, pos);
          list.add(written);
        });
    nbt.put(ENTRIES, list);
    nbt.putLong(NEXT_ORDER, nextOrder);
    return nbt;
  }
}
