package com.example.emeraldmod.effect;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class ModEffects {

    // ===== ARMOR EFFECTS =====
    // Status Effect untuk Snow Powder Walker (Boots)
    public static final StatusEffect SNOW_POWDER_WALKER = registerEffect("snow_powder_walker",
            new SnowPowderWalkerEffect());
    public static RegistryEntry<StatusEffect> SNOW_POWDER_WALKER_ENTRY;

    // ===== TOOL EFFECTS =====
    // Status Effect untuk Shockwave (Sword)
    public static final StatusEffect SHOCKWAVE = registerEffect("shockwave",
            new ShockwaveEffect());
    public static RegistryEntry<StatusEffect> SHOCKWAVE_ENTRY;

    // Status Effect untuk Auto Smelt (Pickaxe)
    public static final StatusEffect AUTO_SMELT = registerEffect("auto_smelt",
            new AutoSmeltEffect());
    public static RegistryEntry<StatusEffect> AUTO_SMELT_ENTRY;

    // Status Effect untuk Tree Chopping (Axe)
    public static final StatusEffect TREE_CHOPPING = registerEffect("tree_chopping",
            new TreeChoppingEffect());
    public static RegistryEntry<StatusEffect> TREE_CHOPPING_ENTRY;

    // Status Effect untuk Anti-Gravity (Shovel)
    public static final StatusEffect ANTI_GRAVITY = registerEffect("anti_gravity",
            new AntiGravityEffect());
    public static RegistryEntry<StatusEffect> ANTI_GRAVITY_ENTRY;

    // Status Effect untuk Auto Replant (Hoe)
    public static final StatusEffect AUTO_REPLANT = registerEffect("auto_replant",
            new AutoReplantEffect());
    public static RegistryEntry<StatusEffect> AUTO_REPLANT_ENTRY;

    private static StatusEffect registerEffect(String name, StatusEffect effect) {
        Identifier id = Identifier.of(EmeraldMod.MOD_ID, name);
        EmeraldMod.LOGGER.info("Registering status effect: " + id);
        return Registry.register(Registries.STATUS_EFFECT, id, effect);
    }

    public static void registerModEffects() {
        EmeraldMod.LOGGER.info("========================================");
        EmeraldMod.LOGGER.info("Registering Custom Status Effects");
        EmeraldMod.LOGGER.info("========================================");

        // Get registry entries setelah effect di-register
        SNOW_POWDER_WALKER_ENTRY = Registries.STATUS_EFFECT.getEntry(SNOW_POWDER_WALKER);
        SHOCKWAVE_ENTRY = Registries.STATUS_EFFECT.getEntry(SHOCKWAVE);
        AUTO_SMELT_ENTRY = Registries.STATUS_EFFECT.getEntry(AUTO_SMELT);
        TREE_CHOPPING_ENTRY = Registries.STATUS_EFFECT.getEntry(TREE_CHOPPING);
        ANTI_GRAVITY_ENTRY = Registries.STATUS_EFFECT.getEntry(ANTI_GRAVITY);
        AUTO_REPLANT_ENTRY = Registries.STATUS_EFFECT.getEntry(AUTO_REPLANT);

        EmeraldMod.LOGGER.info("✓ Registered Armor Effects:");
        EmeraldMod.LOGGER.info("  - Snow Powder Walker (Boots)");
        EmeraldMod.LOGGER.info("  - Custom Fire Resistance (Hidden Icon)");

        EmeraldMod.LOGGER.info("✓ Registered Tool Effects:");
        EmeraldMod.LOGGER.info("  - Shockwave (Sword)");
        EmeraldMod.LOGGER.info("  - Auto Smelt (Pickaxe)");
        EmeraldMod.LOGGER.info("  - Tree Chopping (Axe)");
        EmeraldMod.LOGGER.info("  - Anti-Gravity (Shovel)");
        EmeraldMod.LOGGER.info("  - Auto Replant (Hoe)");
        EmeraldMod.LOGGER.info("========================================");

        // Log texture paths untuk debugging
        EmeraldMod.LOGGER.info("Expected texture paths:");
        EmeraldMod.LOGGER.info("assets/emeraldmod/textures/mob_effect/snow_powder_walker.png");
        EmeraldMod.LOGGER.info("assets/emeraldmod/textures/mob_effect/shockwave.png");
        EmeraldMod.LOGGER.info("assets/emeraldmod/textures/mob_effect/auto_smelt.png");
        EmeraldMod.LOGGER.info("assets/emeraldmod/textures/mob_effect/tree_chopping.png");
        EmeraldMod.LOGGER.info("assets/emeraldmod/textures/mob_effect/anti_gravity.png");
        EmeraldMod.LOGGER.info("assets/emeraldmod/textures/mob_effect/auto_replant.png");
    }
}