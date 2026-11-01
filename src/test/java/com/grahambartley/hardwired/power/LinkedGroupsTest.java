package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LinkedGroupsTest {

  private static Map<String, List<String>> links(String edges) {
    Map<String, List<String>> links = new HashMap<>();
    for (String edge : edges.split(" ")) {
      String[] ends = edge.split("-");
      links.computeIfAbsent(ends[0], key -> new ArrayList<>()).add(ends[1]);
      links.computeIfAbsent(ends[1], key -> new ArrayList<>()).add(ends[0]);
    }
    return links;
  }

  private static List<String> shown(List<Set<String>> groups) {
    return groups.stream().map(group -> String.join("", new TreeSet<>(group))).sorted().toList();
  }

  @ParameterizedTest(name = "{0} from {1}")
  @CsvSource({"a-b b-c d-e, a, abc", "a-b b-c d-e, d, de", "a-a, a, a", "a-b c-d, c, cd"})
  void reachFollowsEveryLink(String edges, String start, String found) {
    Map<String, List<String>> links = links(edges);
    Set<String> reached = LinkedGroups.reach(List.of(start), node -> links.get(node));
    assertEquals(found, String.join("", new TreeSet<>(reached)));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({"a-b b-c d-e, 'abc de'", "a-b c-d e-f, 'ab cd ef'", "a-b b-c c-a, abc"})
  void nodesFallIntoSeparateGroups(String edges, String expected) {
    Map<String, List<String>> links = links(edges);
    assertEquals(
        Arrays.asList(expected.split(" ")),
        shown(LinkedGroups.of(links.keySet(), node -> links.get(node))));
  }

  @Test
  void aOneWayLinkNeverPutsANodeInTwoGroups() {
    Map<String, List<String>> links = Map.of("a", List.of(), "b", List.of("a"));
    assertEquals(
        List.of("a", "b"), shown(LinkedGroups.of(List.of("a", "b"), node -> links.get(node))));
  }

  @Test
  void groupsKeepToTheNodesAsked() {
    Map<String, List<String>> links = links("a-b b-c");
    assertEquals(List.of("ab"), shown(LinkedGroups.of(List.of("a", "b"), links::get)));
  }
}
