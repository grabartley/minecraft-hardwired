package com.grahambartley.hardwired.phone;

import com.grahambartley.hardwired.wire.Hue;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    UUID placer,
    boolean trusted) {

  public static final int MAX_NAME = 50;

  private static final String EXCHANGE = "exchange";
  private static final String CELLS = "cells";
  private static final String ORDER = "order";
  private static final String NUMBER = "number";
  private static final String NUMBERED_BY = "numbered_by";
  private static final String HOME = "home";
  private static final String NAME = "name";
  private static final String PLACER = "placer";
  private static final String TRUSTED = "trusted";

  public static PhoneEntry added(boolean exchange, List<Long> cells, long order) {
    return new PhoneEntry(
        exchange,
        List.copyOf(cells),
        order,
        PhoneNumbering.NONE,
        0L,
        Optional.empty(),
        "",
        Util.NIL_UUID,
        false);
  }

  public static String fitName(String name) {
    if (name.length() <= MAX_NAME) {
      return name;
    }
    int end = Character.isHighSurrogate(name.charAt(MAX_NAME - 1)) ? MAX_NAME - 1 : MAX_NAME;
    return name.substring(0, end);
  }

  public PhoneEntry placedBy(String by, UUID player) {
    return new PhoneEntry(
        exchange, cells, order, number, numberedBy, home, fitName(by), player, trusted);
  }

  public boolean hasNumber() {
    return number != PhoneNumbering.NONE;
  }

  public PhoneEntry numbered(int value, long by) {
    return new PhoneEntry(
        exchange,
        cells,
        order,
        value,
        by,
        home,
        name,
        placer,
        trusted || value != PhoneNumbering.NONE);
  }

  public PhoneEntry homedOn(Optional<Hue> value) {
    return new PhoneEntry(exchange, cells, order, number, numberedBy, value, name, placer, trusted);
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
    nbt.put(PLACER, NbtHelper.fromUuid(placer));
    nbt.putBoolean(TRUSTED, trusted);
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
        fitName(nbt.getString(NAME)),
        nbt.contains(PLACER) ? NbtHelper.toUuid(nbt.get(PLACER)) : Util.NIL_UUID,
        nbt.getBoolean(TRUSTED));
  }
}
