package com.example.emeraldmod.item;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.item.Item;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.Map;

public class ModArmorMaterial {
    public static final int BASE_DURABILITY = 40;

    public static final TagKey<Item> EMERALD_REPAIR_INGREDIENT = TagKey.of(
            RegistryKeys.ITEM,
            Identifier.of(EmeraldMod.MOD_ID, "emerald_repair_ingredient")
    );

    public static final RegistryKey<EquipmentAsset> EMERALD_EQUIPMENT_ASSET = RegistryKey.of(
            EquipmentAssetKeys.REGISTRY_KEY,
            Identifier.of(EmeraldMod.MOD_ID, "emerald")
    );

    // ArmorMaterial constructor parameters di 1.21.4:
    // 1. durability multiplier (int)
    // 2. protection values (Map<EquipmentType, Integer>)
    // 3. enchantability (int)
    // 4. equip sound (RegistryEntry<SoundEvent>)
    // 5. toughness (float)
    // 6. knockback resistance (float)
    // 7. repair ingredient tag (TagKey<Item>)
    // 8. equipment asset (RegistryKey<EquipmentAsset>)
    public static final ArmorMaterial EMERALD_ARMOR_MATERIAL = new ArmorMaterial(
            BASE_DURABILITY,
            Map.of(
                    EquipmentType.BOOTS, 5,
                    EquipmentType.LEGGINGS, 8,
                    EquipmentType.CHESTPLATE, 10,
                    EquipmentType.HELMET, 5,
                    EquipmentType.BODY, 22
            ),
            10, // Enchantability (Diamond = 10)
            SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,
            5.0F, // Toughness (Netherite level)
            0.3F, // Knockback resistance (higher than diamond)
            EMERALD_REPAIR_INGREDIENT,
            EMERALD_EQUIPMENT_ASSET
    );

    public static void initialize() {
        EmeraldMod.LOGGER.info("Initializing Emerald Armor Materials");
        EmeraldMod.LOGGER.info("  - Base Durability: " + BASE_DURABILITY);
        EmeraldMod.LOGGER.info("  - Enchantability: 10 (Diamond tier)");
        EmeraldMod.LOGGER.info("  - Toughness: 5.0 (Netherite level)");
        EmeraldMod.LOGGER.info("  - Knockback Resistance: 0.3");
    }
}