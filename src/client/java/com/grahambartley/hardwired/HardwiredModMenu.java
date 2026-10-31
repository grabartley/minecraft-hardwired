package com.grahambartley.hardwired;

import com.grahambartley.hardwired.screen.HardwiredConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class HardwiredModMenu implements ModMenuApi {
  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return HardwiredConfigScreen::new;
  }
}
