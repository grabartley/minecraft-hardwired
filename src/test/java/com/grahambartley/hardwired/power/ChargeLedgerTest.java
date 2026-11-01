package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ChargeLedgerTest {

  private static final class Cell implements ChargeStore {
    private final int capacity;
    private int charge;

    Cell(int charge, int capacity) {
      this.charge = charge;
      this.capacity = capacity;
    }

    @Override
    public int charge() {
      return charge;
    }

    @Override
    public int capacity() {
      return capacity;
    }

    @Override
    public void setCharge(int charge) {
      this.charge = charge;
    }
  }

  @ParameterizedTest(name = "{0}+{1} fill {2}")
  @CsvSource({"0,0,5,5,5,0", "9,0,5,5,10,4", "10,10,5,0,10,10", "0,0,99,20,10,10"})
  void fillStopsAtCapacityAndConservesCharge(
      int first, int second, int amount, int added, int firstAfter, int secondAfter) {
    Cell a = new Cell(first, 10);
    Cell b = new Cell(second, 10);
    assertEquals(added, ChargeLedger.fill(List.of(a, b), amount));
    assertEquals(firstAfter, a.charge());
    assertEquals(secondAfter, b.charge());
  }
}
