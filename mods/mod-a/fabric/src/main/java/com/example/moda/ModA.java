package com.example.moda;

import net.fabricmc.api.ModInitializer;

public class ModA implements ModInitializer {

    @Override
    public void onInitialize() {
        Constants.LOG.info("Hello Fabric world from {}!", Constants.MOD_NAME);
        CommonClass.init();
    }
}
