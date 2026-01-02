package com.example.emeraldmod.item;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class RubyToolMaterial {

    // Tag untuk repair ingredient
    public static final TagKey<Item> RUBY_REPAIR_INGREDIENT = TagKey.of(
            RegistryKeys.ITEM,
            Identifier.of(EmeraldMod.MOD_ID, "ruby_repair_ingredient")
    );

    // Ruby Tool Material - UNBREAKABLE (handled in item class)
    // Set durability tinggi tapi reasonable, actual unbreakable logic di item class
    // ToolMaterial constructor parameters di 1.21.4:
    // 1. incorrect blocks tag (TagKey<Block>)
    // 2. durability (int) - Set cukup tinggi, tapi override di item class
    // 3. mining speed (float)
    // 4. attack damage bonus (float)
    // 5. enchantability (int)
    // 6. repair ingredient tag (TagKey<Item>)
    public static final ToolMaterial RUBY = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            10000,              // Durability tinggi tapi tidak extreme, override di item class
            12.0f,              // Mining speed (lebih cepat dari Emerald 9.0)
            6.0f,               // Attack damage bonus (lebih kuat dari Emerald 4.0)
            15,                 // Enchantability (lebih baik dari Diamond/Emerald 10)
            RUBY_REPAIR_INGREDIENT
    );

    public static void initialize() {
        EmeraldMod.LOGGER.info("Initializing Ruby Tool Material");
        EmeraldMod.LOGGER.info("  - Durability: UNBREAKABLE (via item class override)");
        EmeraldMod.LOGGER.info("  - Mining Speed: 12.0 (Superior to all tiers)");
        EmeraldMod.LOGGER.info("  - Attack Damage: 6.0 (Superior to all tiers)");
        EmeraldMod.LOGGER.info("  - Enchantability: 15 (Superior enchanting)");
    }
}