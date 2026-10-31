package com.grahambartley.hardwired.power;

public interface ChargeStore {

  int charge();

  int capacity();

  void setCharge(int charge);

  default int fill(int amount) {
    int added = Math.min(Math.max(amount, 0), capacity() - charge());
    setCharge(charge() + added);
    return added;
  }

  default int drain(int amount) {
    int taken = Math.min(Math.max(amount, 0), charge());
    setCharge(charge() - taken);
    return taken;
  }
}
