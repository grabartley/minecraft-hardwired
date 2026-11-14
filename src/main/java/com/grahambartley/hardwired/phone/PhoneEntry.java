package com.grahambartley.hardwired.phone;

import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireViewer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.util.Util;

public record PhoneEntry(
    boolean exchange,
    List<Long> cells,
    long order,
    int number,
    long numberedBy,
    Optional<Hue> home,
    String name,
    WireViewer placer) {

  private static final String EXCHANGE = "exchange";
  private static final String CELLS = "cells";
  private static final String ORDER = "order";
  private static final String NUMBER = "number";
  private static final String NUMBERED_BY = "numbered_by";
  private static final String HOME = "home";
  private static final String NAME = "name";
  private static final String PLACER = "placer";
  private static final String OVERRIDE = "override";

  public static PhoneEntry added(boolean exchange, List<Long> cells, long order) {
    return new PhoneEntry(
        exchange,
        List.copyOf(cells),
        order,
        PhoneNumbering.NONE,
        0L,
        Optional.empty(),
        "",
        new WireViewer(Util.NIL_UUID, false));
  }

  public PhoneEntry placedBy(String by, WireViewer viewer) {
    return new PhoneEntry(exchange, cells, order, number, numberedBy, home, by, viewer);
  }

  public boolean hasNumber() {
    return number != PhoneNumbering.NONE;
  }

  public PhoneEntry numbered(int value, long by) {
    return new PhoneEntry(exchange, cells, order, value, by, home, name, placer);
  }

  public PhoneEntry homedOn(Optional<Hue> value) {
    return new PhoneEntry(exchange, cells, order, number, numberedBy, value, name, placer);
  }

  NbtCompound write() {
    NbtCompound nbt = new NbtCompound();
    nbt.putBoolean(EXCHANGE, exchange);
    nbt.put(CELLS, new NbtLongArray(cells));
    nbt.putLong(ORDER, order);
    nbt.putInt(NUMBER, number);
    nbt.putLong(NUMBERED_BY, numberedBy);
    home.ifPresent(hue -> nbt.putInt(HOME, hue.ordinal()));
    nbt.putString(NAME, name);
    nbt.put(PLACER, NbtHelper.fromUuid(placer.id()));
    nbt.putBoolean(OVERRIDE, placer.override());
    return nbt;
  }

  static PhoneEntry read(NbtCompound nbt) {
    Optional<Hue> home =
        nbt.contains(HOME) ? Optional.of(Hue.byIndex(nbt.getInt(HOME))) : Optional.empty();
    return new PhoneEntry(
        nbt.getBoolean(EXCHANGE),
        Arrays.stream(nbt.getLongArray(CELLS)).boxed().toList(),
        nbt.getLong(ORDER),
        nbt.getInt(NUMBER),
        nbt.getLong(NUMBERED_BY),
        home,
        nbt.getString(NAME),
        new WireViewer(NbtHelper.toUuid(nbt.get(PLACER)), nbt.getBoolean(OVERRIDE)));
  }
}
