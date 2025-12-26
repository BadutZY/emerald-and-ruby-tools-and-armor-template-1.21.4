package com.example.emeraldmod;

import com.example.emeraldmod.effect.ModEffects;
import com.example.emeraldmod.event.*;
import com.example.emeraldmod.item.ModItemGroups;
import com.example.emeraldmod.item.ModItems;
import com.example.emeraldmod.network.ServerPacketHandler;
import com.example.emeraldmod.network.ToggleEffectPacket;
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

        // Register network packets FIRST
        ToggleEffectPacket.register();
        ServerPacketHandler.register();
        LOGGER.info("✓ Registered Network Packets for Toggle System");

        // Register custom effects FIRST (sebelum handler)
        ModEffects.registerModEffects();

        // PENTING: Register items SEBELUM item groups
        // Ini akan initialize materials dan register semua items
        ModItems.registerModItems();

        // Register item groups SETELAH items
        ModItemGroups.registerItemGroups();

        // Register armor effects handler (with toggle support)
        ArmorEffectsHandler.register();
        LOGGER.info("✓ Registered Emerald Armor Effects Handler (Toggle: ON/OFF)");

        // Register horse armor effects handler (always active)
        HorseArmorEffectsHandler.register();
        LOGGER.info("✓ Registered Emerald Horse Armor Effects Handler (Always Active)");

        // Register tool effects handler (with toggle support)
        ToolEffectsHandler.register();
        LOGGER.info("✓ Registered Emerald Tool Effects Handler (Toggle: ON/OFF)");

        // Register damage prevention handler (with toggle check)
        DamagePreventionHandler.register();
        LOGGER.info("✓ Registered Fire Damage Prevention Handler (Toggleable)");

        // Register auto-smelt handler for pickaxe (with toggle check)
        AutoSmeltHandler.register();
        LOGGER.info("✓ Registered Auto-Smelt Handler for Emerald Pickaxe (Toggleable)");

        // Register tree chopping handler for axe (with toggle check)
        TreeChoppingHandler.register();
        LOGGER.info("✓ Registered Tree Chopping Handler for Emerald Axe (Toggleable)");

        // Register auto-replant handler for hoe (with toggle check)
        AutoReplantHandler.register();
        LOGGER.info("✓ Registered Auto-Replant Handler for Emerald Hoe (Toggleable)");

        // Register shockwave handler for sword (with toggle check)
        SwordShockwaveHandler.register();
        LOGGER.info("✓ Registered Shockwave Handler for Emerald Sword (Toggleable)");

        // Register anti-gravity handler for shovel (with toggle check)
        AntiGravityHandler.register();
        LOGGER.info("✓ Registered Anti-Gravity Handler for Emerald Shovel (Toggleable)");

        // FIXED: Register ONLY PowderSnowHandler (yang sudah include sink logic)
        PowderSnowHandler.register();
        LOGGER.info("✓ Registered Powder Snow Handler (Walk on top when ON, Sink when OFF)");

        // Register server tick event untuk Anti-Gravity handler
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            AntiGravityHandler.tick(world);
        });
        LOGGER.info("✓ Registered Server Tick Event for Anti-Gravity");

        LOGGER.info("========================================");
        LOGGER.info("Emerald Tools & Armor Mod initialized!");
        LOGGER.info("");
        LOGGER.info("🎮 KEYBIND CONTROLS:");
        LOGGER.info("  - Toggle Tools Effect: V (default)");
        LOGGER.info("  - Toggle Armor Effect: B (default)");
        LOGGER.info("  - Customize in: Options → Controls → Emerald Mod");
        LOGGER.info("");
        LOGGER.info("All Protection Features Active:");
        LOGGER.info("  - Water Breathing (Helmet)");
        LOGGER.info("  - Dolphin's Grace (Chestplate)");
        LOGGER.info("  - Fire Immunity (All Armor)");
        LOGGER.info("  - Snow Powder Walker (Boots) - TOGGLEABLE");
        LOGGER.info("  - Piglin Neutral (All Armor) - MEMORY CONTROLLED");
        LOGGER.info("  - Silent Step (Leggings) - STEALTH MODE");
        LOGGER.info("  - ENCHANTABILITY: 10 (Same as Diamond)");
        LOGGER.info("  - ENCHANTMENT SUPPORT: ENABLED");
        LOGGER.info("");
        LOGGER.info("All Tool Features Active:");
        LOGGER.info("  - Shockwave Strike (Sword) - EVERY 3RD HIT + ICON");
        LOGGER.info("  - Auto-Smelt Ores (Pickaxe) - WITH FORTUNE + ICON");
        LOGGER.info("  - Tree Chopping (Axe) - ONE CHOP ALL LOGS + ICON");
        LOGGER.info("  - Anti-Gravity (Shovel) - PREVENT FALLING BLOCKS + ICON");
        LOGGER.info("  - Auto-Replant (Hoe) - RIGHT CLICK CROPS + ICON");
        LOGGER.info("");
        LOGGER.info("Horse Armor Features Active (Always ON):");
        LOGGER.info("  - Speed Boost (30% faster movement)");
        LOGGER.info("  - Regeneration (Health regen)");
        LOGGER.info("  - Fire Resistance (Immune to fire)");
        LOGGER.info("  - Lava Swimming (Can swim in lava)");
        LOGGER.info("  - Water Swimming (Can swim in water)");
        LOGGER.info("  - Jump Boost (Higher jumps)");
        LOGGER.info("  - Resistance (Reduced damage)");
        LOGGER.info("  - Powder Snow Walker (Walk on powder snow)");
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