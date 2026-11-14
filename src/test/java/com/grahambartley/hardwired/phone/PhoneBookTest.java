package com.grahambartley.hardwired.phone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneBookTest {

  @ParameterizedTest(name = "{0} entries")
  @ValueSource(ints = {0, 1, 5})
  void everyEntryAndTheNextOrderSurviveASave(int count) {
    PhoneBook book = new PhoneBook();
    for (int i = 0; i < count; i++) {
      book.put(i, PhoneEntry.added(i % 2 == 0, List.of((long) i), book.nextOrder()));
    }
    PhoneBook loaded = PhoneBook.read(book.writeNbt(new NbtCompound(), null));
    assertEquals(book.entries(), loaded.entries());
    assertEquals(book.nextOrder(), loaded.nextOrder());
  }

  @ParameterizedTest(name = "pos {0}")
  @ValueSource(longs = {3L, 99L})
  void removingAnEntryForgetsIt(long pos) {
    PhoneBook book = new PhoneBook();
    book.put(pos, PhoneEntry.added(false, List.of(pos), book.nextOrder()));
    book.remove(pos);
    assertEquals(0, book.entries().size());
  }
}
