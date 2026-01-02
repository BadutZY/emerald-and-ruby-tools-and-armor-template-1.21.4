package com.example.emeraldmod.item;

import net.minecraft.entity.Entity;
import net.minecraft.item.*;
import net.minecraft.world.World;

public class RubyToolItem {

    // Ruby Sword - Unbreakable
    public static class RubySwordItem extends SwordItem {
        public RubySwordItem(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
            super(material, attackDamage, attackSpeed, settings);
        }

        public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
            super.inventoryTick(stack, world, entity, slot, selected);

            // Reset damage ke 0 setiap tick untuk memastikan durability tidak berkurang
            if (stack.getDamage() > 0) {
                stack.setDamage(0);
            }
        }
    }

    // Ruby Pickaxe - Unbreakable
    public static class RubyPickaxeItem extends PickaxeItem {
        public RubyPickaxeItem(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
            super(material, attackDamage, attackSpeed, settings);
        }

        public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
            super.inventoryTick(stack, world, entity, slot, selected);

            // Reset damage ke 0 setiap tick untuk memastikan durability tidak berkurang
            if (stack.getDamage() > 0) {
                stack.setDamage(0);
            }
        }
    }

    // Ruby Axe - Unbreakable
    public static class RubyAxeItem extends AxeItem {
        public RubyAxeItem(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
            super(material, attackDamage, attackSpeed, settings);
        }

        public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
            super.inventoryTick(stack, world, entity, slot, selected);

            // Reset damage ke 0 setiap tick untuk memastikan durability tidak berkurang
            if (stack.getDamage() > 0) {
                stack.setDamage(0);
            }
        }
    }

    // Ruby Shovel - Unbreakable
    public static class RubyShovelItem extends ShovelItem {
        public RubyShovelItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
            super(material, attackDamage, attackSpeed, settings);
        }

        public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
            super.inventoryTick(stack, world, entity, slot, selected);

            // Reset damage ke 0 setiap tick untuk memastikan durability tidak berkurang
            if (stack.getDamage() > 0) {
                stack.setDamage(0);
            }
        }
    }

    // Ruby Hoe - Unbreakable
    public static class RubyHoeItem extends HoeItem {
        public RubyHoeItem(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
            super(material, attackDamage, attackSpeed, settings);
        }

        public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
            super.inventoryTick(stack, world, entity, slot, selected);

            // Reset damage ke 0 setiap tick untuk memastikan durability tidak berkurang
            if (stack.getDamage() > 0) {
                stack.setDamage(0);
            }
        }
    }
}