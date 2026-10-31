package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ChargeStoreTest {

  private static ChargeStore store(int start, int capacity) {
    return new ChargeStore() {
      private int charge = start;

      @Override
      public int charge() {
        return charge;
      }

      @Override
      public int capacity() {
        return capacity;
      }

      @Override
      public void setCharge(int next) {
        charge = next;
      }
    };
  }

  @ParameterizedTest(name = "{0} of {1} fill {2}")
  @CsvSource({
    "0,100,30,30,30",
    "90,100,30,10,100",
    "100,100,1,0,100",
    "50,100,-5,0,50",
    "500,100,30,0,500"
  })
  void fillTakesOnlyWhatFitsAndNeverSpillsWhenCapacityFalls(
      int charge, int capacity, int amount, int added, int after) {
    ChargeStore store = store(charge, capacity);
    assertEquals(added, store.fill(amount));
    assertEquals(after, store.charge());
  }

  @ParameterizedTest(name = "{0} drain {1}")
  @CsvSource({"50,30,30,20", "10,30,10,0", "10,-3,0,10"})
  void drainTakesOnlyWhatIsThere(int charge, int amount, int taken, int after) {
    ChargeStore store = store(charge, 100);
    assertEquals(taken, store.drain(amount));
    assertEquals(after, store.charge());
  }
}
