package com.example.emeraldmod;

import com.example.emeraldmod.client.EffectSpriteLoader;
import com.example.emeraldmod.client.ModKeybinds;
import com.example.emeraldmod.client.TooltipHandler;
import net.fabricmc.api.ClientModInitializer;

public class EmeraldModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EmeraldMod.LOGGER.info("========================================");
        EmeraldMod.LOGGER.info("Initializing Emerald Mod Client");
        EmeraldMod.LOGGER.info("========================================");

        // Register keybinds for toggle system
        ModKeybinds.register();
        EmeraldMod.LOGGER.info("✓ Registered Keybinds (Tools: V, Armor: B)");

        // Register tooltip handler
        TooltipHandler.register();
        EmeraldMod.LOGGER.info("✓ Registered Tooltip Handler");

        // Register effect sprite loader
        EffectSpriteLoader.register();
        EmeraldMod.LOGGER.info("✓ Registered Effect Sprite Loader");

        EmeraldMod.LOGGER.info("========================================");
        EmeraldMod.LOGGER.info("Emerald Mod Client Initialized!");
        EmeraldMod.LOGGER.info("Keybinds can be customized in Controls");
        EmeraldMod.LOGGER.info("========================================");
    }
}