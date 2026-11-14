package com.grahambartley.hardwired.line;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.wire.Hue;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SpanDraftsTest {

  private static final UUID ALICE = new UUID(0, 1);
  private static final Identifier OVERWORLD = Identifier.ofVanilla("overworld");
  private static final SpanDrafts.Draft DRAFT =
      new SpanDrafts.Draft(OVERWORLD, new BlockPos(4, 72, 9), Hue.RED);

  @ParameterizedTest(name = "{0} in {1}")
  @CsvSource({
    "00000000-0000-0000-0000-000000000001,overworld,true",
    "00000000-0000-0000-0000-000000000002,overworld,false",
    "00000000-0000-0000-0000-000000000001,the_nether,false"
  })
  void aDraftBelongsToOnePlayerInOneWorld(UUID player, String world, boolean found) {
    SpanDrafts drafts = new SpanDrafts();
    drafts.start(ALICE, DRAFT);
    assertEquals(
        found ? Optional.of(DRAFT) : Optional.empty(),
        drafts.of(player, Identifier.ofVanilla(world)));
  }

  @ParameterizedTest(name = "stale {0}")
  @CsvSource({"true", "false"})
  void aStaleDraftIsDropped(boolean stale) {
    SpanDrafts drafts = new SpanDrafts();
    drafts.start(ALICE, DRAFT);
    drafts.dropWhere((player, draft) -> stale);
    assertEquals(!stale, drafts.of(ALICE, OVERWORLD).isPresent());
    drafts.end(ALICE);
    assertEquals(Optional.empty(), drafts.of(ALICE, OVERWORLD));
  }
}
