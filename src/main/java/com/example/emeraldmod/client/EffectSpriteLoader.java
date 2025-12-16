package com.example.emeraldmod.client;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.util.Identifier;

import java.io.InputStream;

public class EffectSpriteLoader {

    public static void register() {
        EmeraldMod.LOGGER.info("========================================");
        EmeraldMod.LOGGER.info("Registering Effect Sprite Loader");
        EmeraldMod.LOGGER.info("========================================");

        // Test semua texture paths
        testEffectTexture("snow_powder_walker", "Boots Armor Effect");
        testEffectTexture("shockwave", "Sword Tool Effect");
        testEffectTexture("auto_smelt", "Pickaxe Tool Effect");
        testEffectTexture("tree_chopping", "Axe Tool Effect");
        testEffectTexture("anti_gravity", "Shovel Tool Effect");
        testEffectTexture("auto_replant", "Hoe Tool Effect");

        EmeraldMod.LOGGER.info("========================================");
    }

    private static void testEffectTexture(String effectName, String description) {
        Identifier textureId = Identifier.of(EmeraldMod.MOD_ID, "mob_effect/" + effectName);
        String resourcePath = "assets/" + EmeraldMod.MOD_ID + "/textures/mob_effect/" + effectName + ".png";

        EmeraldMod.LOGGER.info("Testing: " + description);
        EmeraldMod.LOGGER.info("  Texture ID: " + textureId);
        EmeraldMod.LOGGER.info("  Full path: " + resourcePath);

        try {
            InputStream stream = EffectSpriteLoader.class.getClassLoader().getResourceAsStream(resourcePath);
            if (stream != null) {
                int size = stream.available();
                EmeraldMod.LOGGER.info("  ✓ FOUND (Size: " + size + " bytes)");
                stream.close();
            } else {
                EmeraldMod.LOGGER.error("  ✗ NOT FOUND");
                EmeraldMod.LOGGER.error("  -> Create file at: src/main/resources/" + resourcePath);
            }
        } catch (Exception e) {
            EmeraldMod.LOGGER.error("  ✗ Error: " + e.getMessage());
        }
    }
}