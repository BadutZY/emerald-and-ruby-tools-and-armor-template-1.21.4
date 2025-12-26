package com.example.emeraldmod.item;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;

public class EmeraldArmorItem extends ArmorItem {
    private final EquipmentType equipmentType;

    public EmeraldArmorItem(ArmorMaterial material, EquipmentType type, Settings settings) {
        super(material, type, settings);
        this.equipmentType = type;

        // DEBUG: Log creation
        EmeraldMod.LOGGER.info("Creating EmeraldArmorItem: " + type.getName());
        EmeraldMod.LOGGER.info("  - Equipment Type: " + type.getName());
        EmeraldMod.LOGGER.info("  - Expected Max Damage: " + type.getMaxDamage(40));
    }

    public EquipmentType getEquipmentType() {
        return this.equipmentType;
    }

    // Di 1.21.4, enchantability dihandle oleh material dan Item class
    // Tidak perlu override apa-apa, cukup pastikan material memiliki enchantability yang benar
}