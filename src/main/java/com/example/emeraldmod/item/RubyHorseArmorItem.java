package com.example.emeraldmod.item;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.Entity;
import net.minecraft.item.AnimalArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.world.World;

public class RubyHorseArmorItem extends AnimalArmorItem {

    public RubyHorseArmorItem(ArmorMaterial material, Type animalType, Settings settings) {
        super(material, animalType, settings);

        EmeraldMod.LOGGER.info("Creating RubyHorseArmorItem");
        EmeraldMod.LOGGER.info("  - UNBREAKABLE - Will not take damage");
    }

    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        // Reset damage ke 0 setiap tick untuk memastikan durability tidak berkurang
        if (stack.getDamage() > 0) {
            stack.setDamage(0);
        }
    }
}