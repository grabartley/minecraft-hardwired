package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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

  @ParameterizedTest(name = "{0}+{1} drain {2}")
  @CsvSource({
    "10,10,5,true,5,10",
    "10,10,15,true,0,5",
    "10,10,20,true,0,0",
    "10,10,21,false,10,10",
    "0,0,1,false,0,0",
    "3,4,0,true,3,4"
  })
  void drainIsAllOrNothing(
      int first, int second, int amount, boolean drained, int firstAfter, int secondAfter) {
    Cell a = new Cell(first, 100);
    Cell b = new Cell(second, 100);
    assertEquals(drained, ChargeLedger.tryDrain(List.of(a, b), amount));
    assertEquals(firstAfter, a.charge());
    assertEquals(secondAfter, b.charge());
  }

  @ParameterizedTest(name = "{0}+{1} available {2}")
  @CsvSource({
    "0,0,0",
    "3,4,7",
    "2147483647,0,2147483647",
    "2147483647,1,2147483647",
    "2147483647,2147483647,2147483647"
  })
  void availableAddsUpAndNeverOverflows(int first, int second, int available) {
    List<Cell> cells =
        List.of(new Cell(first, Integer.MAX_VALUE), new Cell(second, Integer.MAX_VALUE));
    assertEquals(available, ChargeLedger.available(cells));
  }

  @ParameterizedTest(name = "drain {0} over two full cells")
  @ValueSource(ints = {1, Integer.MAX_VALUE})
  void aHugeNetworkCanStillPay(int amount) {
    Cell a = new Cell(Integer.MAX_VALUE, Integer.MAX_VALUE);
    Cell b = new Cell(Integer.MAX_VALUE, Integer.MAX_VALUE);
    assertEquals(true, ChargeLedger.tryDrain(List.of(a, b), amount));
    assertEquals(Integer.MAX_VALUE - amount, a.charge());
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
