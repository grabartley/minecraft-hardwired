package com.grahambartley.hardwired.phone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CallAudienceTest {

  private static final UUID ALICE = new UUID(0, 1);
  private static final UUID BOB = new UUID(0, 2);
  private static final UUID CAROL = new UUID(0, 3);
  private static final UUID STRANGER = new UUID(0, 9);

  private static Map<Long, UUID> members() {
    Map<Long, UUID> members = new LinkedHashMap<>();
    members.put(1L, ALICE);
    members.put(2L, BOB);
    members.put(3L, CAROL);
    return members;
  }

  private static UUID player(String name) {
    return switch (name) {
      case "alice" -> ALICE;
      case "bob" -> BOB;
      default -> STRANGER;
    };
  }

  @ParameterizedTest(name = "{0} on phone {1}")
  @CsvSource({
    "alice,1,true,true,803,2",
    "bob,2,true,true,4,2",
    "alice,2,true,true,803,0",
    "stranger,1,true,true,803,0",
    "alice,1,false,true,803,0",
    "alice,1,true,false,803,0",
    "alice,1,true,true,3,0",
    "alice,1,true,true,804,0"
  })
  void aFrameReachesEveryoneElseOnTheCallOnlyWhenEveryRuleHolds(
      String speaker, long phone, boolean connected, boolean voice, int bytes, int heard) {
    List<UUID> audience =
        CallAudience.of(members(), connected, phone, player(speaker), voice, bytes);
    assertEquals(heard, audience.size());
    assertEquals(false, audience.contains(player(speaker)));
  }
}
