package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.radio.VoiceFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

public final class MicrophoneSource implements CaptureSource {

  private static final AudioFormat FORMAT =
      new AudioFormat(
          VoiceFormat.SAMPLE_RATE, VoiceFormat.BYTES_PER_SAMPLE * Byte.SIZE, 1, true, false);
  private static final int LINE_BUFFER_FRAMES = 4;

  private final String chosen;
  private volatile TargetDataLine line;

  public MicrophoneSource(String chosen) {
    this.chosen = chosen;
  }

  public static List<String> names() {
    List<String> names = new ArrayList<>();
    DataLine.Info wanted = new DataLine.Info(TargetDataLine.class, FORMAT);
    for (Mixer.Info info : AudioSystem.getMixerInfo()) {
      if (AudioSystem.getMixer(info).isLineSupported(wanted)) {
        names.add(info.getName());
      }
    }
    return names;
  }

  private static Optional<Mixer.Info> mixerNamed(String name) {
    for (Mixer.Info info : AudioSystem.getMixerInfo()) {
      if (info.getName().equals(name)) {
        return Optional.of(info);
      }
    }
    return Optional.empty();
  }

  @Override
  public void open() throws MicFailure {
    try {
      Optional<Mixer.Info> mixer = mixerNamed(chosen);
      line =
          mixer.isPresent()
              ? AudioSystem.getTargetDataLine(FORMAT, mixer.get())
              : AudioSystem.getTargetDataLine(FORMAT);
      line.open(
          FORMAT, LINE_BUFFER_FRAMES * VoiceFormat.FRAME_SAMPLES * VoiceFormat.BYTES_PER_SAMPLE);
      line.start();
    } catch (LineUnavailableException failure) {
      throw new MicFailure(MicProblem.BUSY, failure);
    } catch (IllegalArgumentException failure) {
      throw new MicFailure(MicProblem.NO_INPUT, failure);
    } catch (SecurityException failure) {
      throw new MicFailure(MicProblem.REFUSED, failure);
    }
  }

  @Override
  public void read(short[] frame) throws MicFailure {
    fill(line, frame);
  }

  static void fill(TargetDataLine source, short[] frame) throws MicFailure {
    byte[] bytes = new byte[frame.length * VoiceFormat.BYTES_PER_SAMPLE];
    int done = 0;
    while (done < bytes.length) {
      int got = source.read(bytes, done, bytes.length - done);
      if (got <= 0) {
        throw new MicFailure(MicProblem.NO_INPUT);
      }
      done += got;
    }
    short[] samples = VoiceFormat.toSamples(bytes, done);
    System.arraycopy(samples, 0, frame, 0, samples.length);
  }

  @Override
  public void close() {
    TargetDataLine opened = line;
    if (opened != null) {
      opened.close();
    }
  }
}
