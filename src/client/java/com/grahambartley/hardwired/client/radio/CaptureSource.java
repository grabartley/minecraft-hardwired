package com.grahambartley.hardwired.client.radio;

public interface CaptureSource {

  void open() throws MicFailure;

  void read(short[] frame) throws MicFailure;

  void close();
}
