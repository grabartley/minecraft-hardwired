package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.grahambartley.hardwired.config.ClientWireSettings;

final class ClientConfigSection {
  private static final String KEY = "screen.hardwired.settings.";

  private ClientWireSettings settings = ClientSettingsStore.get();

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
  }

  ClientWireSettings settings() {
    return settings;
  }
}
