package com.example.modb;

import net.fabricmc.api.ModInitializer;

public class ModB implements ModInitializer {

    @Override
    public void onInitialize() {
        Constants.LOG.info("Hello Fabric world from {}!", Constants.MOD_NAME);
        CommonClass.init();
    }
}
