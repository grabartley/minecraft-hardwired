package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.device.PanelAction;
import com.grahambartley.hardwired.wire.LampLook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record MainPanelActionPayload(
    BlockPos panel, BlockPos target, PanelAction action, LampLook look) implements CustomPayload {

  public static final Id<MainPanelActionPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "main_panel_action"));
  private static final PacketCodec<RegistryByteBuf, PanelAction> ACTION_CODEC =
      PacketCodecs.indexed(PanelAction::byIndex, PanelAction::ordinal).cast();
  public static final PacketCodec<RegistryByteBuf, MainPanelActionPayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          MainPanelActionPayload::panel,
          BlockPos.PACKET_CODEC,
          MainPanelActionPayload::target,
          ACTION_CODEC,
          MainPanelActionPayload::action,
          LampLook.PACKET_CODEC,
          MainPanelActionPayload::look,
          MainPanelActionPayload::new);

  public static MainPanelActionPayload refresh(BlockPos panel) {
    return new MainPanelActionPayload(panel, panel, PanelAction.REFRESH, LampLook.DEFAULT);
  }

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
