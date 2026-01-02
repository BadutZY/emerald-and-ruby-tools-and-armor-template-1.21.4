package com.example.emeraldmod.block;

import com.example.emeraldmod.EmeraldMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.UniformIntProvider;

public class ModBlocks {

    private static RegistryKey<Block> createRegistryKey(String name) {
        return RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EmeraldMod.MOD_ID, name));
    }

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(EmeraldMod.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(EmeraldMod.MOD_ID, name));
        Registry.register(Registries.ITEM, Identifier.of(EmeraldMod.MOD_ID, name),
                new BlockItem(block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey()));
    }

    // ============================================
    // RUBY BLOCKS
    // ============================================

    // Ruby Ore - drops raw ruby, gives XP
    public static final Block RUBY_ORE = registerBlock("ruby_ore",
            new ExperienceDroppingBlock(
                    UniformIntProvider.create(3, 7),
                    AbstractBlock.Settings.create()
                            .registryKey(createRegistryKey("ruby_ore"))
                            .strength(3.0f, 3.0f)
                            .requiresTool()
                            .sounds(BlockSoundGroup.STONE)
            ));

    // Deepslate Ruby Ore - drops raw ruby, gives XP
    public static final Block DEEPSLATE_RUBY_ORE = registerBlock("deepslate_ruby_ore",
            new ExperienceDroppingBlock(
                    UniformIntProvider.create(3, 7),
                    AbstractBlock.Settings.create()
                            .registryKey(createRegistryKey("deepslate_ruby_ore"))
                            .strength(4.5f, 3.0f)
                            .requiresTool()
                            .sounds(BlockSoundGroup.DEEPSLATE)
            ));

    // Nether Ruby Ore - drops Ruby Scrap (2-4), gives XP
    public static final Block NETHER_RUBY_ORE = registerBlock("nether_ruby_ore",
            new ExperienceDroppingBlock(
                    UniformIntProvider.create(0, 1),
                    AbstractBlock.Settings.create()
                            .registryKey(createRegistryKey("nether_ruby_ore"))
                            .strength(3.0f, 3.0f)
                            .requiresTool()
                            .sounds(BlockSoundGroup.NETHER_GOLD_ORE)
            ));

    // Ruby Block - drops itself
    public static final Block RUBY_BLOCK = registerBlock("ruby_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(createRegistryKey("ruby_block"))
                    .strength(5.0f, 6.0f)
                    .requiresTool()
                    .sounds(BlockSoundGroup.METAL)
            ));

    // Raw Ruby Block - drops itself
    public static final Block RAW_RUBY_BLOCK = registerBlock("raw_ruby_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(createRegistryKey("raw_ruby_block"))
                    .strength(5.0f, 6.0f)
                    .requiresTool()
                    .sounds(BlockSoundGroup.STONE)
            ));

    // Ruby Scrap Block - NEW! (seperti Ancient Debris)
    public static final Block RUBY_DEBRIS = registerBlock("ruby_debris",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(createRegistryKey("ruby_debris"))
                    .strength(5.0f, 6.0f)
                    .requiresTool()
                    .sounds(BlockSoundGroup.ANCIENT_DEBRIS)
            ));

    // Ruby Ingot Block - NEW! (seperti metal block lainnya)
    public static final Block RUBY_INGOT_BLOCK = registerBlock("ruby_ingot_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(createRegistryKey("ruby_ingot_block"))
                    .strength(5.0f, 6.0f)
                    .requiresTool()
                    .sounds(BlockSoundGroup.METAL)
            ));

    public static void registerModBlocks() {
        EmeraldMod.LOGGER.info("Registering Mod Blocks for " + EmeraldMod.MOD_ID);

        // Add Ruby blocks to Building Blocks tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.add(RUBY_BLOCK);
            entries.add(RAW_RUBY_BLOCK);
            entries.add(RUBY_DEBRIS);
            entries.add(RUBY_INGOT_BLOCK);
        });

        // Add Ruby ores to Natural Blocks tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(entries -> {
            entries.add(RUBY_ORE);
            entries.add(DEEPSLATE_RUBY_ORE);
            entries.add(NETHER_RUBY_ORE);
        });

        EmeraldMod.LOGGER.info("✓ Successfully registered all Ruby blocks");
        EmeraldMod.LOGGER.info("  - Ruby Ore (drops raw ruby)");
        EmeraldMod.LOGGER.info("  - Deepslate Ruby Ore (drops raw ruby)");
        EmeraldMod.LOGGER.info("  - Nether Ruby Ore (drops 2-4 ruby scrap)");
        EmeraldMod.LOGGER.info("  - Ruby Block (drops itself)");
        EmeraldMod.LOGGER.info("  - Raw Ruby Block (drops itself)");
        EmeraldMod.LOGGER.info("  - Ruby Scrap Block (drops itself - Ancient Debris style)");
        EmeraldMod.LOGGER.info("  - Ruby Ingot Block (drops itself - Metal block)");
    }
}