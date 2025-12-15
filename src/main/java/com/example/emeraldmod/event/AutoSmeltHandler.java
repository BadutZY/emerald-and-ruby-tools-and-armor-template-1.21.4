package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.ModItems;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AutoSmeltHandler {

    private static final Random RANDOM = new Random();

    // Map ore block ke smelted item
    private static final Map<Block, ItemStack> ORE_TO_INGOT = new HashMap<>();

    static {
        // Copper Ore
        ORE_TO_INGOT.put(Blocks.COPPER_ORE, new ItemStack(Items.COPPER_INGOT));
        ORE_TO_INGOT.put(Blocks.DEEPSLATE_COPPER_ORE, new ItemStack(Items.COPPER_INGOT));

        // Iron Ore
        ORE_TO_INGOT.put(Blocks.IRON_ORE, new ItemStack(Items.IRON_INGOT));
        ORE_TO_INGOT.put(Blocks.DEEPSLATE_IRON_ORE, new ItemStack(Items.IRON_INGOT));

        // Gold Ore
        ORE_TO_INGOT.put(Blocks.GOLD_ORE, new ItemStack(Items.GOLD_INGOT));
        ORE_TO_INGOT.put(Blocks.DEEPSLATE_GOLD_ORE, new ItemStack(Items.GOLD_INGOT));
        ORE_TO_INGOT.put(Blocks.NETHER_GOLD_ORE, new ItemStack(Items.GOLD_INGOT));

        // Ancient Debris -> Netherite Scrap
        ORE_TO_INGOT.put(Blocks.ANCIENT_DEBRIS, new ItemStack(Items.NETHERITE_SCRAP));
    }

    public static void register() {
        // Gunakan BEFORE untuk cancel drop default
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            // Cek apakah player menggunakan Emerald Pickaxe
            ItemStack tool = player.getMainHandStack();
            if (tool.getItem() == ModItems.EMERALD_PICKAXE) {
                // Cek apakah ada Silk Touch enchantment
                int silkTouchLevel = EnchantmentHelper.getLevel(
                        world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                                .getOrThrow(Enchantments.SILK_TOUCH),
                        tool
                );

                // Jika ada Silk Touch, biarkan drop normal (ore block)
                if (silkTouchLevel > 0) {
                    return true; // Allow normal drop (ore block)
                }

                Block block = state.getBlock();
                // Cek apakah block adalah ore yang bisa di-smelt
                if (ORE_TO_INGOT.containsKey(block)) {
                    // Handle auto-smelt di sini
                    handleAutoSmelt(world, player, pos, state, tool);
                    // Return false untuk cancel drop default dari loot table
                    return false;
                }
            }
            // Return true untuk allow normal drop
            return true;
        });

        EmeraldMod.LOGGER.info("✓ Registered Auto-Smelt Handler for Emerald Pickaxe (with Silk Touch support)");
    }

    private static void handleAutoSmelt(World world, PlayerEntity player, BlockPos pos, BlockState state, ItemStack tool) {
        if (world.isClient) return;

        Block block = state.getBlock();

        // Cek apakah block adalah ore yang bisa di-smelt
        if (ORE_TO_INGOT.containsKey(block)) {
            // Dapatkan Fortune level
            int fortuneLevel = EnchantmentHelper.getLevel(
                    world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                            .getOrThrow(Enchantments.FORTUNE),
                    tool
            );

            // Hitung jumlah drop berdasarkan Fortune
            int dropCount = calculateDropCount(block, fortuneLevel);

            // Dapatkan smelted item
            ItemStack smeltedItem = ORE_TO_INGOT.get(block).copy();
            smeltedItem.setCount(dropCount);

            // Drop smelted item di posisi block
            if (world instanceof ServerWorld serverWorld) {
                // Hapus block tanpa drop default
                serverWorld.breakBlock(pos, false, player);

                // Spawn ingot saja
                ItemEntity itemEntity = new ItemEntity(
                        serverWorld,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        smeltedItem
                );

                // Set velocity kecil agar item tidak terlalu jauh
                itemEntity.setVelocity(
                        (RANDOM.nextDouble() - 0.5) * 0.1,
                        0.2,
                        (RANDOM.nextDouble() - 0.5) * 0.1
                );

                serverWorld.spawnEntity(itemEntity);

                // Add experience orbs (seperti smelting di furnace)
                int experience = calculateExperience(block, dropCount);
                if (experience > 0) {
                    player.addExperience(experience);
                }

                // Damage tool
                tool.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);

                EmeraldMod.LOGGER.debug("Auto-smelted {} x{} (Fortune {})",
                        smeltedItem.getItem().getName().getString(),
                        dropCount,
                        fortuneLevel);
            }
        }
    }

    private static int calculateDropCount(Block block, int fortuneLevel) {
        // Base drop count
        int baseCount = 1;

        // Copper Ore: 2-5 raw copper (rata-rata 3.5), kita buat 1-2 ingot base
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
            baseCount = RANDOM.nextInt(2) + 2; // 2-3 copper ingot base
        }

        // Nether Gold Ore: 2-6 gold nugget base, kita buat jadi 1 gold ingot
        if (block == Blocks.NETHER_GOLD_ORE) {
            baseCount = 1;
        }

        // Tambahkan bonus Fortune untuk SEMUA ore termasuk Ancient Debris
        if (fortuneLevel > 0) {
            // Fortune dapat menambah drop
            // Formula: chance untuk bonus item meningkat dengan Fortune level
            int maxBonus = fortuneLevel; // Fortune I = max +1, Fortune II = max +2, Fortune III = max +3

            for (int i = 0; i < fortuneLevel; i++) {
                // Setiap level Fortune punya 33% chance untuk menambah 1 item
                if (RANDOM.nextFloat() < 0.33f) {
                    baseCount++;
                }
            }

            // Bonus tambahan: ada small chance untuk jackpot drop
            if (fortuneLevel >= 3 && RANDOM.nextFloat() < 0.1f) { // 10% chance dengan Fortune III
                baseCount += RANDOM.nextInt(2) + 1; // Bonus 1-2 extra items
            }
        }

        return Math.max(1, baseCount);
    }

    private static int calculateExperience(Block block, int dropCount) {
        // Experience yang didapat saat smelting
        int baseExp = 0;

        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
            baseExp = 1; // 0.7 di furnace, kita bulatkan jadi 1
        } else if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            baseExp = 1; // 0.7 di furnace
        } else if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE ||
                block == Blocks.NETHER_GOLD_ORE) {
            baseExp = 1; // 1.0 di furnace
        } else if (block == Blocks.ANCIENT_DEBRIS) {
            baseExp = 2; // 2.0 di furnace
        }

        // Multiply by drop count
        return baseExp * dropCount;
    }
}