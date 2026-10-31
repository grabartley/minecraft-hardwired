package com.grahambartley.hardwired;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hardwired implements ModInitializer {

  public static final String MOD_ID = "hardwired";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitialize() {
    LOGGER.info("Hardwired initialised");
  }
}
