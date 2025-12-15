package com.example.emeraldmod.item;

import com.example.emeraldmod.EmeraldMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {

    // ============================================
    // EMERALD TOOLS & ARMOR TAB
    // ============================================
    public static final ItemGroup EMERALD_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(EmeraldMod.MOD_ID, "emerald_group"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModItems.EMERALD_SWORD))
                    .displayName(Text.translatable("itemgroup.emeraldmod.emerald"))
                    .entries((displayContext, entries) -> {
                        // Emerald Upgrade Template
                        entries.add(ModItems.EMERALD_UPGRADE_SMITHING_TEMPLATE);

                        // Emerald Tools
                        entries.add(ModItems.EMERALD_SWORD);
                        entries.add(ModItems.EMERALD_PICKAXE);
                        entries.add(ModItems.EMERALD_AXE);
                        entries.add(ModItems.EMERALD_SHOVEL);
                        entries.add(ModItems.EMERALD_HOE);

                        // Emerald Armor
                        entries.add(ModItems.EMERALD_HELMET);
                        entries.add(ModItems.EMERALD_CHESTPLATE);
                        entries.add(ModItems.EMERALD_LEGGINGS);
                        entries.add(ModItems.EMERALD_BOOTS);

                        // Emerald Horse Armor
                        entries.add(ModItems.EMERALD_HORSE_ARMOR);

                        entries.add(Items.EMERALD);
                    })
                    .build());

    // ============================================
    // NETHERITE TOOLS & ARMOR TAB (NEW!)
    // ============================================
    public static final ItemGroup NETHERITE_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(EmeraldMod.MOD_ID, "netherite_group"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(Items.NETHERITE_SWORD))
                    .displayName(Text.translatable("itemgroup.emeraldmod.netherite"))
                    .entries((displayContext, entries) -> {
                        // Netherite Upgrade Template (Vanilla)
                        entries.add(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);

                        // Netherite Tools (Vanilla)
                        entries.add(Items.NETHERITE_SWORD);
                        entries.add(Items.NETHERITE_PICKAXE);
                        entries.add(Items.NETHERITE_AXE);
                        entries.add(Items.NETHERITE_SHOVEL);
                        entries.add(Items.NETHERITE_HOE);

                        // Netherite Armor (Vanilla)
                        entries.add(Items.NETHERITE_HELMET);
                        entries.add(Items.NETHERITE_CHESTPLATE);
                        entries.add(Items.NETHERITE_LEGGINGS);
                        entries.add(Items.NETHERITE_BOOTS);

                        // Netherite Horse Armor (MOD ADDITION!)
                        entries.add(ModItems.NETHERITE_HORSE_ARMOR);

                        // Netherite Ingot & Scrap (untuk crafting)
                        entries.add(Items.NETHERITE_INGOT);
                        entries.add(Items.NETHERITE_SCRAP);
                    })
                    .build());

    public static void registerItemGroups() {
        EmeraldMod.LOGGER.info("Registering Item Groups for " + EmeraldMod.MOD_ID);

        // Initialize armor materials
        ModArmorMaterial.initialize();
        NetheriteArmorMaterial.initialize();

        EmeraldMod.LOGGER.info("✓ Registered Emerald Tools & Armor Group");
        EmeraldMod.LOGGER.info("✓ Registered Netherite Tools & Armor Group");
    }
}