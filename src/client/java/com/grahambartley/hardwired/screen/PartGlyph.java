package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.circuit.PartType;
import java.util.List;

public final class PartGlyph {

  public static final int SIZE = 7;

  private PartGlyph() {}

  public static List<String> rows(PartType type) {
    return switch (type) {
      case AND ->
          List.of("####...", "#...#..", "#....#.", "#....#.", "#....#.", "#...#..", "####...");
      case OR ->
          List.of("###....", ".#..#..", "..#..#.", "..#...#", "..#..#.", ".#..#..", "###....");
      case NOT ->
          List.of("#......", "##.....", "#.#....", "#..#.##", "#.#..##", "##.....", "#......");
      case XOR ->
          List.of("#.##...", ".#..#..", "..#..#.", "..#...#", "..#..#.", ".#..#..", "#.##...");
      case NAND ->
          List.of("###....", "#..#...", "#...#..", "#...#.#", "#...#..", "#..#...", "###....");
      case NOR ->
          List.of("##.....", ".#.#...", "..#.#..", "..#..##", "..#.#..", ".#.#...", "##.....");
      case SR_LATCH ->
          List.of("###.##.", "#...#.#", "#...#.#", "###.##.", "..#.#.#", "..#.#.#", "###.#.#");
      case D_LATCH ->
          List.of("####...", "#...#..", "#....#.", "#....#.", "#....#.", "#...#..", "####...")
              .stream()
              .map(PartGlyph::withLatchBar)
              .toList();
      case TIMER ->
          List.of("..###..", ".#.#.#.", "#..#..#", "#..##.#", "#.....#", ".#...#.", "..###..");
      case COUNTER ->
          List.of(".#..#..", ".#..#..", "#######", ".#..#..", "#######", ".#..#..", ".#..#..");
    };
  }

  private static String withLatchBar(String row) {
    return row.substring(0, SIZE - 1) + "#";
  }
}
