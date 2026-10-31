package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.device.PanelRow;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record MainPanelViewPayload(
    BlockPos panel, boolean open, List<PanelRow> rows, int unknownPoints, int omitted)
    implements CustomPayload {

  public static final Id<MainPanelViewPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "main_panel_view"));
  public static final PacketCodec<RegistryByteBuf, MainPanelViewPayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          MainPanelViewPayload::panel,
          PacketCodecs.BOOL.<RegistryByteBuf>cast(),
          MainPanelViewPayload::open,
          PanelRow.LIST_PACKET_CODEC,
          MainPanelViewPayload::rows,
          PacketCodecs.VAR_INT,
          MainPanelViewPayload::unknownPoints,
          PacketCodecs.VAR_INT,
          MainPanelViewPayload::omitted,
          MainPanelViewPayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
