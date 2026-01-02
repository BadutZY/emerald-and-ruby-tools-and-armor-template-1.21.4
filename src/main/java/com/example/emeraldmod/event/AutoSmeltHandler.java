package com.example.emeraldmod.event;

import com.example.emeraldmod.block.ModBlocks;
import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.ModItems;
import com.example.emeraldmod.state.EffectStateManager;
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

import java.util.*;

public class AutoSmeltHandler {

    private static final Random RANDOM = new Random();

    // Maximum ore blocks untuk vein mining (Ruby Pickaxe only)
    private static final int MAX_VEIN_SIZE = 64;

    // Radius pencarian ore untuk vein mining (Ruby Pickaxe only)
    private static final int SEARCH_RADIUS = 3;

    // Map ore block ke smelted item
    private static final Map<Block, ItemStack> ORE_TO_INGOT = new HashMap<>();

    // Set semua ore blocks yang bisa auto-smelt
    private static final Set<Block> SMELTABLE_ORES = new HashSet<>();

    // Set ore blocks yang bisa vein mining tapi TIDAK auto-smelt
    private static final Set<Block> VEIN_MINING_ONLY_ORES = new HashSet<>();

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

        // Coal Ore
        ORE_TO_INGOT.put(Blocks.COAL_ORE, new ItemStack(Items.COAL));
        ORE_TO_INGOT.put(Blocks.DEEPSLATE_COAL_ORE, new ItemStack(Items.COAL));

        // Diamond Ore
        ORE_TO_INGOT.put(Blocks.DIAMOND_ORE, new ItemStack(Items.DIAMOND));
        ORE_TO_INGOT.put(Blocks.DEEPSLATE_DIAMOND_ORE, new ItemStack(Items.DIAMOND));

        // Emerald Ore
        ORE_TO_INGOT.put(Blocks.EMERALD_ORE, new ItemStack(Items.EMERALD));
        ORE_TO_INGOT.put(Blocks.DEEPSLATE_EMERALD_ORE, new ItemStack(Items.EMERALD));

        // Lapis Ore
        ORE_TO_INGOT.put(Blocks.LAPIS_ORE, new ItemStack(Items.LAPIS_LAZULI));
        ORE_TO_INGOT.put(Blocks.DEEPSLATE_LAPIS_ORE, new ItemStack(Items.LAPIS_LAZULI));

        // Redstone Ore
        ORE_TO_INGOT.put(Blocks.REDSTONE_ORE, new ItemStack(Items.REDSTONE));
        ORE_TO_INGOT.put(Blocks.DEEPSLATE_REDSTONE_ORE, new ItemStack(Items.REDSTONE));

        // Nether Quartz Ore
        ORE_TO_INGOT.put(Blocks.NETHER_QUARTZ_ORE, new ItemStack(Items.QUARTZ));

        // Ruby Ores (Overworld - auto-smelt)
        ORE_TO_INGOT.put(ModBlocks.RUBY_ORE, new ItemStack(ModItems.RUBY_INGOT));
        ORE_TO_INGOT.put(ModBlocks.DEEPSLATE_RUBY_ORE, new ItemStack(ModItems.RUBY_INGOT));

        ORE_TO_INGOT.put(ModBlocks.RUBY_DEBRIS, new ItemStack(ModItems.RUBY_SCRAP));

        // Populate SMELTABLE_ORES set
        SMELTABLE_ORES.addAll(ORE_TO_INGOT.keySet());

