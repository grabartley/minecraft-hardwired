package com.grahambartley.hardwired.device;

import java.util.UUID;

public final class PanelRates {

  private static final int MIN_TICKS_BETWEEN_OPENS = 5;
  private static final int MIN_TICKS_BETWEEN_REFRESHES = 10;
  private static final int MIN_TICKS_BETWEEN_ACTIONS = 2;

  private final ActionRate opens = new ActionRate(MIN_TICKS_BETWEEN_OPENS);
  private final ActionRate refreshes = new ActionRate(MIN_TICKS_BETWEEN_REFRESHES);
  private final ActionRate actions = new ActionRate(MIN_TICKS_BETWEEN_ACTIONS);

  public boolean allowOpen(UUID player, long now) {
    return opens.allow(player, now);
  }

  public boolean allow(UUID player, PanelAction action, long now) {
    return (action == PanelAction.REFRESH ? refreshes : actions).allow(player, now);
  }

  public void forget(UUID player) {
    opens.forget(player);
    refreshes.forget(player);
    actions.forget(player);
  }
}
