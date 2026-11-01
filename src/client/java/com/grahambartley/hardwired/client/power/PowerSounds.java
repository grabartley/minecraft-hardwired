package com.grahambartley.hardwired.client.power;

import com.grahambartley.hardwired.block.SourceBlockEntity;
import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.config.SyncedServerConfig;
import com.grahambartley.hardwired.network.HumLevelPayload;
import com.grahambartley.hardwired.power.PowerSoundLevels;
import com.grahambartley.hardwired.power.SourceKind;
import com.grahambartley.hardwired.registry.ModSounds;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;

public final class PowerSounds {

  private static final int LOOP_REACH = 24;
  private static final int MOST_LOOPS = 8;
  private static final int SOLAR_TICK_GAP = 60;
  private static final float SOLAR_TICK_VOLUME = 0.2F;

  private static final Set<SourceBlockEntity> SOURCES = new HashSet<>();
  private static final Map<SourceBlockEntity, LoopSound> LOOPS = new HashMap<>();
  private static volatile int humLoad;
  private static HumSound hum;

  private PowerSounds() {}

  public static void register() {
    ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register(
        (entity, world) -> {
          if (entity instanceof SourceBlockEntity source) {
            SOURCES.add(source);
          }
        });
    ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(
        (entity, world) -> {
          if (entity instanceof SourceBlockEntity source) {
            SOURCES.remove(source);
          }
        });
    ClientPlayNetworking.registerGlobalReceiver(
        HumLevelPayload.ID, (payload, context) -> humLoad = payload.load());
    ClientTickEvents.END_CLIENT_TICK.register(PowerSounds::tick);
  }

  private static void tick(MinecraftClient client) {
    if (client.world == null || client.player == null) {
      SOURCES.clear();
      LOOPS.clear();
      humLoad = 0;
      hum = null;
      return;
    }
    BlockPos listener = client.player.getBlockPos();
    List<SourceBlockEntity> heard =
        SOURCES.stream()
            .filter(SourceBlockEntity::isRunning)
            .filter(source -> source.getPos().isWithinDistance(listener, LOOP_REACH))
            .sorted(
                Comparator.comparingDouble(source -> source.getPos().getSquaredDistance(listener)))
            .limit(MOST_LOOPS)
            .toList();
    LOOPS.entrySet().removeIf(entry -> entry.getValue().isDone());
    for (SourceBlockEntity source : heard) {
      SoundEvent loop = loopOf(source.kind());
      if (loop != null && !LOOPS.containsKey(source)) {
        LoopSound sound = new LoopSound(loop, source, client.world.getRandom());
        LOOPS.put(source, sound);
        client.getSoundManager().play(sound);
      } else if (source.kind() == SourceKind.SOLAR
          && (client.world.getTime() + source.getPos().hashCode()) % SOLAR_TICK_GAP == 0) {
        client.world.playSoundAtBlockCenter(
            source.getPos(),
            ModSounds.SOLAR_TICK,
            SoundCategory.BLOCKS,
            SOLAR_TICK_VOLUME,
            1F,
            false);
      }
    }
    PowerParticles.tick(client, heard);
    keepHumming(client);
  }

  private static double humLevel() {
    PowerSettings power = SyncedServerConfig.get().power();
    return PowerSoundLevels.hum(
        humLoad,
        power.humStartLoad(),
        power.humFullLoad(),
        ClientSettingsStore.radio().electricHumPercent());
  }

  private static void keepHumming(MinecraftClient client) {
    if (hum != null && hum.isDone()) {
      hum = null;
    }
    if (hum == null && humLevel() > 0) {
      hum = new HumSound(PowerSounds::humLevel, client.world.getRandom());
      client.getSoundManager().play(hum);
    }
  }

  private static SoundEvent loopOf(SourceKind kind) {
    return switch (kind) {
      case CELL -> ModSounds.CELL_LOOP;
      case GENERATOR -> ModSounds.GENERATOR_LOOP;
      case DYNAMO -> ModSounds.DYNAMO_LOOP;
      case SOLAR -> null;
    };
  }
}
