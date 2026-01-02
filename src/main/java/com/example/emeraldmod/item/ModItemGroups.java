package com.example.emeraldmod.item;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.block.ModBlocks;
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
    // RUBY EQUIPMENT TAB (Tools, Armor, Template)
    // ============================================
    public static final ItemGroup RUBY_EQUIPMENT_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(EmeraldMod.MOD_ID, "ruby_equipment"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModItems.RUBY_SWORD))
                    .displayName(Text.translatable("itemgroup.emeraldmod.ruby_equipment"))
                    .entries((displayContext, entries) -> {
                        // Ruby Upgrade Template
                        entries.add(ModItems.RUBY_UPGRADE_SMITHING_TEMPLATE);

                        // Ruby Tools
                        entries.add(ModItems.RUBY_SWORD);
                        entries.add(ModItems.RUBY_PICKAXE);
                        entries.add(ModItems.RUBY_AXE);
                        entries.add(ModItems.RUBY_SHOVEL);
                        entries.add(ModItems.RUBY_HOE);

                        // Ruby Armor
                        entries.add(ModItems.RUBY_HELMET);
                        entries.add(ModItems.RUBY_CHESTPLATE);
                        entries.add(ModItems.RUBY_LEGGINGS);
                        entries.add(ModItems.RUBY_BOOTS);

                        // Ruby Horse Armor
                        entries.add(ModItems.RUBY_HORSE_ARMOR);

                        // Ruby Materials
                        entries.add(ModItems.RUBY);
                        entries.add(ModItems.RAW_RUBY);
                        entries.add(ModItems.RUBY_INGOT);
                        entries.add(ModItems.RUBY_NUGGET);
                        entries.add(ModItems.RUBY_SCRAP);
                    })
                    .build());

    // ============================================
    // RUBY MATERIALS TAB (Ores, Blocks, Materials)
    // ============================================
    public static final ItemGroup RUBY_MATERIALS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(EmeraldMod.MOD_ID, "ruby_materials"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModBlocks.RUBY_BLOCK))
                    .displayName(Text.translatable("itemgroup.emeraldmod.ruby_materials"))
                    .entries((displayContext, entries) -> {
                        // Ruby Ores
                        entries.add(ModBlocks.RUBY_ORE);
                        entries.add(ModBlocks.DEEPSLATE_RUBY_ORE);
                        entries.add(ModBlocks.NETHER_RUBY_ORE);

                        // Ruby Blocks
                        entries.add(ModBlocks.RUBY_BLOCK);
                        entries.add(ModBlocks.RAW_RUBY_BLOCK);
                        entries.add(ModBlocks.RUBY_DEBRIS);
                        entries.add(ModBlocks.RUBY_INGOT_BLOCK);

                        // Ruby Materials
                        entries.add(ModItems.RUBY);
                        entries.add(ModItems.RAW_RUBY);
                        entries.add(ModItems.RUBY_INGOT);
                        entries.add(ModItems.RUBY_NUGGET);
                        entries.add(ModItems.RUBY_SCRAP);
                    })
                    .build());

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

                        // Emerald (vanilla)
                        entries.add(Items.EMERALD);
                    })
                    .build());

    // ============================================
    // NETHERITE TOOLS & ARMOR TAB
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
        RubyArmorMaterial.initialize();
        ModArmorMaterial.initialize();
        NetheriteArmorMaterial.initialize();

        EmeraldMod.LOGGER.info("✓ Registered Ruby Equipment Group (Tools, Armor, Template)");
        EmeraldMod.LOGGER.info("✓ Registered Ruby Materials Group (Ores, Blocks, Materials)");
        EmeraldMod.LOGGER.info("✓ Registered Emerald Tools & Armor Group");
        EmeraldMod.LOGGER.info("✓ Registered Netherite Tools & Armor Group");
    }
}