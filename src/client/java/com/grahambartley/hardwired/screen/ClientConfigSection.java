package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.client.radio.MicrophoneSource;
import com.grahambartley.hardwired.config.ClientRadioSettings;
import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.grahambartley.hardwired.config.ClientWireSettings;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.text.Text;

final class ClientConfigSection {
  private static final String KEY = "screen.hardwired.settings.";

  private ClientWireSettings settings = ClientSettingsStore.get();
  private ClientRadioSettings radio = ClientSettingsStore.radio();

  void addTo(final HardwiredConfigList list, final ConfigRows rows) {
    list.addRow(rows.header(KEY + "client"));
    list.addRow(
        rows.percentSlider(
            KEY + "throughopacity",
            ClientWireSettings.THROUGH_MIN,
            ClientWireSettings.THROUGH_MAX,
            settings.throughBlocksPercent(),
            value -> settings = settings.withThroughBlocksPercent(value)));
    list.addRow(
        rows.percentSlider(
            KEY + "surfaceopacity",
            ClientWireSettings.SURFACE_MIN,
            ClientWireSettings.SURFACE_MAX,
            settings.onSurfacePercent(),
            value -> settings = settings.withOnSurfacePercent(value)));
    list.addRow(
        rows.toggle(
            KEY + "showmarkers",
            settings.showMarkers(),
            value -> settings = settings.withShowMarkers(value)));
    list.addRow(
        rows.percentSlider(
            KEY + "radionoise",
            ClientRadioSettings.NOISE_MIN,
            ClientRadioSettings.NOISE_MAX,
            radio.radioNoisePercent(),
            value -> radio = radio.withRadioNoisePercent(value)));
    list.addRow(
        rows.percentSlider(
            KEY + "electrichum",
            ClientRadioSettings.NOISE_MIN,
            ClientRadioSettings.NOISE_MAX,
            radio.electricHumPercent(),
            value -> radio = radio.withElectricHumPercent(value)));
    list.addRow(
        rows.toggle(
            KEY + "hearvoicebroadcasts",
            radio.hearVoiceBroadcasts(),
            value -> radio = radio.withHearVoiceBroadcasts(value)));
    list.addRow(
        rows.choice(
            KEY + "microphone",
            microphones(),
            radio.microphone(),
            name ->
                name.isEmpty() ? Text.translatable(KEY + "microphone.default") : Text.literal(name),
            value -> radio = radio.withMicrophone(value)));
  }

  private List<String> microphones() {
    List<String> names = new ArrayList<>();
    names.add(ClientRadioSettings.DEFAULT_MICROPHONE);
    names.addAll(MicrophoneSource.names());
    if (!names.contains(radio.microphone())) {
      names.add(radio.microphone());
    }
    return names;
  }

  ClientRadioSettings radioSettings() {
    return radio;
  }

  ClientWireSettings settings() {
    return settings;
  }
}