        // Nether Ruby Ore - vein mining only, NO auto-smelt
        VEIN_MINING_ONLY_ORES.add(ModBlocks.NETHER_RUBY_ORE);
    }

    public static void register() {
        // Gunakan BEFORE untuk cancel drop default
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            ItemStack tool = player.getMainHandStack();
            boolean isEmeraldPickaxe = tool.getItem() == ModItems.EMERALD_PICKAXE;
            boolean isRubyPickaxe = tool.getItem() == ModItems.RUBY_PICKAXE;

            // Cek apakah menggunakan Emerald atau Ruby Pickaxe
            if (isEmeraldPickaxe || isRubyPickaxe) {
                // Check tools effect enabled/disabled
                if (!world.isClient) {
                    ServerWorld serverWorld = (ServerWorld) world;
                    EffectStateManager stateManager = EffectStateManager.getServerState(serverWorld.getServer());

                    if (!stateManager.isToolsEnabled(player.getUuid())) {
                        // Tools effect DISABLED, biarkan break normal
                        EmeraldMod.LOGGER.debug("Auto-smelt disabled for player {}", player.getName().getString());
                        return true;
                    }
                }

                // Cek Silk Touch enchantment
                int silkTouchLevel = EnchantmentHelper.getLevel(
                        world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                                .getOrThrow(Enchantments.SILK_TOUCH),
                        tool
                );

                Block block = state.getBlock();

                // Handle Nether Ruby Ore (Vein Mining Only - NO Auto-Smelt)
                if (VEIN_MINING_ONLY_ORES.contains(block)) {
                    if (isRubyPickaxe) {
                        // Ruby Pickaxe: Vein Mining tanpa auto-smelt
                        handleVeinMiningNoSmelt(world, player, pos, state, tool);
                        return false; // Cancel default drop
                    } else {
                        // Emerald Pickaxe: Break normal (biarkan loot table handle drop)
                        return true;
                    }
                }

                // Handle regular ores dengan auto-smelt
                if (SMELTABLE_ORES.contains(block)) {
                    // Jika ada Silk Touch, biarkan drop normal
                    if (silkTouchLevel > 0) {
                        return true;
                    }

                    if (isRubyPickaxe) {
                        // Ruby Pickaxe: Auto-smelt + Vein Mining
                        handleVeinMining(world, player, pos, state, tool);
                    } else {
                        // Emerald Pickaxe: Auto-smelt only (single block)
                        handleSingleBlockSmelt(world, player, pos, state, tool);
                    }
                    return false; // Cancel default drop
                }
            }
            return true; // Allow normal drop
        });

        EmeraldMod.LOGGER.info("✓ Registered Auto-Smelt Handler (Toggleable)");
        EmeraldMod.LOGGER.info("  - Emerald Pickaxe: Auto-smelt only (single block)");
        EmeraldMod.LOGGER.info("  - Ruby Pickaxe: Auto-smelt + Vein Mining");
        EmeraldMod.LOGGER.info("  - Ruby Pickaxe: Vein Mining Nether Ruby Ore (NO auto-smelt)");
        EmeraldMod.LOGGER.info("  - Supports all vanilla ores + Ruby Ore + Nether Ruby Ore");
    }

    /**
     * Handle single block auto-smelt (Emerald Pickaxe)
     */
    private static void handleSingleBlockSmelt(World world, PlayerEntity player, BlockPos pos, BlockState state, ItemStack tool) {
        if (world.isClient) return;

        ServerWorld serverWorld = (ServerWorld) world;
        Block block = state.getBlock();

        // Get Fortune level
        int fortuneLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE),
                tool
        );

        // Calculate drops
        int dropCount = calculateDropCount(block, fortuneLevel);
        ItemStack smeltedItem = ORE_TO_INGOT.get(block).copy();
        smeltedItem.setCount(dropCount);

        // Break block without drop
        serverWorld.breakBlock(pos, false, player);

        // Spawn item entity
        ItemEntity itemEntity = new ItemEntity(
                serverWorld,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                smeltedItem
        );

        itemEntity.setVelocity(
                (RANDOM.nextDouble() - 0.5) * 0.1,
                0.2,
                (RANDOM.nextDouble() - 0.5) * 0.1
        );

        serverWorld.spawnEntity(itemEntity);

        // Add experience
        int experience = calculateExperience(block, dropCount);
        if (experience > 0) {
            player.addExperience(experience);
        }

        // Damage tool (1 durability per block)
        tool.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);

        EmeraldMod.LOGGER.debug("Single block auto-smelt: {} -> {} x{} (Fortune {})",
                block.getName().getString(),
                smeltedItem.getItem().getName().getString(),
                dropCount,
                fortuneLevel);
    }

    /**
     * Handle vein mining dengan auto-smelt (Ruby Pickaxe untuk ore biasa)
     */
    private static void handleVeinMining(World world, PlayerEntity player, BlockPos startPos, BlockState startState, ItemStack tool) {
        if (world.isClient) return;

        ServerWorld serverWorld = (ServerWorld) world;
        Block targetOre = startState.getBlock();

        // Set untuk track blocks yang sudah di-process
        Set<BlockPos> processedBlocks = new HashSet<>();

        // Queue untuk BFS (Breadth-First Search)
        Queue<BlockPos> toProcess = new LinkedList<>();
        toProcess.add(startPos);
        processedBlocks.add(startPos);

        // Counter untuk total ores
        int totalOresMined = 0;

        // Map untuk track total drops
        Map<ItemStack, Integer> totalDrops = new HashMap<>();

        // Get Fortune level
        int fortuneLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE),
                tool
        );

        EmeraldMod.LOGGER.debug("Starting vein mining for {} at {}", targetOre.getName().getString(), startPos);

        // BFS untuk menemukan semua ore yang connected
        while (!toProcess.isEmpty() && totalOresMined < MAX_VEIN_SIZE) {
            BlockPos currentPos = toProcess.poll();
            BlockState currentState = serverWorld.getBlockState(currentPos);
            Block currentBlock = currentState.getBlock();

            // Cek apakah block ini adalah ore yang sama dengan target
            if (currentBlock == targetOre && SMELTABLE_ORES.contains(currentBlock)) {
                // Process ore ini
                totalOresMined++;

                // Hitung drops
                int dropCount = calculateDropCount(currentBlock, fortuneLevel);
                ItemStack smeltedItem = ORE_TO_INGOT.get(currentBlock).copy();

                // Tambahkan ke total drops
                addToTotalDrops(totalDrops, smeltedItem, dropCount);

                // Break block tanpa drop
                serverWorld.breakBlock(currentPos, false, player);

                // Add experience
                int experience = calculateExperience(currentBlock, dropCount);
                if (experience > 0) {
                    player.addExperience(experience);
                }

                // Cari ore di sekitar block ini
                for (BlockPos neighborPos : getNeighborPositions(currentPos)) {
                    if (!processedBlocks.contains(neighborPos) &&
                            isWithinSearchRadius(startPos, neighborPos)) {

                        BlockState neighborState = serverWorld.getBlockState(neighborPos);
                        Block neighborBlock = neighborState.getBlock();

                        // Hanya tambahkan jika block tetangga adalah ore yang SAMA
                        if (neighborBlock == targetOre) {
                            toProcess.add(neighborPos);
                            processedBlocks.add(neighborPos);
                        }
                    }
                }
            }
        }

        // Drop semua items yang terkumpul di posisi awal
        if (!totalDrops.isEmpty()) {
            for (Map.Entry<ItemStack, Integer> entry : totalDrops.entrySet()) {
                ItemStack itemToDrop = entry.getKey().copy();
                int totalCount = entry.getValue();

                // Split into multiple stacks if needed
                int maxStackSize = itemToDrop.getMaxCount();
                while (totalCount > 0) {
                    int stackSize = Math.min(totalCount, maxStackSize);
                    ItemStack dropStack = itemToDrop.copy();
                    dropStack.setCount(stackSize);

                    // Spawn item entity
                    ItemEntity itemEntity = new ItemEntity(
                            serverWorld,
                            startPos.getX() + 0.5,
                            startPos.getY() + 0.5,
                            startPos.getZ() + 0.5,
                            dropStack
                    );

                    itemEntity.setVelocity(
                            (RANDOM.nextDouble() - 0.5) * 0.1,
                            0.2,
                            (RANDOM.nextDouble() - 0.5) * 0.1
                    );

                    serverWorld.spawnEntity(itemEntity);
                    totalCount -= stackSize;
                }
            }
        }

        // Damage tool berdasarkan jumlah ore yang di-mine
        int durabilityDamage = Math.min(totalOresMined, tool.getMaxDamage() - tool.getDamage());
        if (durabilityDamage > 0) {
            tool.damage(durabilityDamage, player, net.minecraft.entity.EquipmentSlot.MAINHAND);
        }

        EmeraldMod.LOGGER.info("Vein mining completed: {} {} ores mined (Fortune {})",
                totalOresMined,
                targetOre.getName().getString(),
                fortuneLevel);
    }

    /**
     * Handle vein mining TANPA auto-smelt (Ruby Pickaxe untuk Nether Ruby Ore)
     * Drop akan menggunakan loot table default (Ruby Scrap 2-4)
     */
    private static void handleVeinMiningNoSmelt(World world, PlayerEntity player, BlockPos startPos, BlockState startState, ItemStack tool) {
        if (world.isClient) return;

        ServerWorld serverWorld = (ServerWorld) world;
        Block targetOre = startState.getBlock();

        // Set untuk track blocks yang sudah di-process
        Set<BlockPos> processedBlocks = new HashSet<>();

        // Queue untuk BFS (Breadth-First Search)
        Queue<BlockPos> toProcess = new LinkedList<>();
        toProcess.add(startPos);
        processedBlocks.add(startPos);

        // Counter untuk total ores
        int totalOresMined = 0;

        // Get Fortune level
        int fortuneLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE),
                tool
        );

        // Cek Silk Touch
        int silkTouchLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.SILK_TOUCH),
                tool
        );

        EmeraldMod.LOGGER.debug("Starting vein mining (NO SMELT) for {} at {}", targetOre.getName().getString(), startPos);

        // Total drops untuk Nether Ruby Ore
        int totalRubyScrap = 0;
        int totalExperience = 0;

        // BFS untuk menemukan semua ore yang connected
        while (!toProcess.isEmpty() && totalOresMined < MAX_VEIN_SIZE) {
            BlockPos currentPos = toProcess.poll();
            BlockState currentState = serverWorld.getBlockState(currentPos);
            Block currentBlock = currentState.getBlock();

            // Cek apakah block ini adalah ore yang sama dengan target
            if (currentBlock == targetOre && VEIN_MINING_ONLY_ORES.contains(currentBlock)) {
                // Process ore ini
                totalOresMined++;

                // Calculate drops untuk Nether Ruby Ore
                if (currentBlock == ModBlocks.NETHER_RUBY_ORE) {
                    if (silkTouchLevel > 0) {
                        // Dengan Silk Touch, drop block-nya
                        ItemStack blockDrop = new ItemStack(ModBlocks.NETHER_RUBY_ORE);
                        ItemEntity itemEntity = new ItemEntity(
                                serverWorld,
                                currentPos.getX() + 0.5,
                                currentPos.getY() + 0.5,
                                currentPos.getZ() + 0.5,
                                blockDrop
                        );
                        itemEntity.setVelocity(
                                (RANDOM.nextDouble() - 0.5) * 0.1,
                                0.2,
                                (RANDOM.nextDouble() - 0.5) * 0.1
                        );
                        serverWorld.spawnEntity(itemEntity);
                    } else {
                        // Drop Ruby Scrap (2-4 base, affected by Fortune)
                        int scrapCount = calculateNetherRubyScrapDrop(fortuneLevel);
                        totalRubyScrap += scrapCount;

                        // Add XP (0-1 per ore)
                        if (RANDOM.nextBoolean()) {
                            totalExperience += 1;
                        }
                    }
                }

                // Break block tanpa drop
                serverWorld.breakBlock(currentPos, false, player);

                // Cari ore di sekitar block ini
                for (BlockPos neighborPos : getNeighborPositions(currentPos)) {
                    if (!processedBlocks.contains(neighborPos) &&
                            isWithinSearchRadius(startPos, neighborPos)) {

                        BlockState neighborState = serverWorld.getBlockState(neighborPos);
                        Block neighborBlock = neighborState.getBlock();

                        // Hanya tambahkan jika block tetangga adalah ore yang SAMA
                        if (neighborBlock == targetOre) {
                            toProcess.add(neighborPos);
                            processedBlocks.add(neighborPos);
                        }
                    }
                }
            }
        }

        // Drop all Ruby Scrap (jika tidak Silk Touch)
        if (totalRubyScrap > 0) {
            ItemStack rubyScrapStack = new ItemStack(ModItems.RUBY_NUGGET);
            int maxStackSize = rubyScrapStack.getMaxCount();

            while (totalRubyScrap > 0) {
                int stackSize = Math.min(totalRubyScrap, maxStackSize);
                ItemStack dropStack = new ItemStack(ModItems.RUBY_NUGGET, stackSize);

                // Spawn item entity di posisi awal
                ItemEntity itemEntity = new ItemEntity(
                        serverWorld,
                        startPos.getX() + 0.5,
                        startPos.getY() + 0.5,
                        startPos.getZ() + 0.5,
                        dropStack
                );

                itemEntity.setVelocity(
                        (RANDOM.nextDouble() - 0.5) * 0.1,
                        0.2,
                        (RANDOM.nextDouble() - 0.5) * 0.1
                );

                serverWorld.spawnEntity(itemEntity);
                totalRubyScrap -= stackSize;
            }
        }

        // Add experience
        if (totalExperience > 0) {
            player.addExperience(totalExperience);
        }

        // Damage tool berdasarkan jumlah ore yang di-mine
        int durabilityDamage = Math.min(totalOresMined, tool.getMaxDamage() - tool.getDamage());
        if (durabilityDamage > 0) {
            tool.damage(durabilityDamage, player, net.minecraft.entity.EquipmentSlot.MAINHAND);
        }

        EmeraldMod.LOGGER.info("Vein mining (NO SMELT) completed: {} {} ores mined, {} Ruby Scrap dropped (Fortune {})",
                totalOresMined,
                targetOre.getName().getString(),
                totalRubyScrap,
                fortuneLevel);
    }

    /**
     * Calculate drop count untuk Nether Ruby Ore (Ruby Scrap)
     * Base: 2-4, affected by Fortune
     */
    private static int calculateNetherRubyScrapDrop(int fortuneLevel) {
        // Base drop: 2-4 Ruby Scrap
        int baseCount = RANDOM.nextInt(3) + 2; // 2, 3, atau 4

        // Fortune bonus
        if (fortuneLevel > 0) {
            // Fortune I: +0-1
            // Fortune II: +0-2
            // Fortune III: +0-3
            int fortuneBonus = RANDOM.nextInt(fortuneLevel + 1);
            baseCount += fortuneBonus;
        }

        return Math.max(2, baseCount); // Minimum 2 scrap
    }

    /**
     * Helper method untuk menambahkan drops ke total
     */
    private static void addToTotalDrops(Map<ItemStack, Integer> totalDrops, ItemStack item, int count) {
        boolean found = false;
        for (Map.Entry<ItemStack, Integer> entry : totalDrops.entrySet()) {
            if (ItemStack.areItemsEqual(entry.getKey(), item)) {
                entry.setValue(entry.getValue() + count);
                found = true;
                break;
            }
        }
        if (!found) {
            totalDrops.put(item.copy(), count);
        }
    }

    /**
     * Get semua posisi tetangga (26 blocks di sekitar - 3x3x3 minus center)
     */
    private static List<BlockPos> getNeighborPositions(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    neighbors.add(pos.add(x, y, z));
                }
            }
        }
        return neighbors;
    }

    /**
     * Cek apakah posisi masih dalam radius pencarian
     */
    private static boolean isWithinSearchRadius(BlockPos start, BlockPos current) {
        return Math.abs(current.getX() - start.getX()) <= SEARCH_RADIUS &&
                Math.abs(current.getY() - start.getY()) <= SEARCH_RADIUS &&
                Math.abs(current.getZ() - start.getZ()) <= SEARCH_RADIUS;
    }

    private static int calculateDropCount(Block block, int fortuneLevel) {
        int baseCount = 1;

        // Copper Ore: 2-3 copper ingot base
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
            baseCount = RANDOM.nextInt(2) + 2;
        }

        // Nether Gold Ore: 1 gold ingot
        if (block == Blocks.NETHER_GOLD_ORE) {
            baseCount = 1;
        }

        // Lapis: 4-9 lapis base
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
            baseCount = RANDOM.nextInt(6) + 4;
        }

        // Redstone: 4-5 redstone base
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            baseCount = RANDOM.nextInt(2) + 4;
        }

        // Ruby Ore: 1 ruby ingot base
        if (block == ModBlocks.RUBY_ORE || block == ModBlocks.DEEPSLATE_RUBY_ORE) {
            baseCount = 1;
        }

        // Fortune bonus
        if (fortuneLevel > 0) {
            for (int i = 0; i < fortuneLevel; i++) {
                if (RANDOM.nextFloat() < 0.33f) {
                    baseCount++;
                }
            }

            // Jackpot bonus untuk Fortune III
            if (fortuneLevel >= 3 && RANDOM.nextFloat() < 0.1f) {
                baseCount += RANDOM.nextInt(2) + 1;
            }
        }

        return Math.max(1, baseCount);
    }

    private static int calculateExperience(Block block, int dropCount) {
        int baseExp = 0;

        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
            baseExp = 1;
        } else if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            baseExp = 1;
        } else if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE ||
                block == Blocks.NETHER_GOLD_ORE) {
            baseExp = 1;
        } else if (block == Blocks.ANCIENT_DEBRIS) {
            baseExp = 2;
        } else if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) {
            baseExp = 1;
        } else if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            baseExp = 3;
        } else if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
            baseExp = 3;
        } else if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
            baseExp = 2;
        } else if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            baseExp = 1;
        } else if (block == Blocks.NETHER_QUARTZ_ORE) {
            baseExp = 2;
        } else if (block == ModBlocks.RUBY_ORE || block == ModBlocks.DEEPSLATE_RUBY_ORE) {
            baseExp = 3;
        } else if (block == ModBlocks.RUBY_DEBRIS) {
            baseExp = 2;
        }

        return baseExp * dropCount;
    }
}