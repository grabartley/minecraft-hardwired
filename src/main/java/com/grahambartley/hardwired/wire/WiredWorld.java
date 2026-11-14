package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.phone.Switchboard;
import com.grahambartley.hardwired.radio.StationRegistry;

public interface WiredWorld {

  WireEngine hardwired$wires();

  StationRegistry hardwired$stations();

  Switchboard hardwired$switchboard();
}
