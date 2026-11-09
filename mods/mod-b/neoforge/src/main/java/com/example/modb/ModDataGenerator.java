package com.example.modb;

import net.neoforged.neoforge.data.event.GatherDataEvent;

public class ModDataGenerator {

    public static void gatherData(GatherDataEvent.Client event) {
        // Register providers here. The data run uses clientData(), so providers
        // added here run for both client and server data.
        // event.addProvider(new MyRecipeProvider(event.getGenerator().getPackOutput(), event.getLookupProvider()));
    }
}
