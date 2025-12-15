package com.example.emeraldmod.item;

import com.example.emeraldmod.EmeraldMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItems {

    private static RegistryKey<Item> createRegistryKey(String name) {
        return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(EmeraldMod.MOD_ID, name));
    }

    private static <T extends Item> T registerItem(String name, T item) {
        return Registry.register(Registries.ITEM, Identifier.of(EmeraldMod.MOD_ID, name), item);
    }

    // ============================================
    // EMERALD TOOLS
    // ============================================
    public static final SwordItem EMERALD_SWORD = registerItem("emerald_sword",
            new SwordItem(ModToolMaterial.EMERALD, 5, -1.5F,
                    new Item.Settings().registryKey(createRegistryKey("emerald_sword")).fireproof()));

    public static final PickaxeItem EMERALD_PICKAXE = registerItem("emerald_pickaxe",
            new PickaxeItem(ModToolMaterial.EMERALD, 3, -1.5F,
                    new Item.Settings().registryKey(createRegistryKey("emerald_pickaxe")).fireproof()));

    public static final AxeItem EMERALD_AXE = registerItem("emerald_axe",
            new AxeItem(ModToolMaterial.EMERALD, 7, -2.5F,
                    new Item.Settings().registryKey(createRegistryKey("emerald_axe")).fireproof()));

    public static final ShovelItem EMERALD_SHOVEL = registerItem("emerald_shovel",
            new ShovelItem(ModToolMaterial.EMERALD, 3.5F, -3.0F,
                    new Item.Settings().registryKey(createRegistryKey("emerald_shovel")).fireproof()));

    public static final HoeItem EMERALD_HOE = registerItem("emerald_hoe",
            new HoeItem(ModToolMaterial.EMERALD, -1, 2.0F,
                    new Item.Settings().registryKey(createRegistryKey("emerald_hoe")).fireproof()));

    // ============================================
    // EMERALD ARMOR
    // ============================================
    public static final EmeraldArmorItem EMERALD_HELMET = registerItem("emerald_helmet",
            new EmeraldArmorItem(ModArmorMaterial.EMERALD_ARMOR_MATERIAL, EquipmentType.HELMET,
                    new Item.Settings().registryKey(createRegistryKey("emerald_helmet")).fireproof()));

    public static final EmeraldArmorItem EMERALD_CHESTPLATE = registerItem("emerald_chestplate",
            new EmeraldArmorItem(ModArmorMaterial.EMERALD_ARMOR_MATERIAL, EquipmentType.CHESTPLATE,
                    new Item.Settings().registryKey(createRegistryKey("emerald_chestplate")).fireproof()));

    public static final EmeraldArmorItem EMERALD_LEGGINGS = registerItem("emerald_leggings",
            new EmeraldArmorItem(ModArmorMaterial.EMERALD_ARMOR_MATERIAL, EquipmentType.LEGGINGS,
                    new Item.Settings().registryKey(createRegistryKey("emerald_leggings")).fireproof()));

    public static final EmeraldArmorItem EMERALD_BOOTS = registerItem("emerald_boots",
            new EmeraldArmorItem(ModArmorMaterial.EMERALD_ARMOR_MATERIAL, EquipmentType.BOOTS,
                    new Item.Settings().registryKey(createRegistryKey("emerald_boots")).fireproof()));

    // ============================================
    // EMERALD HORSE ARMOR
    // ============================================
    public static final AnimalArmorItem EMERALD_HORSE_ARMOR = registerItem("emerald_horse_armor",
            new AnimalArmorItem(
                    ModArmorMaterial.EMERALD_ARMOR_MATERIAL,
                    AnimalArmorItem.Type.EQUESTRIAN,
                    new Item.Settings().registryKey(createRegistryKey("emerald_horse_armor")).fireproof().maxCount(1)
            ));

    // ============================================
    // NETHERITE HORSE ARMOR (NEW!)
    // ============================================
    public static final AnimalArmorItem NETHERITE_HORSE_ARMOR = registerItem("netherite_horse_armor",
            new AnimalArmorItem(
                    NetheriteArmorMaterial.NETHERITE_HORSE_ARMOR_MATERIAL,
                    AnimalArmorItem.Type.EQUESTRIAN,
                    new Item.Settings().registryKey(createRegistryKey("netherite_horse_armor")).fireproof().maxCount(1)
            ));

    // ============================================
    // SMITHING TEMPLATE
    // ============================================
    public static final Item EMERALD_UPGRADE_SMITHING_TEMPLATE = registerItem(
            "emerald_upgrade_smithing_template",
            new Item(new Item.Settings().registryKey(createRegistryKey("emerald_upgrade_smithing_template")).maxCount(64)));

    public static void registerModItems() {
        EmeraldMod.LOGGER.info("Registering Mod Items for " + EmeraldMod.MOD_ID);

        // Add to Combat tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(EMERALD_SWORD);
            entries.add(EMERALD_AXE);
            entries.add(EMERALD_HELMET);
            entries.add(EMERALD_CHESTPLATE);
            entries.add(EMERALD_LEGGINGS);
            entries.add(EMERALD_BOOTS);
        });

        // Add to Tools tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(EMERALD_PICKAXE);
            entries.add(EMERALD_SHOVEL);
            entries.add(EMERALD_HOE);
        });

        // Add to Ingredients tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(EMERALD_UPGRADE_SMITHING_TEMPLATE);
            entries.add(EMERALD_HORSE_ARMOR);
            entries.add(NETHERITE_HORSE_ARMOR); // NEW!
        });

        EmeraldMod.LOGGER.info("Successfully registered items including Netherite Horse Armor");
    }
}