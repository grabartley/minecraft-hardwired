package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.device.IntercomLine;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record PhoneActionPayload(BlockPos phone, Action action, int number, String line)
    implements CustomPayload {

  public enum Action {
    DIAL,
    HANG_UP,
    SAY;

    static Action byIndex(int index) {
      Action[] values = values();
      return values[Math.floorMod(index, values.length)];
    }
  }

  public static PhoneActionPayload dial(BlockPos phone, int number) {
    return new PhoneActionPayload(phone, Action.DIAL, number, "");
  }

  public static PhoneActionPayload hangUp(BlockPos phone) {
    return new PhoneActionPayload(phone, Action.HANG_UP, 0, "");
  }

  public static PhoneActionPayload say(BlockPos phone, String line) {
    return new PhoneActionPayload(phone, Action.SAY, 0, line);
  }

  public static final Id<PhoneActionPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "phone_action"));
  public static final PacketCodec<RegistryByteBuf, PhoneActionPayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          PhoneActionPayload::phone,
          PacketCodecs.VAR_INT.<RegistryByteBuf>cast().xmap(Action::byIndex, Action::ordinal),
          PhoneActionPayload::action,
          PacketCodecs.VAR_INT.<RegistryByteBuf>cast(),
          PhoneActionPayload::number,
          PacketCodecs.string(IntercomLine.MAX_LENGTH).<RegistryByteBuf>cast(),
          PhoneActionPayload::line,
          PhoneActionPayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
