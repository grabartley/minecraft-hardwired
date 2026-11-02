package com.grahambartley.hardwired.client.glow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

final class GlowBuildSlot<T> {

  private final T empty;
  private T current;
  private CompletableFuture<T> pending;

  GlowBuildSlot(T empty) {
    this.empty = empty;
    this.current = empty;
  }

  T current() {
    if (pending != null && pending.isDone()) {
      current = pending.join();
      pending = null;
    }
    return current;
  }

  boolean isBusy() {
    return pending != null;
  }

  void build(Supplier<T> task, Executor executor) {
    pending = CompletableFuture.supplyAsync(task, executor);
  }

  void clear() {
    current = empty;
    pending = null;
  }
}
