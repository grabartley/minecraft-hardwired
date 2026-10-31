package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.wire.LampLook;
import com.grahambartley.hardwired.wire.Scene;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record PanelActionPayload(BlockPos pos, LampLook look, int slot, String sceneName)
    implements CustomPayload {

  public static final int NO_SLOT = -1;
  public static final Id<PanelActionPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "panel_action"));
  public static final PacketCodec<RegistryByteBuf, PanelActionPayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          PanelActionPayload::pos,
          LampLook.PACKET_CODEC,
          PanelActionPayload::look,
          PacketCodecs.VAR_INT,
          PanelActionPayload::slot,
          PacketCodecs.string(Scene.MAX_NAME_LENGTH),
          PanelActionPayload::sceneName,
          PanelActionPayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
