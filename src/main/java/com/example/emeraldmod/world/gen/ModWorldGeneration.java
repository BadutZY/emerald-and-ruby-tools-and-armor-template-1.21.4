package com.example.emeraldmod.world.gen;

import com.example.emeraldmod.EmeraldMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

public class ModWorldGeneration {

    // Ruby Ore Placed Features (Overworld)
    public static final RegistryKey<PlacedFeature> RUBY_ORE_PLACED_KEY = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE,
            Identifier.of(EmeraldMod.MOD_ID, "ruby_ore")
    );

    public static final RegistryKey<PlacedFeature> RUBY_ORE_LARGE_PLACED_KEY = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE,
            Identifier.of(EmeraldMod.MOD_ID, "ruby_ore_large")
    );

    // Nether Ruby Ore Placed Feature (NEW!)
    public static final RegistryKey<PlacedFeature> NETHER_RUBY_ORE_PLACED_KEY = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE,
            Identifier.of(EmeraldMod.MOD_ID, "nether_ruby_ore")
    );

    public static final RegistryKey<PlacedFeature> RUBY_DEBRIS_PLACED_KEY = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE,
            Identifier.of(EmeraldMod.MOD_ID, "ruby_debris")
    );

    public static void generateModWorldGen() {
        EmeraldMod.LOGGER.info("Registering Ruby Ore World Generation");

        // Add Ruby Ore to Overworld (semua biome)
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                RUBY_ORE_PLACED_KEY
        );

        // Add Large Ruby Ore veins (lebih jarang tapi lebih besar)
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                RUBY_ORE_LARGE_PLACED_KEY
        );

        // Add Nether Ruby Ore to Nether (NEW!)
        BiomeModifications.addFeature(
                BiomeSelectors.foundInTheNether(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                NETHER_RUBY_ORE_PLACED_KEY
        );

        // Add Nether Ruby Ore to Nether (NEW!)
        BiomeModifications.addFeature(
                BiomeSelectors.foundInTheNether(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                RUBY_DEBRIS_PLACED_KEY
        );

        EmeraldMod.LOGGER.info("✓ Ruby Ore will generate in all Overworld biomes");
        EmeraldMod.LOGGER.info("✓ Generation: Y -64 to Y 64 (like Diamond)");
        EmeraldMod.LOGGER.info("✓ Vein Size: 4-12 ores per vein (more than Diamond)");
        EmeraldMod.LOGGER.info("✓ Rarity: Similar to Diamond but larger veins");

        EmeraldMod.LOGGER.info("✓ Nether Ruby Ore will generate in all Nether biomes");
        EmeraldMod.LOGGER.info("✓ Nether Generation: Y 10 to Y 117");
        EmeraldMod.LOGGER.info("✓ Nether Vein Size: 2-4 ores per vein");
        EmeraldMod.LOGGER.info("✓ Nether Rarity: Similar to Nether Gold Ore");
        EmeraldMod.LOGGER.info("✓ Drops: 2-4 Ruby Scrap per ore");
    }
}