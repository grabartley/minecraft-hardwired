package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.device.CircuitActions;
import com.grahambartley.hardwired.device.ControllerActions;
import com.grahambartley.hardwired.device.DeviceActions;
import com.grahambartley.hardwired.device.IntercomActions;
import com.grahambartley.hardwired.device.MainPanelActions;
import com.grahambartley.hardwired.lift.LiftActions;
import com.grahambartley.hardwired.network.ServerConfigPayloads.EditServerConfigC2SPayload;
import com.grahambartley.hardwired.network.ServerConfigPayloads.SyncServerConfigS2CPayload;
import com.grahambartley.hardwired.network.handler.ServerConfigNetworkHandler;
import com.grahambartley.hardwired.radio.DeskActions;
import com.grahambartley.hardwired.radio.RadioActions;
import com.grahambartley.hardwired.wire.WireInspector;
import com.grahambartley.hardwired.wire.WireLaying;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {

  private ModNetworking() {}

  public static void register() {
    PayloadTypeRegistry.playC2S().register(WireLayPayload.ID, WireLayPayload.CODEC);
    PayloadTypeRegistry.playC2S().register(WireRemovePayload.ID, WireRemovePayload.CODEC);
    PayloadTypeRegistry.playC2S().register(WireInspectPayload.ID, WireInspectPayload.CODEC);
    PayloadTypeRegistry.playS2C().register(WireChunkPayload.ID, WireChunkPayload.CODEC);
    PayloadTypeRegistry.playS2C().register(WireViewClearPayload.ID, WireViewClearPayload.CODEC);
    PayloadTypeRegistry.playS2C()
        .register(WireInspectResultPayload.ID, WireInspectResultPayload.CODEC);
    PayloadTypeRegistry.playS2C()
        .register(SyncServerConfigS2CPayload.ID, SyncServerConfigS2CPayload.CODEC);
    PayloadTypeRegistry.playC2S()
        .register(EditServerConfigC2SPayload.ID, EditServerConfigC2SPayload.CODEC);
    ServerPlayNetworking.registerGlobalReceiver(
        EditServerConfigC2SPayload.ID, ServerConfigNetworkHandler::handleEditServerConfig);
    PayloadTypeRegistry.playC2S().register(PanelActionPayload.ID, PanelActionPayload.CODEC);
    PayloadTypeRegistry.playC2S().register(JunctionSetPayload.ID, JunctionSetPayload.CODEC);
    PayloadTypeRegistry.playS2C().register(OpenDevicePayload.ID, OpenDevicePayload.CODEC);
    PayloadTypeRegistry.playC2S().register(ControllerSetPayload.ID, ControllerSetPayload.CODEC);
    PayloadTypeRegistry.playC2S().register(MainPanelActionPayload.ID, MainPanelActionPayload.CODEC);
    PayloadTypeRegistry.playS2C().register(MainPanelViewPayload.ID, MainPanelViewPayload.CODEC);
    PayloadTypeRegistry.playC2S().register(CircuitActionPayload.ID, CircuitActionPayload.CODEC);
    PayloadTypeRegistry.playS2C().register(CircuitResultPayload.ID, CircuitResultPayload.CODEC);
    PayloadTypeRegistry.playC2S().register(IntercomSayPayload.ID, IntercomSayPayload.CODEC);
    PayloadTypeRegistry.playC2S().register(LiftChoosePayload.ID, LiftChoosePayload.CODEC);
    PayloadTypeRegistry.playC2S().register(DeskActionPayload.ID, DeskActionPayload.CODEC);
    PayloadTypeRegistry.playC2S().register(RadioSetPayload.ID, RadioSetPayload.CODEC);
    PayloadTypeRegistry.playS2C().register(LiftViewPayload.ID, LiftViewPayload.CODEC);
    ServerPlayNetworking.registerGlobalReceiver(
        DeskActionPayload.ID, (payload, context) -> DeskActions.act(context.player(), payload));
    ServerPlayNetworking.registerGlobalReceiver(
        RadioSetPayload.ID, (payload, context) -> RadioActions.set(context.player(), payload));
    ServerPlayNetworking.registerGlobalReceiver(
        LiftChoosePayload.ID, (payload, context) -> LiftActions.choose(context.player(), payload));
    ServerPlayNetworking.registerGlobalReceiver(
        IntercomSayPayload.ID,
        (payload, context) -> IntercomActions.say(context.player(), payload));
    ServerPlayNetworking.registerGlobalReceiver(
        CircuitActionPayload.ID,
        (payload, context) -> CircuitActions.act(context.player(), payload));
    ServerPlayNetworking.registerGlobalReceiver(
        ControllerSetPayload.ID,
        (payload, context) -> ControllerActions.save(context.player(), payload));
    ServerPlayNetworking.registerGlobalReceiver(
        MainPanelActionPayload.ID,
        (payload, context) -> MainPanelActions.act(context.player(), payload));
    ServerPlayNetworking.registerGlobalReceiver(
        PanelActionPayload.ID,
        (payload, context) -> DeviceActions.panel(context.player(), payload));
    ServerPlayNetworking.registerGlobalReceiver(
        JunctionSetPayload.ID,
        (payload, context) -> DeviceActions.junction(context.player(), payload));
    ServerPlayNetworking.registerGlobalReceiver(
        WireLayPayload.ID, (payload, context) -> WireLaying.lay(context.player(), payload.faces()));
    ServerPlayNetworking.registerGlobalReceiver(
        WireRemovePayload.ID,
        (payload, context) -> WireLaying.remove(context.player(), payload.face()));
    ServerPlayNetworking.registerGlobalReceiver(
        WireInspectPayload.ID,
        (payload, context) -> WireInspector.inspect(context.player(), payload));
  }
}
