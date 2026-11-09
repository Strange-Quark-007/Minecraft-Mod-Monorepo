package com.example.moda;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class ModA {

    public ModA(IEventBus eventBus) {
        Constants.LOG.info("Hello NeoForge world from {}!", Constants.MOD_NAME);
        CommonClass.init();
        eventBus.addListener(ModDataGenerator::gatherData);
    }
}
