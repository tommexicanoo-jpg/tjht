package com.theecho;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TheEcho implements ModInitializer {
    public static final String MOD_ID = "the_echo";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        EchoConfig.load();
        ModBlocks.register();
        ModEntities.register();
        EchoManager.register();
        EchoCommands.register();
        LOGGER.info("...algo te está mirando.");
    }
}
