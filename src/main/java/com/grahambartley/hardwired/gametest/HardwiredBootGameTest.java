package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.Hardwired;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

public class HardwiredBootGameTest implements FabricGameTest {

  @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
  public void modIsLoadedOnTheServer(TestContext context) {
    context.assertTrue(
        FabricLoader.getInstance().isModLoaded(Hardwired.MOD_ID),
        Hardwired.MOD_ID + " should be loaded on the test server");
    context.complete();
  }
}
