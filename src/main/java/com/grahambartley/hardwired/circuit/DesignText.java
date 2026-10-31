package com.grahambartley.hardwired.circuit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class DesignText {

  public static final int MAX_LENGTH = 32 * 1024;

  static final String VERSION = "HWC1";

  private static final String VERSION_FAMILY = "HWC";
  private static final String SECTION_SPLIT = "\\|";
  private static final String SECTION_JOIN = "|";
  private static final String ITEM = ",";
  private static final String FIELD = ":";
  private static final int SECTIONS = 4;
  private static final int PART_FIELDS = 4;
  private static final int LINE_FIELDS = 3;
  private static final int MAX_NUMBER_DIGITS = 6;
  private static final int MAX_PARTS_READ = 1024;
  private static final int MAX_LINES_READ = 4096;

  private DesignText() {}

  public sealed interface Result {
    record Accepted(Design design) implements Result {}

    record Refused(Refusal reason) implements Result {}
  }

  public static String encode(Design design) {
    String sides =
        design.sides().stream()
            .map(mode -> String.valueOf(mode.code()))
            .collect(Collectors.joining());
    String parts =
        design.parts().stream()
            .map(
                part ->
                    String.join(
                        FIELD,
                        part.type().id(),
                        Integer.toString(part.x()),
                        Integer.toString(part.y()),
                        Integer.toString(part.setting())))
            .collect(Collectors.joining(ITEM));
    String lines =
        design.lines().stream()
            .map(line -> line.from() + FIELD + line.to() + FIELD + line.pin())
            .collect(Collectors.joining(ITEM));
    return String.join(SECTION_JOIN, VERSION, sides, parts, lines);
  }

  public static Result parse(String text, int maxParts) {
    if (text.length() > MAX_LENGTH) {
      return new Result.Refused(Refusal.TOO_LONG);
    }
    String[] sections = text.strip().split(SECTION_SPLIT, -1);
    if (!sections[0].equals(VERSION)) {
      return new Result.Refused(
          isLaterVersion(sections[0]) ? Refusal.NEWER_VERSION : Refusal.BAD_TEXT);
    }
    if (sections.length != SECTIONS) {
      return new Result.Refused(Refusal.BAD_TEXT);
    }
    Optional<Design> design = read(sections);
    if (design.isEmpty()) {
      return new Result.Refused(Refusal.BAD_TEXT);
    }
    return Validator.check(design.get(), maxParts)
        .<Result>map(Result.Refused::new)
        .orElseGet(() -> new Result.Accepted(design.get()));
  }

  private static boolean isLaterVersion(String word) {
    if (!word.startsWith(VERSION_FAMILY)) {
      return false;
    }
    return number(word.substring(VERSION_FAMILY.length())).isPresent();
  }

  private static Optional<Design> read(String[] sections) {
    List<PortMode> sides = readSides(sections[1]);
    List<Part> parts = readParts(sections[2]);
    List<Line> lines = readLines(sections[3]);
    if (sides == null || parts == null || lines == null) {
      return Optional.empty();
    }
    return Optional.of(new Design(sides, parts, lines));
  }

  private static List<PortMode> readSides(String section) {
    if (section.length() != Side.COUNT) {
      return null;
    }
    List<PortMode> sides = new ArrayList<>(Side.COUNT);
    for (char code : section.toCharArray()) {
      PortMode mode = PortMode.byCode(code);
      if (mode == null) {
        return null;
      }
      sides.add(mode);
    }
    return sides;
  }

  private static List<Part> readParts(String section) {
    if (section.isEmpty()) {
      return List.of();
    }
    String[] items = section.split(ITEM, -1);
    if (items.length > MAX_PARTS_READ) {
      return null;
    }
    List<Part> parts = new ArrayList<>(items.length);
    for (String item : items) {
      String[] fields = item.split(FIELD, -1);
      if (fields.length != PART_FIELDS || PartType.byId(fields[0]) == null) {
        return null;
      }
      Optional<Integer> x = number(fields[1]);
      Optional<Integer> y = number(fields[2]);
      Optional<Integer> setting = number(fields[3]);
      if (x.isEmpty() || y.isEmpty() || setting.isEmpty()) {
        return null;
      }
      parts.add(new Part(PartType.byId(fields[0]), x.get(), y.get(), setting.get()));
    }
    return parts;
  }

  private static List<Line> readLines(String section) {
    if (section.isEmpty()) {
      return List.of();
    }
    String[] items = section.split(ITEM, -1);
    if (items.length > MAX_LINES_READ) {
      return null;
    }
    List<Line> lines = new ArrayList<>(items.length);
    for (String item : items) {
      String[] fields = item.split(FIELD, -1);
      if (fields.length != LINE_FIELDS) {
        return null;
      }
      Optional<Integer> from = number(fields[0]);
      Optional<Integer> to = number(fields[1]);
      Optional<Integer> pin = number(fields[2]);
      if (from.isEmpty() || to.isEmpty() || pin.isEmpty()) {
        return null;
      }
      lines.add(new Line(from.get(), to.get(), pin.get()));
    }
    return lines;
  }

  private static Optional<Integer> number(String field) {
    if (field.isEmpty() || field.length() > MAX_NUMBER_DIGITS) {
      return Optional.empty();
    }
    for (char digit : field.toCharArray()) {
      if (digit < '0' || digit > '9') {
        return Optional.empty();
      }
    }
    return Optional.of(Integer.parseInt(field));
  }
}
