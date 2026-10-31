package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.registry.ModBlockEntities;
import com.grahambartley.hardwired.registry.ModComponents;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.LampLook;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public final class LampBlockEntity extends BlockEntity {

  private static final String COLOR = "color";
  private static final String BRIGHTNESS = "brightness";
  private static final String OWN_COLOR = "own_color";
  private static final String OWN_BRIGHTNESS = "own_brightness";
  private static final String OWN_STAMP = "own_stamp";
  private static final int RERENDER = 8;

  private Hue color = Hue.PLAIN;
  private int brightness = LampLook.MAX_BRIGHTNESS;
  private long lastChangeTick = Long.MIN_VALUE / 2;
  private LampLook ownLook;
  private long ownStamp;

  public LampBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.LAMP, pos, state);
  }

  public Hue color() {
    return color;
  }

  public int brightness() {
    return brightness;
  }

  public long lastChangeTick() {
    return lastChangeTick;
  }

  public void markChanged(long tick) {
    lastChangeTick = tick;
  }

  public void setOwnLook(LampLook look, long stamp) {
    ownLook = look;
    ownStamp = stamp;
    markDirty();
  }

  public void recolourOwnLook(Hue hue) {
    if (ownLook != null) {
      setOwnLook(new LampLook(hue, ownLook.brightness()), ownStamp);
    }
  }

  public LampLook lookOver(LampLook networkLook, long networkStamp) {
    if (ownLook == null) {
      return networkLook;
    }
    if (LampLookRule.ownLookStands(ownLook, ownStamp, networkLook, networkStamp)) {
      return ownLook;
    }
    ownLook = null;
    markDirty();
    return networkLook;
  }

  public boolean setLook(Hue newColor, int newBrightness) {
    if (newColor == color && newBrightness == brightness) {
      return false;
    }
    color = newColor;
    brightness = newBrightness;
    markDirty();
    if (world != null && !world.isClient) {
      world.updateListeners(pos, getCachedState(), getCachedState(), RERENDER);
    }
    return true;
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    nbt.putString(COLOR, color.asString());
    nbt.putInt(BRIGHTNESS, brightness);
    if (ownLook != null) {
      nbt.putString(OWN_COLOR, ownLook.color().asString());
      nbt.putInt(OWN_BRIGHTNESS, ownLook.brightness());
      nbt.putLong(OWN_STAMP, ownStamp);
    }
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    color = Hue.byName(nbt.getString(COLOR));
    brightness = nbt.contains(BRIGHTNESS) ? nbt.getInt(BRIGHTNESS) : LampLook.MAX_BRIGHTNESS;
    ownLook =
        nbt.contains(OWN_COLOR)
            ? new LampLook(Hue.byName(nbt.getString(OWN_COLOR)), nbt.getInt(OWN_BRIGHTNESS))
            : null;
    ownStamp = nbt.getLong(OWN_STAMP);
    if (world != null && world.isClient) {
      world.updateListeners(pos, getCachedState(), getCachedState(), RERENDER);
      world.updateListeners(
          pos.up(StreetlightBlock.HEAD), getCachedState(), getCachedState(), RERENDER);
    }
  }

  @Override
  protected void readComponents(ComponentsAccess components) {
    super.readComponents(components);
    color = components.getOrDefault(ModComponents.LAMP_COLOR, Hue.PLAIN);
  }

  @Override
  protected void addComponents(ComponentMap.Builder builder) {
    super.addComponents(builder);
    builder.add(ModComponents.LAMP_COLOR, color);
  }

  @Override
  public void removeFromCopiedStackNbt(NbtCompound nbt) {
    nbt.remove(COLOR);
    nbt.remove(BRIGHTNESS);
    nbt.remove(OWN_COLOR);
    nbt.remove(OWN_BRIGHTNESS);
    nbt.remove(OWN_STAMP);
  }

  @Override
  public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
    return createNbt(registries);
  }

  @Override
  public Packet<ClientPlayPacketListener> toUpdatePacket() {
    return BlockEntityUpdateS2CPacket.create(this);
  }
}
