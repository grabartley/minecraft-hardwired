package com.grahambartley.hardwired.radio;

public final class VoiceJitter {

  private final byte[] ring;
  private final int prefill;
  private int head;
  private int held;
  private boolean flowing;

  public VoiceJitter(int prefillBytes, int mostBytes) {
    ring = new byte[mostBytes];
    prefill = prefillBytes;
  }

  public synchronized void offer(byte[] pcm) {
    int skip = Math.max(pcm.length - ring.length, 0);
    for (int i = skip; i < pcm.length; i++) {
      if (held == ring.length) {
        head = (head + 1) % ring.length;
        held--;
      }
      ring[(head + held) % ring.length] = pcm[i];
      held++;
    }
  }

  public synchronized byte[] take(int length) {
    byte[] out = new byte[length];
    if (!flowing && held < prefill) {
      return out;
    }
    flowing = held >= length;
    int count = Math.min(held, length);
    for (int i = 0; i < count; i++) {
      out[i] = ring[head];
      head = (head + 1) % ring.length;
    }
    held -= count;
    return out;
  }

  public synchronized int held() {
    return held;
  }
}
