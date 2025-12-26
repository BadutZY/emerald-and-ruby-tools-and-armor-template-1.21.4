package com.example.emeraldmod.item;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

public class ModToolMaterial {

    // Tag untuk repair ingredient (gunakan tag yang sama dengan armor)
    public static final TagKey<Item> EMERALD_REPAIR_INGREDIENT = TagKey.of(
            RegistryKeys.ITEM,
            Identifier.of(EmeraldMod.MOD_ID, "emerald_repair_ingredient")
    );

    // ToolMaterial constructor parameters di 1.21.4:
    // 1. incorrect blocks tag (TagKey<Block>)
    // 2. durability (int)
    // 3. mining speed (float)
    // 4. attack damage bonus (float)
    // 5. enchantability (int)
    // 6. repair ingredient tag (TagKey<Item>)
    public static final ToolMaterial EMERALD = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            2031,      // Durability (Diamond = 1561, Netherite = 2031)
            9.0f,      // Mining speed (Diamond = 8.0, Netherite = 9.0)
            4.0f,      // Attack damage bonus (Diamond = 3.0, Netherite = 4.0)
            10,        // Enchantability (Diamond = 10, Gold = 22)
            EMERALD_REPAIR_INGREDIENT
    );

    public static void initialize() {
        EmeraldMod.LOGGER.info("Initializing Emerald Tool Material");
        EmeraldMod.LOGGER.info("  - Durability: 2031 (Netherite tier)");
        EmeraldMod.LOGGER.info("  - Mining Speed: 9.0 (Netherite tier)");
        EmeraldMod.LOGGER.info("  - Attack Damage: 4.0 (Netherite tier)");
        EmeraldMod.LOGGER.info("  - Enchantability: 10 (Diamond tier)");
    }
}