package com.example.emeraldmod;

import com.example.emeraldmod.client.EffectSpriteLoader;
import com.example.emeraldmod.client.TooltipHandler;
import net.fabricmc.api.ClientModInitializer;

public class EmeraldModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EmeraldMod.LOGGER.info("Initializing Emerald Mod Client");

        // Register tooltip handler
        TooltipHandler.register();
        EmeraldMod.LOGGER.info("✓ Registered Tooltip Handler");

        // Register effect sprite loader
        EffectSpriteLoader.register();
        EmeraldMod.LOGGER.info("✓ Registered Effect Sprite Loader");
    }
}