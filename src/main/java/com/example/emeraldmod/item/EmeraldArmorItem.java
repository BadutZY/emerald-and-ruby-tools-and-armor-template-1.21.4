package com.example.emeraldmod.item;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;

public class EmeraldArmorItem extends ArmorItem {
    private final EquipmentType equipmentType;

    public EmeraldArmorItem(ArmorMaterial material, EquipmentType type, Settings settings) {
        super(material, type, settings);
        this.equipmentType = type;
    }

    public EquipmentType getEquipmentType() {
        return this.equipmentType;
    }
}