package com.example.emeraldmod;

import com.example.emeraldmod.effect.ModEffects;
import com.example.emeraldmod.event.*;
import com.example.emeraldmod.item.ModItemGroups;
import com.example.emeraldmod.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

public class EmeraldMod implements ModInitializer {
    public static final String MOD_ID = "emeraldmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("========================================");
        LOGGER.info("Initializing Emerald Tools & Armor Mod");
        LOGGER.info("========================================");

        debugResourceLoading();
        debugArmorModels();
        debugTextures();

        // Register custom effects FIRST (sebelum handler)
        ModEffects.registerModEffects();

        // PENTING: Register items SEBELUM item groups
        // Ini akan initialize materials dan register semua items
        ModItems.registerModItems();

        // Register item groups SETELAH items
        ModItemGroups.registerItemGroups();

        // Register armor effects handler
        ArmorEffectsHandler.register();
        LOGGER.info("✓ Registered Emerald Armor Effects Handler");

        // Register horse armor effects handler
        HorseArmorEffectsHandler.register();
        LOGGER.info("✓ Registered Emerald Horse Armor Effects Handler");

        // Register tool effects handler
        ToolEffectsHandler.register();
        LOGGER.info("✓ Registered Emerald Tool Effects Handler");

        // Register damage prevention handler
        DamagePreventionHandler.register();
        LOGGER.info("✓ Registered Fire & Piglin Damage Prevention Handler");

        // Register auto-smelt handler for pickaxe
        AutoSmeltHandler.register();
        LOGGER.info("✓ Registered Auto-Smelt Handler for Emerald Pickaxe");

        // Register tree chopping handler for axe
        TreeChoppingHandler.register();
        LOGGER.info("✓ Registered Tree Chopping Handler for Emerald Axe");

        // Register auto-replant handler for hoe
        AutoReplantHandler.register();
        LOGGER.info("✓ Registered Auto-Replant Handler for Emerald Hoe");

        // Register shockwave handler for sword
        SwordShockwaveHandler.register();
        LOGGER.info("✓ Registered Shockwave Handler for Emerald Sword");

        // Register anti-gravity handler for shovel
        AntiGravityHandler.register();
        LOGGER.info("✓ Registered Anti-Gravity Handler for Emerald Shovel");

        // Register server tick event untuk Anti-Gravity handler
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            AntiGravityHandler.tick(world);
        });
        LOGGER.info("✓ Registered Server Tick Event for Anti-Gravity");

        LOGGER.info("========================================");
        LOGGER.info("Emerald Tools & Armor Mod initialized!");
        LOGGER.info("All Protection Features Active:");
        LOGGER.info("  - Water Breathing (Helmet)");
        LOGGER.info("  - Dolphin's Grace (Chestplate)");
        LOGGER.info("  - Fire Immunity (All Armor)");
        LOGGER.info("  - Snow Powder Walker (Boots) - CUSTOM ICON");
        LOGGER.info("  - Piglin Neutral (All Armor) - MEMORY CONTROLLED");
        LOGGER.info("  - ENCHANTABILITY: 20 (Same as Diamond)");
        LOGGER.info("");
        LOGGER.info("All Tool Features Active:");
        LOGGER.info("  - Shockwave Strike (Sword) - EVERY 3RD HIT + ICON");
        LOGGER.info("  - Auto-Smelt Ores (Pickaxe) - WITH FORTUNE + ICON");
        LOGGER.info("  - Tree Chopping (Axe) - ONE CHOP ALL LOGS + ICON");
        LOGGER.info("  - Anti-Gravity (Shovel) - PREVENT FALLING BLOCKS + ICON");
        LOGGER.info("  - Auto-Replant (Hoe) - RIGHT CLICK CROPS + ICON");
        LOGGER.info("");
        LOGGER.info("Horse Armor Features Active:");
        LOGGER.info("  - Speed Boost (30% faster movement)");
        LOGGER.info("  - Regeneration (Health regen)");
        LOGGER.info("  - Fire Resistance (Immune to fire)");
        LOGGER.info("  - Jump Boost (Higher jumps)");
        LOGGER.info("  - Resistance (Reduced damage)");
        LOGGER.info("========================================");
    }

    private void debugArmorModels() {
        LOGGER.info("--- Armor Model JSON Debug ---");

        String[] armorModels = {
                "assets/emeraldmod/models/item/emerald_helmet.json",
                "assets/emeraldmod/models/item/emerald_chestplate.json",
                "assets/emeraldmod/models/item/emerald_leggings.json",
                "assets/emeraldmod/models/item/emerald_boots.json"
        };

        for (String path : armorModels) {
            testResource(path);
        }

        LOGGER.info("--- End Armor Model Debug ---");
    }

    private void debugTextures() {
        LOGGER.info("--- Armor Texture Debug ---");

        String[] armorTextures = {
                "assets/emeraldmod/textures/item/emerald_helmet.png",
                "assets/emeraldmod/textures/item/emerald_chestplate.png",
                "assets/emeraldmod/textures/item/emerald_leggings.png",
                "assets/emeraldmod/textures/item/emerald_boots.png"
        };

        for (String path : armorTextures) {
            testResource(path);
        }

        LOGGER.info("--- End Armor Texture Debug ---");
    }

    private void debugResourceLoading() {
        LOGGER.info("--- Resource Loading Debug ---");

        // Test equipment asset
        String equipmentPath = "assets/emeraldmod/equipment/emerald.json";
        testResource(equipmentPath);

        try {
            LOGGER.info("Listing all 'emeraldmod' resources...");
            ClassLoader classLoader = getClass().getClassLoader();
            Enumeration<URL> resources = classLoader.getResources("assets/emeraldmod/");

            int count = 0;
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                LOGGER.info("Found resource directory: " + url);
                count++;
            }

            if (count == 0) {
                LOGGER.error("!!! NO EMERALDMOD RESOURCES FOUND !!!");
                LOGGER.error("This means the assets folder is not being loaded!");
            } else {
                LOGGER.info("Total resource directories found: " + count);
            }
        } catch (IOException e) {
            LOGGER.error("Error listing resources", e);
        }

        LOGGER.info("--- End Resource Debug ---");
    }

    private void testResource(String path) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream stream = classLoader.getResourceAsStream(path);

            if (stream != null) {
                int size = stream.available();
                LOGGER.info("✓ FOUND: " + path + " (Size: " + size + " bytes)");
                stream.close();
            } else {
                LOGGER.error("✗ NOT FOUND: " + path);
                LOGGER.error("  -> Make sure this file exists in src/main/resources/" + path);
            }
        } catch (Exception e) {
            LOGGER.error("✗ ERROR checking " + path + ": " + e.getMessage());
        }
    }
}