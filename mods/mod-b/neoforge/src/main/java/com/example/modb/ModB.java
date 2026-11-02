package com.example.modb;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class ModB {

    public ModB(IEventBus eventBus) {
        Constants.LOG.info("Hello NeoForge world from {}!", Constants.MOD_NAME);
        CommonClass.init();
    }
}
