package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.lift.LiftFloors;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record LiftViewPayload(BlockPos gate, List<Integer> levels, int here)
    implements CustomPayload {

  public static final Id<LiftViewPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "lift_view"));
  public static final PacketCodec<RegistryByteBuf, LiftViewPayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          LiftViewPayload::gate,
          PacketCodecs.VAR_INT
              .<RegistryByteBuf>cast()
              .collect(PacketCodecs.toList(LiftFloors.MAX_FLOORS)),
          LiftViewPayload::levels,
          PacketCodecs.VAR_INT.<RegistryByteBuf>cast(),
          LiftViewPayload::here,
          LiftViewPayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
