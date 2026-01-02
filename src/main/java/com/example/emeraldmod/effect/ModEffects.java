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

    // Status Effect untuk Silent Step (Leggings)
    public static final StatusEffect SILENT_STEP = registerEffect("silent_step",
            new SilentStepEffect());
    public static RegistryEntry<StatusEffect> SILENT_STEP_ENTRY;

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

    // ===== HORSE ARMOR EFFECTS =====
    // Status Effect untuk Swimming Horse
    public static final StatusEffect SWIMMING_HORSE = registerEffect("swimming_horse",
            new SwimmingHorseEffect());
    public static RegistryEntry<StatusEffect> SWIMMING_HORSE_ENTRY;

    public static final StatusEffect HORSE_FIRE = registerEffect("horse_fire",
            new HorseFireEffect());
    public static RegistryEntry<StatusEffect> HORSE_FIRE_ENTRY;

    public static final StatusEffect HORSE_LAVA = registerEffect("horse_lava",
            new HorseLavaEffect());
    public static RegistryEntry<StatusEffect> HORSE_LAVA_ENTRY;

    public static final StatusEffect HORSE_SNOW = registerEffect("horse_snow",
            new HorseSnowEffect());
    public static RegistryEntry<StatusEffect> HORSE_SNOW_ENTRY;

    public static StatusEffect INFINITE_DURABILITY = registerEffect("infinite_durability",
            new NegativeImmunHorseEffect());
    public static RegistryEntry<StatusEffect> INFINITE_DURABILITY_ENTRY;

    public static StatusEffect NEGATIVE_IMMUNITY = registerEffect("negative_immun",
            new NegativeImmunityEffect());
    public static RegistryEntry<StatusEffect> NEGATIVE_IMMUNITY_ENTRY;

    public static StatusEffect NEGATIVE_IMMUN_HORSE = registerEffect("negative_horse",
            new NegativeImmunHorseEffect());
    public static RegistryEntry<StatusEffect> NEGATIVE_IMMUN_HORSE_ENTRY;

    public static StatusEffect VEIN_MINING = registerEffect("vein_mining",
            new VeinMiningEffect());
    public static RegistryEntry<StatusEffect> VEIN_MINING_ENTRY;

    public static StatusEffect LIGHTING_SLASH = registerEffect("lightning_slash",
            new LightningSlashEffect());
    public static RegistryEntry<StatusEffect> LIGHTING_SLASH_ENTRY;

    public static StatusEffect AUTO_PLACE = registerEffect("auto_place",
            new AutoPlaceEffect());
    public static RegistryEntry<StatusEffect> AUTO_PLACE_ENTRY;

    public static StatusEffect FAST_DIGGING = registerEffect("fast_digging",
            new FastDiggingEffect());
    public static RegistryEntry<StatusEffect> FAST_DIGGING_ENTRY;

    public static StatusEffect MORE_HARVEST = registerEffect("more_harvest",
            new MoreHarvestEffect());
    public static RegistryEntry<StatusEffect> MORE_HARVEST_ENTRY;

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
        SILENT_STEP_ENTRY = Registries.STATUS_EFFECT.getEntry(SILENT_STEP);
        SHOCKWAVE_ENTRY = Registries.STATUS_EFFECT.getEntry(SHOCKWAVE);
        AUTO_SMELT_ENTRY = Registries.STATUS_EFFECT.getEntry(AUTO_SMELT);
        TREE_CHOPPING_ENTRY = Registries.STATUS_EFFECT.getEntry(TREE_CHOPPING);
        ANTI_GRAVITY_ENTRY = Registries.STATUS_EFFECT.getEntry(ANTI_GRAVITY);
        AUTO_REPLANT_ENTRY = Registries.STATUS_EFFECT.getEntry(AUTO_REPLANT);
        SWIMMING_HORSE_ENTRY = Registries.STATUS_EFFECT.getEntry(SWIMMING_HORSE);
        HORSE_FIRE_ENTRY = Registries.STATUS_EFFECT.getEntry(HORSE_FIRE);
        HORSE_LAVA_ENTRY = Registries.STATUS_EFFECT.getEntry(HORSE_LAVA);
        HORSE_SNOW_ENTRY = Registries.STATUS_EFFECT.getEntry(HORSE_SNOW);
        NEGATIVE_IMMUNITY_ENTRY = Registries.STATUS_EFFECT.getEntry(NEGATIVE_IMMUNITY);
        NEGATIVE_IMMUN_HORSE_ENTRY = Registries.STATUS_EFFECT.getEntry(NEGATIVE_IMMUN_HORSE);
        VEIN_MINING_ENTRY = Registries.STATUS_EFFECT.getEntry(VEIN_MINING);
        LIGHTING_SLASH_ENTRY = Registries.STATUS_EFFECT.getEntry(LIGHTING_SLASH);
        AUTO_PLACE_ENTRY = Registries.STATUS_EFFECT.getEntry(AUTO_PLACE);
        FAST_DIGGING_ENTRY = Registries.STATUS_EFFECT.getEntry(FAST_DIGGING);
        MORE_HARVEST_ENTRY = Registries.STATUS_EFFECT.getEntry(MORE_HARVEST);

        EmeraldMod.LOGGER.info("âœ“ Registered Armor Effects:");
        EmeraldMod.LOGGER.info("  - Snow Powder Walker (Boots)");
        EmeraldMod.LOGGER.info("  - Silent Step (Leggings)");
        EmeraldMod.LOGGER.info("  - Custom Fire Resistance (Hidden Icon)");

        EmeraldMod.LOGGER.info("âœ“ Registered Tool Effects:");
        EmeraldMod.LOGGER.info("  - Shockwave (Sword)");
        EmeraldMod.LOGGER.info("  - Auto Smelt (Pickaxe)");
        EmeraldMod.LOGGER.info("  - Tree Chopping (Axe)");
        EmeraldMod.LOGGER.info("  - Anti-Gravity (Shovel)");
        EmeraldMod.LOGGER.info("  - Auto Replant (Hoe)");

        EmeraldMod.LOGGER.info("âœ“ Registered Horse Armor Effects:");
        EmeraldMod.LOGGER.info("  - Swimming Horse");
        EmeraldMod.LOGGER.info("========================================");

        // Log texture paths untuk debugging
        EmeraldMod.LOGGER.info("Expected texture paths:");
        EmeraldMod.LOGGER.info("assets/emeraldmod/textures/mob_effect/snow_powder_walker.png");
        EmeraldMod.LOGGER.info("assets/emeraldmod/textures/mob_effect/silent_step.png");
        EmeraldMod.LOGGER.info("assets/emeraldmod/textures/mob_effect/shockwave.png");
        EmeraldMod.LOGGER.info("assets/emeraldmod/textures/mob_effect/auto_smelt.png");
        EmeraldMod.LOGGER.info("assets/emeraldmod/textures/mob_effect/tree_chopping.png");
        EmeraldMod.LOGGER.info("assets/emeraldmod/textures/mob_effect/anti_gravity.png");
        EmeraldMod.LOGGER.info("assets/emeraldmod/textures/mob_effect/auto_replant.png");
        EmeraldMod.LOGGER.info("assets/emeraldmod/textures/mob_effect/swimming_horse.png");
    }
}