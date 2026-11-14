package com.grahambartley.hardwired.phone;

import com.grahambartley.hardwired.network.PhoneViewPayload;
import com.grahambartley.hardwired.wire.WireViewer;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public final class PhoneViews {

  private PhoneViews() {}

  public static void send(ServerPlayerEntity player, long phone, LineStatus status) {
    PhoneDirectory directory = PhoneDirectory.of(player.getServerWorld());
    int number = directory.entry(phone).map(PhoneEntry::number).orElse(PhoneNumbering.NONE);
    WireViewer viewer = WireViewer.of(player);
    List<PhoneDirectory.Listing> listings =
        directory.systemOf(phone).filter(viewer::canSee).isPresent()
            ? directory.listings(phone)
            : List.of();
    ServerPlayNetworking.send(
        player, new PhoneViewPayload(BlockPos.fromLong(phone), number, status.ordinal(), listings));
  }
}
