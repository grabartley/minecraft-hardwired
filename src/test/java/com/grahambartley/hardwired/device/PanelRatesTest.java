package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class PanelRatesTest {

  private static final UUID PLAYER = new UUID(0, 1);
  private static final long START = 100;

  @ParameterizedTest(name = "{0} after a refresh")
  @EnumSource(
      value = PanelAction.class,
      names = {"TOGGLE", "SET_LOOK"})
  void aRefreshNeverHoldsBackARealAction(PanelAction action) {
    PanelRates rates = new PanelRates();
    assertTrue(rates.allow(PLAYER, PanelAction.REFRESH, START));
    assertTrue(rates.allow(PLAYER, action, START));
  }

  @ParameterizedTest(name = "{0} then {1} after {2} ticks: {3}")
  @CsvSource({
    "TOGGLE,TOGGLE,1,false",
    "TOGGLE,SET_LOOK,1,false",
    "TOGGLE,TOGGLE,2,true",
    "REFRESH,REFRESH,9,false",
    "REFRESH,REFRESH,10,true",
    "TOGGLE,REFRESH,0,true"
  })
  void eachKindIsSpacedOutOnItsOwn(
      PanelAction first, PanelAction second, long gap, boolean allowed) {
    PanelRates rates = new PanelRates();
    assertTrue(rates.allow(PLAYER, first, START));
    assertEquals(allowed, rates.allow(PLAYER, second, START + gap));
  }

  @ParameterizedTest(name = "open again after {0} ticks: {1}")
  @CsvSource({"0,false", "4,false", "5,true"})
  void opensAreSpacedOut(long gap, boolean allowed) {
    PanelRates rates = new PanelRates();
    assertTrue(rates.allowOpen(PLAYER, START));
    assertEquals(allowed, rates.allowOpen(PLAYER, START + gap));
  }

  @ParameterizedTest(name = "{0} right after an open")
  @EnumSource(PanelAction.class)
  void openingHoldsNothingBack(PanelAction action) {
    PanelRates rates = new PanelRates();
    assertTrue(rates.allowOpen(PLAYER, START));
    assertTrue(rates.allow(PLAYER, action, START));
  }

  @ParameterizedTest(name = "forgetting clears {0}")
  @ValueSource(strings = {"open", "refresh", "action"})
  void forgettingAPlayerClearsEveryRate(String kind) {
    PanelRates rates = new PanelRates();
    rates.allowOpen(PLAYER, START);
    rates.allow(PLAYER, PanelAction.REFRESH, START);
    rates.allow(PLAYER, PanelAction.TOGGLE, START);
    rates.forget(PLAYER);
    assertTrue(
        switch (kind) {
          case "open" -> rates.allowOpen(PLAYER, START);
          case "refresh" -> rates.allow(PLAYER, PanelAction.REFRESH, START);
          default -> rates.allow(PLAYER, PanelAction.TOGGLE, START);
        });
  }
}
