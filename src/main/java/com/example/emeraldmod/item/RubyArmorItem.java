package com.example.emeraldmod.item;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.Entity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.world.World;

public class RubyArmorItem extends ArmorItem {
    private final EquipmentType equipmentType;

    public RubyArmorItem(ArmorMaterial material, EquipmentType type, Settings settings) {
        super(material, type, settings);
        this.equipmentType = type;

        EmeraldMod.LOGGER.info("Creating RubyArmorItem: " + type.getName());
        EmeraldMod.LOGGER.info("  - Equipment Type: " + type.getName());
        EmeraldMod.LOGGER.info("  - UNBREAKABLE - Will not take damage");
    }

    public EquipmentType getEquipmentType() {
        return this.equipmentType;
    }

    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        // Reset damage ke 0 setiap tick untuk memastikan durability tidak berkurang
        if (stack.getDamage() > 0) {
            stack.setDamage(0);
        }
    }
}